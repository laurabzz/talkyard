// Copyright (c) 2012 Kaj Magnus Lindberg (born 1979)

package com.debiki.v0

import java.{util => ju}
import collection.{immutable => imm, mutable => mut}
import Prelude._
import Debate._
import FlagReason.FlagReason


/** Actions builds up a page: a page consists of various posts,
  * e.g. the title post, the body post, and comments posted, and actions
  * that edit and affect these posts. (This is the action, a.k.a. command,
  * design pattern.)
  *
  * PostActionDto is a rather stupid data transfer object (DTO): it's used
  * by DbDao, when saving and loading pages. — If you want some more
  * functionality, use the PostAction:s Post and Patch instead. (That's what you
  * almost have to do anyway because that's what Debate(/PageParts/whatever) gives you.)
  *
  * @param id A local id, unique per page. "?" means unknown (used when
  * creating new posts).
  * @param loginId the login session, which in turn identifies
  * the user and IP and session creation time.
  * @param newIp Always None, unless the post was sent from somewhere else than
  * the relevant Login.ip.
  * @param payload What this action does. For example, creates a new post,
  * edits a post, flags it, closes a thread, etcetera.
  */
case class PostActionDto(
  id: String,
  creationDati: ju.Date,
  payload: PostActionPayload,
  postId: String,
  loginId: String,
  newIp: Option[String]) extends PostActionDtoOld {

  require(id != "0")
  require(id nonEmpty)

  def ctime = creationDati
}



sealed abstract class PostActionPayload



object PostActionPayload {


  sealed trait HidingPostPayload


  /** Closes a thread: collapses it and tucks it away under a Closed Threads
    * column, far away to the right.
    *
    * Use on old obsolete threads, e.g. a comment about a spelling mistake
    * that has since been fixed.
    */
  case object CloseTree extends PostActionPayload with HidingPostPayload

  case object CollapsePost extends PostActionPayload with HidingPostPayload

  case object CollapseReplies extends PostActionPayload with HidingPostPayload

  case object CollapseTree extends PostActionPayload with HidingPostPayload

  /** Undoes another action, e.g. an Undo with targetActionId = a CloseTree action
    * would reopen the closed tree.
    *
    * Requires another coulmn in DW1_PAGE_ACTIONS, namely TARGET_ACTION_ID.
    */
  case class Undo(targetActionId: String) extends PostActionPayload

}



/** Should use PostActionDto + PostActionPayload instead; then it's much
  * easier to add new types of actions.
  */
sealed abstract class PostActionDtoOld {
  /** A local id, unique only in the Debate that this action modifies.
    * "?" means unknown.
    */
  def id: String
  require(id != "0")
  require(id nonEmpty)

  /**
   * Identifies the login session, which in turn identifies
   * the user and IP and session creation time.
   */
  def loginId: String

  /** Always None, unless the post was sent from somewhere else than
   *  the relevant Login.ip.
   */
  def newIp: Option[String]
  def ctime: ju.Date

  def textLengthUtf8: Int = 0
}



abstract class MaybeApproval extends PostActionDtoOld {

  /**
   * If defined, this action implicitly approves the related post.
   *
   * For example, if an admin edits a post, then `edit.approval`
   * might be set to Approval.AuthoritativeUser, and `edit.isApplied`
   * would be set to true, and then the new version of the edited post
   * has "automatically" been approved.
   */
  def approval: Option[Approval]

}



/** Classifies an action, e.g. tags a Post as being "interesting" and "funny".
 *
 *  If you rate an action many times, only the last rating counts.
 *  - For an authenticated user, his/her most recent rating counts.
 *  - For other users, the most recent rating for the login id / session id
 *    counts.
 *  - Could let different non-authenticated sessions with the same
 *    user name, ip and email overwrite each other's ratings.
 *    But I might as well ask them to login instead? Saves my time, and CPU.
 */
case class Rating (
  id: String,
  postId: String,
  loginId: String,
  newIp: Option[String],
  ctime: ju.Date,
  tags: List[String]
) extends PostActionDtoOld



/** Info on all ratings on a certain action, grouped and sorted in
 *  various manners.
 */
abstract class RatingsOnAction {

  /** The most recent rating, by authenticated users. */
  def mostRecentByUserId: collection.Map[String, Rating]

  /** The most recent rating, by non authenticated users. */
  // COULD rename to ...ByGuestId
  def mostRecentByNonAuLoginId: collection.Map[String, Rating]

  /** The most recent ratings, for all non authenticated users,
   *  grouped by IP address.
   */
  // COULD rename to ...ByIdtyId
  def allRecentByNonAuIp: collection.Map[String, List[Rating]]

  /** The most recent version of the specified rating.
   *  When you rate an action a second time, the most recent rating
   *  overwrites the older one.
   */
  def curVersionOf(rating: Rating): Rating
}



