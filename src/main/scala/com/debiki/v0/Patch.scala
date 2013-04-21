/**
 * Copyright (c) 2012-2013 Kaj Magnus Lindberg (born 1979)
 */

package com.debiki.v0

import java.{util => ju}
import collection.{immutable => imm, mutable => mut}
import Prelude._
import PageParts._
import FlagReason.FlagReason
import com.debiki.v0.{PostActionPayload => PAP}



class Patch(debate: PageParts, val edit: PostActionDto[PAP.EditPost])
  extends PostAction[PAP.EditPost](debate, edit) with MaybeApproval with PostActionActedUpon {

  def post = debate.getPost(edit.postId)
  def post_! = debate.getPost_!(edit.postId)

  def patchText = edit.payload.text
  def newMarkup = edit.payload.newMarkup

  def isDeleted = false // for now. Later: Add PostActionPayload.DeleteEdit?
  def deletedAt: Option[ju.Date] = None // for now

  def isPending = !isApplied && !isDeleted

  def approval = edit.payload.approval

  private def _initiallyAutoApplied = edit.payload.autoApplied

  /**
   * The result of applying patchText to the related Post, as it was
   * when this Edit was submitted (with other later edits ignored, even
   * if they were actually applied before this edit).
   */
  def intendedResult: String =
    // Apply edits to the related post up to this.creationDati,
    // then apply this edit, and return the resulting text.
    "(not implemented)"

  /**
   * The result of applying patchText to the related Post,
   * taking into account other Edits that were applied before this edit,
   * and they made it impossible to correctly
   * apply the patchText of this Edit.
   */
  def actualResult: Option[String] =
    // Apply edits to the related post up to this.applicationDati,
    // then apply this edit, and return the resulting text.
    if (isApplied) Some("(not implemented)") else None


  // COULD let isApplied, applicationDati, applierLoginId, applicationActionId
  // be functions, and remember only Some(applicationAction)?
  val (isApplied, applicationDati,
      applierUserId, applicationActionId,
      isReverted, revertionDati)
        : (Boolean, Option[ju.Date], Option[String], Option[String],
          Boolean, Option[ju.Date]) = {
    val allEditApps = page.editAppsByEdit(id)
    if (allEditApps isEmpty) {
      // This edit might have been auto applied, and deleted & reverted later.
      (_initiallyAutoApplied, isDeleted) match {
        case (false, _) => (false, None, None, None, false, None)
        case (true, false) =>
          // Auto applied at creation. This edit itself is its own application.
          (true, Some(creationDati), Some(userId), Some(id), false, None)
        case (true, true) =>
          // Auto applied, but later deleted and implicitly reverted.
          (false, None, None, None, true, deletedAt)
      }
    } else {
      // Consider the most recent EditApp only. If it hasn't been deleted,
      // this Edit is in effect. However, if the EditApp has been deleted,
      // there should be no other EditApp that has not also been deleted,
      // because you cannot apply an edit that's already been applied.
      val lastEditApp = allEditApps.maxBy(_.ctime.getTime)
      val revertionDati =
        (None // for now. Later, check Undo,
              // was before, broken now:  page.deletionFor(lastEditApp.id).map(_.ctime)
              // perhaps in the future:  lastEditApp.undoneAt
          orElse deletedAt)
      if (revertionDati isEmpty)
        (true, Some(lastEditApp.ctime),
           Some(lastEditApp.userId), Some(lastEditApp.id), false, None)
      else
        (false, None, None, None, true, Some(revertionDati.get))
    }
  }

}


// vim: fdm=marker et ts=2 sw=2 fo=tcqwn list
