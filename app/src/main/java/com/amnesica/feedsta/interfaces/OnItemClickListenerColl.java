package com.amnesica.feedsta.interfaces;

import com.amnesica.feedsta.models.Collection;

import java.util.ArrayList;
import java.util.List;

/**
 * Interface used by CollectionsFragment to communicate with adapter
 */
public interface OnItemClickListenerColl {
    // simple click on collection to go to SingleCollectionFragment
    void onItemClick(int position);

    // remove all bookmarks of collection from storage
    void removeBookmarksOfCollectionFromStorage(ArrayList<Collection> listCollectionsToRemove);

    // rename category in all bookmarks of category
    void renameCategory(List<Collection> listCollectionsToRename);

    // download all bookmarks in selected collections
    void downloadSelectedCollections(ArrayList<Collection> selectedCollections);

    // reset category in all bookmarks of selected collection
    void resetCollectionInAllSelectedBookmarksOfCategory(List<Collection> listCollectionsToReset);
}
