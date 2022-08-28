package com.amnesica.feedsta.asynctasks.bookmarks;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageButton;

import androidx.fragment.app.Fragment;

import com.amnesica.feedsta.R;
import com.amnesica.feedsta.helper.CollectionsHelper;
import com.amnesica.feedsta.helper.EditBookmarksType;
import com.amnesica.feedsta.helper.SnackbarAlertType;
import com.amnesica.feedsta.helper.SnackbarHelper;
import com.amnesica.feedsta.helper.StorageHelper;
import com.amnesica.feedsta.interfaces.SelectOrAddCollectionCallback;
import com.amnesica.feedsta.models.Post;
import com.amnesica.feedsta.views.BtmSheetDialogAddCollection;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BookmarkPost implements SelectOrAddCollectionCallback {
  private final Post post;
  private final Fragment callingFragment;

  private static boolean bookmarked;
  private static boolean downloadError;

  private final ImageButton bookmarkButton;

  public BookmarkPost(Post post, Fragment callingFragment, ImageButton bookmarkButton) {
    this.post = post;
    this.callingFragment = callingFragment;
    this.bookmarkButton = bookmarkButton;
  }

  public void bookmarkPost() {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Handler handler = new Handler(Looper.getMainLooper());

    executor.execute(
        () -> {
          // Background work here
          new BookmarkPostRunnable(callingFragment, post).run();

          // UI Thread work here
          handler.post(() -> showBookmarkResultAndOptionToSaveInCollection(callingFragment));
        });

    executor.shutdown();
  }

  private void showBookmarkResultAndOptionToSaveInCollection(final Fragment callingFragment) {
    SnackbarHelper snackbarHelper =
        new SnackbarHelper(callingFragment, SnackbarAlertType.SNACKBAR_ON_POST);

    // set button background resource
    if (bookmarked && !downloadError) {
      // saved
      bookmarkButton.setBackgroundResource(R.drawable.ic_bookmark_24dp);

      snackbarHelper.showSnackBarToSaveInCollection(post, this);
    } else {
      // not saved yet
      bookmarkButton.setBackgroundResource(R.drawable.ic_bookmark_border_24dp);

      if (downloadError) {
        // show snackBar that bookmark could not get saved because of download error when
        // downloading thumbnail
        snackbarHelper.showSnackBarWithSpecificText(
            callingFragment.getResources().getString(R.string.post_was_not_saved));
      } else {
        // show snackBar that bookmark was removed
        snackbarHelper.showSnackBarWithSpecificText(
            callingFragment.getResources().getString(R.string.post_removed_from_bookmarks));
      }
    }
  }

  @Override
  public void savePostOrListToCollection(
      String category, EditBookmarksType editMode, Fragment fragment) {
    boolean successful = CollectionsHelper.setCategoryToPost(post, category, fragment);
    if (successful) {
      SnackbarHelper snackbarHelper =
          new SnackbarHelper(fragment, SnackbarAlertType.SNACKBAR_ON_POST);
      snackbarHelper.showSnackBarWithSpecificText(
          fragment.getString(R.string.saved_in_collection_successful) + post.getCategory());
    }
  }

  @Override
  public void openAddCollectionBtmSheet() {
    BtmSheetDialogAddCollection.show(
        post.getCategory(), callingFragment, EditBookmarksType.SAVE_BOOKMARKS, BookmarkPost.this);
  }

  public static class BookmarkPostRunnable implements Runnable {
    private final WeakReference<Fragment> fragmentReference;
    private final Post post;

    public BookmarkPostRunnable(Fragment fragment, Post post) {
      this.fragmentReference = new WeakReference<>(fragment);
      this.post = post;
    }

    @Override
    public void run() {
      bookmarkPost();
    }

    private void bookmarkPost() {
      try {
        final Fragment fragment = fragmentReference.get();
        if (fragment == null || post == null) return;

        if (!StorageHelper.checkIfAccountOrPostIsInFile(
            post, StorageHelper.FILENAME_BOOKMARKS, fragment.requireContext())) {

          bookmarked =
              StorageHelper.storePostInInternalStorage(
                  post, fragment.requireContext(), StorageHelper.FILENAME_BOOKMARKS);

        } else {
          // post is already bookmarked and needs to be deleted
          boolean bookmarkRemoved =
              StorageHelper.removePostFromInternalStorage(
                  post, fragment.requireContext(), StorageHelper.FILENAME_BOOKMARKS);

          if (bookmarkRemoved) {
            bookmarked = false;
          }
        }
      } catch (Exception e) {
        Log.d("BookmarkPostRunnable", Log.getStackTraceString(e));

        // handle failed bookmark with note to user
        downloadError = true;
      }
    }
  }
}
