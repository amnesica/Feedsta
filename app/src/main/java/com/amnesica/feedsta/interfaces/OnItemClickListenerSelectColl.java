package com.amnesica.feedsta.interfaces;

/** Interface used by BtmSheetDialogSelectCollection to communicate with adapter */
public interface OnItemClickListenerSelectColl {
  /**
   * Simple click on bookmark to go to PostFragment
   *
   * @param position int
   */
  void onItemClick(int position);
}
