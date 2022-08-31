package com.amnesica.feedsta.helper.collections;

/** Enum to know the current edit mode when dealing with bookmarks */
public enum EditBookmarksType {
  RENAME_COLLECTION, // collection should be renamed
  MOVE_BOOKMARKS, // bookmarks should be moved to other collection
  SAVE_BOOKMARKS, // bookmarks should be saved
  REMOVE_FROM_THIS_COLLECTION, // bookmarks should be removed from this collection only
  REMOVE_FROM_MULTIPLE_COLLECTIONS, // bookmarks should be removed from multiple collections
}