object FlagReason extends Enumeration {
  type FlagReason = Value
  val Spam, Illegal, /* Copyright Violation */ CopyVio, Other = Value
}



case class Flag(
  id: String,
  postId: String,
  loginId: String,
  newIp: Option[String],
  ctime: ju.Date,
  reason: FlagReason,
  details: String
) extends PostActionDtoOld {
  override def textLengthUtf8: Int = details.getBytes("UTF-8").length
}



case object CreatePostAction {

  def newTitleBySystem(text: String, creationDati: ju.Date) =
    newTitle(text, creationDati, loginId = SystemUser.Login.id,
      approval = Some(Approval.AuthoritativeUser))

  def newPageBodyBySystem(text: String, creationDati: ju.Date, pageRole: PageRole) =
    newPageBody(text, creationDati, pageRole, loginId = SystemUser.Login.id,
      approval = Some(Approval.AuthoritativeUser))


  def newTitle(
        text: String,
        creationDati: ju.Date,
        loginId: String,
        approval: Option[Approval]) =
    CreatePostAction(Page.TitleId, Page.TitleId, creationDati,
      loginId = loginId,
      newIp = None,
      text = text,
      markup = Markup.DefaultForPageTitle.id,
      approval = approval)


  def newPageBody(
        text: String,
        creationDati: ju.Date,
        pageRole: PageRole,
        loginId: String,
        approval: Option[Approval]) =
    CreatePostAction(Page.BodyId, Page.BodyId, creationDati,
      loginId = loginId,
      newIp = None,
      text = text,
      markup = Markup.defaultForPageBody(pageRole).id,
      approval = approval)

}



case class CreatePostAction(

  // is "?" if unknown, or e.g. "?x" if it's unknown.
  // COULD replace w case classes e.g. IdKnown(id)/IdPending(tmpId)/IdUnknown,
  // then it would not be possible to forget to check for "?".
  id: String,
  parent: String,
  ctime: ju.Date,
  loginId: String,
  newIp: Option[String],
  text: String,

  /** The markup language to use when rendering this post.
   */
  markup: String,

  /**
   * Defined iff this post was approved on creation, and clarifies why it was.
   */
  approval: Option[Approval],

  /** If defined, this is an inline comment and the value
   *  specifies where in the parent post it is to be placed.
   */
  where: Option[String] = None   // COULD move to separate Meta post?
                                 // Benefits: Editing and versioning of
                                 // `where', without affecting this Post.text.
                                // Benefit 2: There could be > 1 meta-Where
                                // for each post, so you could make e.g. a
                                // generic comment that results in ...
                                // ...?? arrows to e.g. 3 other comments ??
) extends MaybeApproval {
  override def textLengthUtf8: Int = text.getBytes("UTF-8").length
}



case class Edit (
  id: String,
  postId: String,
  ctime: ju.Date,
  loginId: String,
  newIp: Option[String],
  text: String,

  /** Changes the markup henceforth applied to postId's text.
   *
   * None means reuse the current markup.
   */
  newMarkup: Option[String],

  /**
   * If the related post is to be automatically approved, when this
   * edit is auto applied. (Example: a moderator edits a comment
   * that is already approved, then the edit would be
   * auto applied, and the related post would be approved implicitly,
   * (since it was already approved, and a *moderator* did the edit.))
   */
  approval: Option[Approval],

  /**
   * If this edit was applied automatically on creation, e.g. because
   * someone edited his/her own comment.
   *
   * Currently not in use. And I'd have to refactor page-actions-smart.scala
   * fairly much for `autoApplied`to work, since currently all appl info
   * is instead handled via EditApp:s.
   *   - Perhaps easier to remove this field, and construct
   * an additional EditApp action when reading an Edit from database,
   * if the db says it was auto approved? But I might need this field
   * anyway, when *saving* an edit, so the db knows it should mark it as
   * auto applied.
   */
  autoApplied: Boolean
) extends MaybeApproval {
  override def textLengthUtf8: Int = text.getBytes("UTF-8").length

  // An edit that hasn't been applied cannot have been approved.
  // (It might have been applied, but not approved, however, if a
  // user edits his/her own comment, and the changes are then pending
  // moderator review.)
  require(approval.isEmpty || autoApplied)
}


/** Edit applications (i.e. when edits are applied).
 *
 *  COULD make generic: applying a Post means it's published.
 *  Applying an Edit means the relevant Post is edited.
 *  Applying a Flag means the relevant Post is hidden.
 *  Applying a Delete means whatever is whatever-happens-when-it's-deleted.
 *  And each Action could have a applied=true/false field?
 *  so they can be applied directly on creation?
 */
