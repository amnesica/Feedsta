package com.amnesica.feedsta.interfaces;

import androidx.fragment.app.Fragment;

import com.amnesica.feedsta.helper.EditBookmarksType;

public interface SelectOrAddCollectionCallback {
  void savePostOrListToCollection(String category, EditBookmarksType editMode, Fragment fragment);

  void openAddCollectionBtmSheet();
}
