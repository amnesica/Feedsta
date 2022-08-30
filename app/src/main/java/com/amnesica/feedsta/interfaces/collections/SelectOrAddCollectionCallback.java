package com.amnesica.feedsta.interfaces.collections;

import androidx.fragment.app.Fragment;

import com.amnesica.feedsta.helper.collections.EditBookmarksType;

public interface SelectOrAddCollectionCallback {
  void savePostOrListToCollection(String category, EditBookmarksType editMode, Fragment fragment);

  void openAddCollectionBtmSheet();
}
