package com.amnesica.feedsta.interfaces;

import com.amnesica.feedsta.Post;

import java.util.List;

/**
 * Interface used by SingleCollectionsFragment to communicate with adapter
 */
public interface OnItemClickListSingleColl {
    // simple click on bookmark to go to PostFragment
    void onItemClick(int position);

    // removes all selected bookmarks from storage
    void removeBookmarkedPostsFromStorage(List<Post> selectedPosts);

    // move all selected bookmarks to other collection
    void moveBookmarksToOtherCollection(List<Post> selectedPosts);

    // download all selected bookmarks
    void downloadSelectedBookmarks(List<Post> selectedPosts);

    // reset category of all selected bookmarks
    void resetCategoryOnSelectedBookmarks(List<Post> selectedPosts);
}
