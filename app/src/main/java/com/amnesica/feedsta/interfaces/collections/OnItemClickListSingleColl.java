package com.amnesica.feedsta.interfaces.collections;

import com.amnesica.feedsta.models.Post;

import java.util.List;

/** Interface used by SingleCollectionsFragment to communicate with adapter */
public interface OnItemClickListSingleColl {
  /**
   * Simple click on bookmark to go to PostFragment
   *
   * @param position int
   */
  void onItemClick(int position);

  /**
   * Removes all selected bookmarks from storage
   *
   * @param selectedPosts List<Post>
   */
  void removeBookmarkedPostsFromStorage(List<Post> selectedPosts);

  /**
   * Move all selected bookmarks to other collection
   *
   * @param selectedPosts List<Post
   */
  void moveBookmarksToOtherCollection(List<Post> selectedPosts);

  /**
   * Download all selected bookmarks
   *
   * @param selectedPosts List<Post>
   */
  void downloadSelectedBookmarks(List<Post> selectedPosts);

  /**
   * Reset category of all selected bookmarks
   *
   * @param selectedPosts List<Post>
   */
  void resetCategoryOnSelectedBookmarks(List<Post> selectedPosts);
}
