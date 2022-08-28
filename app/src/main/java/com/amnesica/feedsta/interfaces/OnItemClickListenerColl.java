package com.amnesica.feedsta.interfaces;

import com.amnesica.feedsta.models.Collection;

import java.util.ArrayList;
import java.util.List;

/** Interface used by CollectionsFragment to communicate with adapter RecViewAdapterCollections */
public interface OnItemClickListenerColl {
  /**
   * Simple click on collection to go to SingleCollectionFragment
   *
   * @param position int
   */
  void onItemClick(int position);

  /**
   * Remove all bookmarks of collection from storage
   *
   * @param listCollectionsToRemove ArrayList<Collection>
   */
  void removeBookmarksOfCollectionFromStorage(ArrayList<Collection> listCollectionsToRemove);

  /**
   * Rename category in all bookmarks of category
   *
   * @param listCollectionsToRename List<Collection>
   */
  void renameCategory(List<Collection> listCollectionsToRename);

  /**
   * Download all bookmarks in selected collections
   *
   * @param selectedCollections ArrayList<Collection>
   */
  void downloadSelectedCollections(ArrayList<Collection> selectedCollections);

  /**
   * Reset category in all bookmarks of selected collection
   *
   * @param listCollectionsToReset List<Collection>
   */
  void resetCollectionInAllSelectedBookmarksOfCategory(List<Collection> listCollectionsToReset);
}
