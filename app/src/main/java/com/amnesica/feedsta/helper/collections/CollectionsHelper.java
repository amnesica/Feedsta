package com.amnesica.feedsta.helper.collections;

import android.util.Log;

import androidx.fragment.app.Fragment;

import com.amnesica.feedsta.helper.Error;
import com.amnesica.feedsta.helper.FragmentHelper;
import com.amnesica.feedsta.helper.StorageHelper;
import com.amnesica.feedsta.models.Post;

import java.io.IOException;

public class CollectionsHelper {

  public static boolean setCategoryToPost(Post post, String category, Fragment fragment) {
    // hint: no async task here -> no long running operation
    if (post == null) return false;
    post.setCategory(category);

    try {
      return StorageHelper.updateBookmarkCategoryInStorage(post, fragment.requireContext());
    } catch (IOException e) {
      Log.d("FragmentHelper", Log.getStackTraceString(e));
      FragmentHelper.notifyUserOfProblem(fragment, Error.UPDATING_BOOKMARKS_CATEGORY_FAILED);
      return false;
    }
  }
}
