package com.amnesica.feedsta.interfaces;

import com.amnesica.feedsta.helper.EditBookmarksType;

/**
 * Interface used to communicate between fragments
 */
public interface FragmentCallback {
    // sets the category of a post or a list of posts
    void savePostOrListToCollection(String category, EditBookmarksType editMode);

    // opens the BtmSheetDialogAddCollection fragment
    void openBtmSheetDialogAddCollection(EditBookmarksType editMode);
}