case class EditApp(
  id: String,
  editId: String,
  loginId: String,
  newIp: Option[String],
  ctime: ju.Date,
  approval: Option[Approval],

  /** The text after the edit was applied. Needed, in case an `Edit'
   *  contains a diff, not the resulting text itself. Then we'd better not
   *  find the resulting text by applying the diff to the previous
   *  version, more than once -- that wouldn't be future compatible: the diff
   *  algorithm might functioni differently depending on software version
   *  (e.g. because of bugs).
   *  So, only apply the diff algorithm once, to find the EddidApplied.result,
   *  and thereafter always use EditApplied.result.
   *
   *  Update: What! I really don't think the diff alg will change in the
   *  future! Anyone changing the diff file format would be insane,
   *  and would not have been able to write a diff alg at all.
   *  So `result' is *not* needed, except for perhaps improving performance.
   *
   *  COULD change to an Option[String] and define it only one time out of ten?
   *  Then the entire post would be duplicated only 1 time out of 10 times,
   *  and that would hardly affect storage space requirements,
   *  but at the same time improving performance reasonably much ??
   *  Or store cached post texts in a dedicated db table.
   */
  result: String
) extends MaybeApproval

/** Deletes an action. When actionId (well, postId right now)
 *  is a post, it won't be rendered. If `wholeTree', no reply is shown either.
 *  If actionId is an EditApp, that edit is undone. BAD??! Too complicated??
 *    What does it mean to Delete an EditApp Delete:ion?
 *    Of course it would mean that the Deletion was undone,
 *    that is, Undo, that is, the EditApp would be in effect again,
 *    that is, the Edit would be in effect again.
 *    However I think this is too complicated.
 *    Perhaps it's easier to introduce a Revert class.
 *    And if there are many Apply:s and Revert:s for an Edit,
 *    then the most recent Apply or Revert is in effect.
 *    Benefit: You'd never need to walk along a chain of Delete:s,
 *    to find out which EditApp or Post was actually deleted.
 *    ????
 *    Perhaps introduce a class ToggleExistence, and the very last
 *    instance determines if a Post is deleted or restored.
 *  --- Not implemented: --------
 *  If `wholeTree', all edit applications from actionId and up to
 *  delete.ctime are undone.
 *  If actionId is an Edit, the edit will no longer appear in the list of
 *  edits you can choose to apply.
 *  If `wholeTree', no Edit from actionId up to delete.ctime will appear
 *  in the list of edits that can be applied.
 *  -----------------------------
 */
case class Delete(
  id: String,
  postId: String,
  loginId: String,
  newIp: Option[String],
  ctime: ju.Date,
  wholeTree: Boolean,  // COULD rename to `recursively'?
  reason: String  // COULD replace with a Post that is a reply to this Delete?
) extends PostActionDtoOld {
  override def textLengthUtf8: Int = reason.getBytes("UTF-8").length
}


// COULD make a Deletion class, and a DelApp (deletion application) class.
// Then sometimes e.g. 3 people must delete a post for it to really
// be deleted. Perhaps DelSug = deletion suggestion?
// And rename Edit to EditSug?
// EditSug & EditApp, + DelSug & DelApp - or join these 4 classes to 2 generic?
// What about Flag and FlagApp? Eventually if flagged as spam e.g. 2 times
// then the post would be automatically hidden?
// What about only one ActionApp class, that applies the relevant action?
// But then e.g. EditApp.result (resulting text) would be gone!?
// And DelApp.wholeTree ... and FlagApp.reason + details.
// Perhaps better have many small specialized classes.


/**
 * Approves or rejects e.g. comments and edits to `targetId`.
 */
case class ReviewPostAction(
  id: String,
  targetId: String,
  loginId: String,
  newIp: Option[String],
  ctime: ju.Date,
  approval: Option[Approval]) extends MaybeApproval {
}


/**
 * Moderators and the computer review posts and might approve them
 * Only the `Manual` approval is done manually by a human,
 * all others happen automatically, done by the computer.
 * (Should I prefix them with 'Auto'?)
 */
sealed abstract class Approval
object Approval {

  /**
   * The first few posts of a new user are approved preliminarily.
   * (An admin is notified though and might decide to delete the posts.)
   */
  case object Preliminary extends Approval

  /**
   * A user that has posted many useful comments will have a few of
   * his/her next comments approved automatically, and no admin is nodified.
   */
  case object WellBehavedUser extends Approval

  /**
   * Posts by admins and moderators are always automatically approved.
   */
  case object AuthoritativeUser extends Approval

  /**
   * When an authoritative user manually approved something.
   */
  case object Manual extends Approval


  def parse(text: String): Approval = text match {
    case "Preliminary" => Preliminary
    case "WellBehavedUser" => WellBehavedUser
    case "AuthoritativeUser" => AuthoritativeUser
    case "Manual" => Manual
    case _ => illArgErr("DwE931k35", s"Bad approval value: `$text'")
  }

}



// vim: fdm=marker et ts=2 sw=2 tw=80 fo=tcqwn list
