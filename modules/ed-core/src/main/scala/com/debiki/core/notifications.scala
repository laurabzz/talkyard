/**
 * Copyright (C) 2014 Kaj Magnus Lindberg
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.debiki.core

import com.debiki.core.Prelude._
import java.{util => ju}


/** Notifications about e.g. new replies, @mentions, someone liked your post.
  * Or a @mention to delete because someone edited the post and removed the
  * mention, so we're no longer supposed to send an email about it.
  */
case class Notifications(
  toCreate: Seq[Notification] = Nil,
  toDelete: Seq[NotificationToDelete] = Nil) {
  def isEmpty: Boolean = toCreate.isEmpty && toDelete.isEmpty
}

object Notifications {
  val None = Notifications(Nil, Nil)
}



sealed abstract class NotificationType(val IntValue: Int) { def toInt: Int = IntValue }

// Could:  enum / 1000 = notification type, enum % 1000 = how many got notified?
// No, instead, case class:  NotfTypeNumNotified(notfType, numNotified: Int)
object NotificationType {
  case object DirectReply extends NotificationType(1)
  case object Mention extends NotificationType(2)  // –> 1000, group mention 1001 ...
  // + Quote
  case object Message extends NotificationType(4)
  case object NewPost extends NotificationType(5)
  // + NewTopic  [REFACTORNOTFS]
  // + TopicProgress
  // + QuestionAnswered
  // + TopicDone
  // + TopicClosed
  case object PostTagged extends NotificationType(6)

  // + GoupMention: 1001 ... 1999 = group size + 1000

  def fromInt(value: Int): Option[NotificationType] = Some(value match {
    case DirectReply.IntValue => DirectReply
    case Mention.IntValue => Mention
    case Message.IntValue => Message
    case NewPost.IntValue => NewPost
    case PostTagged.IntValue => PostTagged
    case _ => return None
  })
}



sealed abstract class Notification {
  def siteId: SiteId
  def id: NotificationId
  def createdAt: ju.Date
  def tyype: NotificationType
  def toUserId: UserId
  def emailId: Option[EmailId]
  def emailStatus: NotfEmailStatus
  def seenAt: Option[ju.Date]
}


object Notification {

  /** A reply, @mention, or new post in a topic you're watching.
    */
  case class NewPost(
    notfType: NotificationType,
    siteId: SiteId,
    id: NotificationId,
    createdAt: ju.Date,
    uniquePostId: PostId,
    byUserId: UserId,
    toUserId: UserId,
    emailId: Option[EmailId] = None,
    emailStatus: NotfEmailStatus = NotfEmailStatus.Undecided,
    seenAt: Option[ju.Date] = None) extends Notification {
    override def tyype: NotificationType = notfType
  }

  /*
  case class Approved extends Notification
  case class Quote extends Notification
  case class Edit extends Notification
  case class LikeVote extends Notification
  case class WrongVote extends Notification
  case class OffTopicVote extends Notification */
}



sealed abstract class NotificationToDelete

object NotificationToDelete {

  case class MentionToDelete(
    siteId: SiteId,
    uniquePostId: PostId,
    toUserId: UserId) extends NotificationToDelete

  case class NewPostToDelete(
    siteId: SiteId,
    uniquePostId: PostId) extends NotificationToDelete

}


sealed abstract class NotfEmailStatus(val IntValue: Int ) { def toInt: Int = IntValue }
object NotfEmailStatus {

  /** This notification has not yet been processed; we have yet to decide if to send an email. */
  case object Undecided extends NotfEmailStatus(1)

  /** We've decided to not send any email for this notf (perhaps the user has seen it already) */
  case object Skipped extends NotfEmailStatus(2)

  /** Email created, will soon be sent, or has already been sent. */
  case object Created extends NotfEmailStatus(3)

  // 4 = email sent? Not in use, right now.

  def fromInt(value: Int): Option[NotfEmailStatus] = Some(value match {
    case Undecided.IntValue => Undecided
    case Created.IntValue => Created
    case Skipped.IntValue => Skipped
    case _ => return None
  })
}



sealed abstract class NotfLevel(val IntVal: Int) {
  def toInt: Int = IntVal
}


/** Sync with database constraint function in `r__functions.sql`. [7KJE0W3]
  */
object NotfLevel {


