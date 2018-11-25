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

/// <reference path="../slim-bundle.d.ts" />

//------------------------------------------------------------------------------
   namespace debiki2.notfs {
//------------------------------------------------------------------------------

const r = ReactDOMFactories;
const DropdownModal = utils.DropdownModal;
const ExplainingListItem = util.ExplainingListItem;


let notfsLevelDropdownModal;

export function openNotfPrefDropdown(atRect, props: {
      target: NotfPrefTarget, saveFn?: (newLevel: NotfLevel) => void }) {
                                                //subject: NotfSubject, currentLevel: NotfLevel) {
  if (!notfsLevelDropdownModal) {
    notfsLevelDropdownModal = ReactDOM.render(NotfsLevelDropdownModal(), utils.makeMountNode());
  }
  notfsLevelDropdownModal.openAtFor(atRect, props); //subject, currentLevel);
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
  openAtFor: function(rect, props: {
      target: NotfPrefTarget, saveFn?: (newLevel: NotfLevel) => void }) {
    this.setState({
      isOpen: true,
      atX: rect.left,
      atY: rect.bottom,
      target: props.target,
      saveFn: props.saveFn,
    });
  },

  close: function() {
    this.setState({ isOpen: false, pref: undefined, me: undefined });
  },

  saveNotfLevel: function(notfLevel) {
    if (this.state.saveFn) {
      this.state.saveFn(notfLevel);
    }
    else {
      const store: Store = this.state.store;
      const me: Myself = store.me;
      const target: NotfPrefTarget = this.state.target;
      Server.saveContNotfPrefUpdStore(me.id, target, notfLevel);
    }
    /*
    if (pref.wholeSite) {
    }
    else if (pref.pagesInCategoryId) {
    }
    else if (pref.pageId) {
      ReactActions.setPageNoftLevel(newLevel);  // xx rm
    }
    else if (pref.tagLabel) {
      Server.setTagNotfLevel(subject.tagLabel, newLevel);  // xx rm
    }
    else {
      die('EsE4KG8F2');
    }*/
    this.close();
  },

  render: function() {
    const state = this.state;
    const store: Store = this.state.store;
    let everyPostListItem;
    let newTopicsListItem;
    let normalListItem;
    let hushedListItem;
    let mutedListItem;

    if (state.isOpen) {
      const target: NotfPrefTarget = this.state.target;
      const pref: EffContNotfPref = notfPrefTarget_findEffNotfPref(target, store);
      const inheritedLevel = pref.inheritedNotfPref && pref.inheritedNotfPref.notfLevel;
      const currentLevel: NotfLevel = pref.notfLevel || inheritedLevel;
      dieIf(!pref.pageId && !pref.wholeSite, 'EsE4GK02');   // &&!pref.tagLabel

      console.log("Debug:\n" + JSON.stringify(pref));
      everyPostListItem =
        ExplainingListItem({
          active: currentLevel === NotfLevel.WatchingAll,
          title: r.span({ className: 'e_NtfAll' }, t.nl.WatchingAll),
          text: notfLevel_descr(NotfLevel.WatchingAll, pref),
          onSelect: () => this.saveNotfLevel(NotfLevel.WatchingAll) });
      newTopicsListItem = pref.pageId ? null :
        ExplainingListItem({
          active: currentLevel === NotfLevel.WatchingFirst,
          title: r.span({ className: 'e_NtfFst' }, t.nl.WatchingFirst),
          text: notfLevel_descr(NotfLevel.WatchingFirst, pref),
          onSelect: () => this.saveNotfLevel(NotfLevel.WatchingFirst) });
      normalListItem =
        ExplainingListItem({
          active: currentLevel === NotfLevel.Normal,
          title: r.span({ className: '' }, t.nl.Normal),
          text: t.nl.NormalDescr,
          onSelect: () => this.saveNotfLevel(NotfLevel.Normal) }),
      hushedListItem =
        ExplainingListItem({
          active: currentLevel === NotfLevel.Hushed,
          title: r.span({ className: '' }, t.nl.Hushed),
          text: t.nl.HushedDescr,
          onSelect: () => this.saveNotfLevel(NotfLevel.Hushed) }),
      mutedListItem =
        ExplainingListItem({
          active: currentLevel === NotfLevel.Muted,
          title: r.span({ className: 'e_NtfMtd'  }, t.nl.Muted),
          text: t.nl.MutedTopic,
          onSelect: () => this.saveNotfLevel(NotfLevel.Muted) });
    }

    return (
      DropdownModal({ show: state.isOpen, onHide: this.close, atX: state.atX, atY: state.atY,
          pullLeft: true },
        everyPostListItem,
        newTopicsListItem,
        normalListItem,
        hushedListItem,
        mutedListItem));
  }
});


//------------------------------------------------------------------------------
   }
//------------------------------------------------------------------------------
// vim: fdm=marker et ts=2 sw=2 tw=0 fo=tcqwn list
