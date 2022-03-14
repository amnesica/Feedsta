package com.amnesica.feedsta.interfaces;

import com.amnesica.feedsta.helper.EditBookmarksType;

/**
 * Interface used to communicate between fragments
 */
public interface FragmentCallback {
    /**
     * Sets the category of a post or a list of posts
     *
     * @param category String
     * @param editMode EditBookmarksType
     */
    void savePostOrListToCollection(String category, EditBookmarksType editMode);

    /**
     * Opens the BtmSheetDialogAddCollection fragment
     *
     * @param editMode EditBookmarksType
     */
    void openBtmSheetDialogAddCollection(EditBookmarksType editMode);
}