  /** Like EveryPost, but also notified of edits.
    *
    * If this is desirable only for, say, the Orig Post (e.g. to get notified if someone
    * edits one's blog post = the orig post), then don't add more
    * notification levels here. Instead, make it possible to subscribe for notifications
    * on individual posts, and sub-threads, in addition to on whole pages / categories / tags.
    * This could be a power user feature in the More... post action dropdown.
    */
  case object EveryPostAllEdits extends NotfLevel(9)

  /** Notified about every new post (incl topic status changes).
    */
  case object WatchingAll extends NotfLevel(8)    ; RENAME // to EveryPost

  /** For questions: Notified about new answers suggestions (that is. orig post replies).
    * For questions, problems, ideas: Notified about progress posts and status changes,
    * e.g. status changed from Planned to Doing.
    */
  case object TopicProgress extends NotfLevel(7)

  /** Notified if an answer to a Question topic, is accepted as the correct answer.
    * Or if a Problem is marked as solved. Or an Idea marked as Implemented.
    * Or if topic closed (won't get solved / done).
    */
  case object TopicSolved extends NotfLevel(6)

  /** Notified about new topics. (For categories.)
    */
  case object WatchingFirst extends NotfLevel(5)  ; RENAME // to NewTopics

  /** Like Normal, plus highlights the topic in the topic list, if new posts.
    */
  case object Tracking extends NotfLevel(4)  ; RENAME // to Highlight ?

  /** Notified of @mentions and posts in one's sub threads (incl direct replies).
    */
  case object Normal extends NotfLevel(3)

  // If doesn't matter.
  val DoesNotMatterHere: Normal.type = Normal

  /** Notified of @mentions and direct replies only.
    */
  case object Hushed extends NotfLevel(2)

  /** No notifications for this page.
    */
  case object Muted extends NotfLevel(1)

  def fromInt(value: Int): Option[NotfLevel] = Some(value match {
    case EveryPostAllEdits.IntVal => EveryPostAllEdits
    case WatchingAll.IntVal => WatchingAll
    case TopicProgress.IntVal => TopicProgress
    case TopicSolved.IntVal => TopicSolved
    case WatchingFirst.IntVal => WatchingFirst
    case Tracking.IntVal => Tracking
    case Normal.IntVal => Normal
    case Hushed.IntVal => Hushed
    case Muted.IntVal => Muted
    case _ => return None
  })

}


/**
  * @param peopleId — the group or individual these settings concern.
  * @param notfLevel
  * @param pageId — if these notification settings are for a single page only.
  * @param pagesInCategoryId — the settings apply to all pages in this category.
  * @param wholeSite — the group's or member's default settings for pages across the whole site
  */
case class PageNotfPref(   // ? RENAME to ContNotfPref?  No: NotfPrefAboutContent?  (will also be an ...AboutGroup) or don't?
  peopleId: UserId,  // RENAME to memberId, + db column.  [pps]
  notfLevel: NotfLevel,
  pageId: Option[PageId] = None,   // RENAME to forPageId
  pagesInCategoryId: Option[CategoryId] = None,  // not yet impl [7KBR2AF5]   // RENAME to forCategoryId
  //pagesWithTagLabelId: Option[TagLabelId] = None, — later
  wholeSite: Boolean = false) {    // RENAME to forWholeSite

  require(pageId.isDefined.toZeroOne + pagesInCategoryId.isDefined.toZeroOne +
    wholeSite.toZeroOne == 1, "TyE2BKP053")

  if (pageId.isDefined) {
    require(notfLevel != NotfLevel.WatchingFirst)
  }
}


case class PageNotfLevels(   // xx rm
  forPage: Option[NotfLevel] = None,
  forCategory: Option[NotfLevel] = None,
  forWholeSite: Option[NotfLevel] = None) {

  /** The most specific notf level (per page), overrides the less specific (category, whole site).
    */
  def effectiveNotfLevel: Option[NotfLevel] =
    // Tested here: [TyT7KSJQ296]
    forPage.orElse(forCategory.orElse(forWholeSite))

}


/**
  * @param ownPageNotfLevel Any notf level one has set, directly on the page or category (or whole site).
  * @param inheritedPref Inherited from groups one is in, or from one's own category setting, if
  *                      is for a page.
  */
case class EffPageNotfPref(
  pageId: PageId,
  ownPageNotfLevel: Option[NotfLevel],
  inheritedPref: Option[PageNotfPref])

case class EffSiteNotfPref(
  ownSitePref: Option[PageNotfPref],
  inheritedPref: Option[PageNotfPref])

