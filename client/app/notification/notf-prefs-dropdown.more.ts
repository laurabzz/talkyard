/*
 * Copyright (c) 2014-2018 Kaj Magnus Lindberg
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

/// <reference path="../ReactStore.ts" />
/// <reference path="../react-elements/name-login-btns.ts" />
/// <reference path="../utils/DropdownModal.ts" />
/// <reference path="../util/ExplainingDropdown.ts" />
/// <reference path="../widgets.ts" />
/// <reference path="../oop-methods.ts" />

//------------------------------------------------------------------------------
   namespace debiki2.notfs {
//------------------------------------------------------------------------------

const r = ReactDOMFactories;
const DropdownModal = utils.DropdownModal;
const ExplainingListItem = util.ExplainingListItem;


let notfsLevelDropdownModal;

export function openNotfPrefDropdown(openButton, props) {
                                                //subject: NotfSubject, currentLevel: NotfLevel) {
  if (!notfsLevelDropdownModal) {
    notfsLevelDropdownModal = ReactDOM.render(NotfsLevelDropdownModal(), utils.makeMountNode());
  }
  notfsLevelDropdownModal.openAtFor(openButton, props); //subject, currentLevel);
}


// some dupl code [6KUW24]
const NotfsLevelDropdownModal = createComponent({
  displayName: 'NotfsLevelDropdownModal',

  mixins: [StoreListenerMixin],

  getInitialState: function () {
    return {
      isOpen: false,
      store: debiki2.ReactStore.allData(),
    };
  },

  onChange: function() {
    this.setState({ store: debiki2.ReactStore.allData() });
  },

  // dupl code [6KUW24]
  openAtFor: function(at, props: { pref: MyAndInheritedNotfPref, me: Myself }) {
                                // subject: NotfSubject, currentLevel: NotfLevel
    const rect = at.getBoundingClientRect();
    this.setState({
      isOpen: true,
      atX: rect.left,
      atY: rect.bottom,
      subject: props.pref,
      //currentLevel: currentLevel,
    });
  },

  close: function() {
    this.setState({ isOpen: false });
  },

  setNotfLevel: function(newLevel) {
    const subject: NotfSubject = this.state.subject;
    if (subject.pageId) {
      ReactActions.setPageNoftLevel(newLevel);
    }
    else if (subject.tagLabel) {
      Server.setTagNotfLevel(subject.tagLabel, newLevel);
    }
    else {
      die('EsE4KG8F2');
    }
    this.close();
  },

  render: function() {
    const state = this.state;
    const store: Store = this.state.store;
    const subject: NotfSubject = this.state.subject;
    const currentLevel: NotfLevel = this.state.currentLevel || NotfLevel.Normal;
    let watchingAllListItem;
    let watchingFirstListItem;
    let mutedListItem;

    if (state.isOpen) {
      dieIf(!subject.pageId && !subject.tagLabel, 'EsE4GK02');

      watchingAllListItem = !subject.pageId ? null :
        ExplainingListItem({
          active: currentLevel === NotfLevel.WatchingAll,
          title: r.span({ className: 'e_NtfAll' }, t.nl.WatchingAll),
          text: subject.tagLabel ? t.nl.WatchingAllTag : t.nl.WatchingAllTopic,
          onSelect: () => this.setNotfLevel(NotfLevel.WatchingAll) });
      watchingFirstListItem = !subject.tagLabel ? null :
        ExplainingListItem({
          active: currentLevel === NotfLevel.WatchingFirst,
          title: r.span({ className: 'e_NtfFst' }, t.nl.WatchingFirst),
          text: t.nl.WatchingFirstTag,
          onSelect: () => this.setNotfLevel(NotfLevel.WatchingFirst) });
      mutedListItem =
        ExplainingListItem({
          active: currentLevel === NotfLevel.Muted,
          title: r.span({ className: 'e_NtfMtd'  }, t.nl.Muted),
          text: t.nl.MutedTopic,
          onSelect: () => this.setNotfLevel(NotfLevel.Muted) });
    }

    return (
      DropdownModal({ show: state.isOpen, onHide: this.close, atX: state.atX, atY: state.atY,
          pullLeft: true },
        watchingAllListItem,
        watchingFirstListItem,
        /*
        ExplainingListItem({
          active: currentLevel === NotfLevel.Tracking,
          title: r.span({ className: '' }, t.nl.Tracking),
          text: r.span({}, t.nl.TrackingTopic),
          onSelect: () => this.setNotfLevel(NotfLevel.Tracking) }),
          */
        ExplainingListItem({
          active: currentLevel === NotfLevel.Normal,
          title: r.span({ className: '' }, t.nl.Normal),
          text: r.span({}, t.nl.NormalTopic_1, r.samp({}, t.nl.NormalTopic_2), '.'),
          onSelect: () => this.setNotfLevel(NotfLevel.Normal) }),
        mutedListItem));
  }
});


//------------------------------------------------------------------------------
   }
//------------------------------------------------------------------------------
// vim: fdm=marker et ts=2 sw=2 tw=0 fo=tcqwn list