case class OwnAndGropsContNotfPrefs(
  memberId: MemberId,
  ownPrefsByPageId: Map[PageId, PageNotfPref],
  ownPrefsByCatId: Map[CategoryId, PageNotfPref],
  ownSitePref: Option[PageNotfPref],
  groupsMaxPrefsByPageId: Map[PageId, PageNotfPref],
  groupsMaxPrefsByCatId: Map[CategoryId, PageNotfPref],
  groupsMaxSitePref: Option[PageNotfPref]) {

  def maxOwnCatLevel: Option[PageNotfPref] =
    ownPrefsByCatId.valuesIterator.reduceOption(
      (a, b) => if (a.notfLevel.toInt > b.notfLevel.toInt) a else b)

  def maxGroupsCatLevel: Option[PageNotfPref] =
    groupsMaxPrefsByCatId.valuesIterator.reduceOption(
      (a, b) => if (a.notfLevel.toInt > b.notfLevel.toInt) a else b)

  def effPageNotfPref(pageId: PageId): EffPageNotfPref = {
    dieIf(ownPrefsByPageId.size > 1, "TyE4ABRT02")
    dieIf(groupsMaxPrefsByPageId.size > 1, "TyE4ABRT03")
    var inheritedPref = groupsMaxPrefsByPageId.get(pageId)
    if (inheritedPref.isEmpty) {
      val myCatPref = maxOwnCatLevel
      if (myCatPref.isDefined) inheritedPref = myCatPref
    }
    if (inheritedPref.isEmpty) {
      val groupsCatLevel = maxGroupsCatLevel
      if (groupsCatLevel.isDefined) inheritedPref = groupsCatLevel
    }
    if (inheritedPref.isEmpty && ownSitePref.isDefined) {
      inheritedPref = ownSitePref
    }
    if (inheritedPref.isEmpty && groupsMaxSitePref.isDefined) {
      inheritedPref = groupsMaxSitePref
    }
    EffPageNotfPref(
        pageId, ownPageNotfLevel = ownPrefsByPageId.get(pageId).map(_.notfLevel), inheritedPref)
  }

}


case object OwnAndGropsContNotfPrefs {

  def apply(memberId: MemberId, prefs: Seq[PageNotfPref]): OwnAndGropsContNotfPrefs = {
    val myPagePrefs = prefs.filter(p => p.pageId.isDefined && p.peopleId == memberId)
    val ownPrefsByPageId =
      myPagePrefs.groupBy(_.pageId.getOrDie("TyE5BR025")).mapValues(_.head)

    val myCatPrefs = prefs.filter(p => p.pagesInCategoryId.isDefined && p.peopleId == memberId)
    val ownPrefsByCatId =
      myCatPrefs.groupBy(_.pagesInCategoryId.getOrDie("TyE5BR025")).mapValues(_.head)

    val ownSitePref = prefs.find(p => p.wholeSite && p.peopleId == memberId)

    val groupsMaxSitePref = prefs.filter(p => p.wholeSite && p.peopleId != memberId)
      .reduceOption((a, b) => if (a.notfLevel.toInt > b.notfLevel.toInt) a else b)
    new OwnAndGropsContNotfPrefs(
      memberId,
      ownPrefsByPageId = ownPrefsByPageId,
      ownPrefsByCatId = ownPrefsByCatId,
      ownSitePref = ownSitePref,
      groupsMaxPrefsByPageId = Map.empty,
      groupsMaxPrefsByCatId = Map.empty,
      groupsMaxSitePref = groupsMaxSitePref)
  }

  /*
  def apply(myPrefs: Seq[PageNotfPref], groupsPrefs: Seq[PageNotfPref]): MembersNotfPrefs = {
    val mySitePref = myPrefs.find(_.wholeSite)
    val myCatPrefs = myPrefs.filter(_.pagesInCategoryId.isDefined)
    val myNotfLevelsByCatId =
      myCatPrefs.groupBy(_.pagesInCategoryId.getOrDie("TyE5BR025")).mapValues(_.head.notfLevel)

    val groupsMaxNotfSitePref = groupsPrefs.filter(_.wholeSite).reduceOption((a, b) => {
      if (a.notfLevel.toInt > b.notfLevel.toInt) a else b
    })

    new MembersNotfPrefs(
      mySiteNotfLevel = mySitePref.map(_.notfLevel),
      myCategoryNotfLevels = myNotfLevelsByCatId,
      groupsMaxNotfSitePref = groupsMaxNotfSitePref,
      groupsMaxCatPrefs = Map.empty)
  } */

}
