package com.amnesica.feedsta.asynctasks;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.amnesica.feedsta.Post;
import com.amnesica.feedsta.R;
import com.amnesica.feedsta.helper.EditBookmarksType;
import com.amnesica.feedsta.helper.FragmentHelper;
import com.amnesica.feedsta.interfaces.FragmentRefreshCallback;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Class to set a new category to a single or multiple bookmarks with ProgressDialog
 */
public class SetCategoryToListOfPosts extends AsyncTask<Void, Integer, Void> {

    private final List<Post> listOfPostsToEditCategory;
    private final String category;
    private final EditBookmarksType editMode;
    WeakReference<Fragment> fragmentWeakReference;
    private ProgressDialog progressDialogBatch;
    private FragmentRefreshCallback callback;

    private int editedItems = 0;
    private boolean successful = false;

    public SetCategoryToListOfPosts(Fragment fragment, List<Post> listOfPostsToEditCategory, String category,
                                    ProgressDialog progressDialogBatch, EditBookmarksType editMode) {
        this.listOfPostsToEditCategory = listOfPostsToEditCategory;
        this.progressDialogBatch = progressDialogBatch;
        this.category = category;
        this.editMode = editMode;
        this.fragmentWeakReference = new WeakReference<>(fragment);
    }

    public void setOnFragmentRefreshCallback(FragmentRefreshCallback callback) {
        this.callback = callback;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        if (!isCancelled()) {
            showProgressDialog();
        }
    }

    @Override
    protected Void doInBackground(Void... voids) {
        if (!isCancelled() && fragmentWeakReference.get() != null && listOfPostsToEditCategory != null) {
            Fragment fragment = fragmentWeakReference.get();

            // set initial max size to zero
            progressDialogBatch.setMax(0);

            // set max size of progressDialog
            int progressMaxSize = listOfPostsToEditCategory.size();

            // set length of progressDialog
            progressDialogBatch.setMax(progressMaxSize);

            for (Post bookmark : listOfPostsToEditCategory) {
                // set new category to single post (null as parameter because there is no fragment
                //  -> toasts are handled in this class!)
                successful = FragmentHelper.setNewCategoryToPost(category, bookmark, fragment.requireContext(), null);
                if (!successful) {
                    return null;
                } else {
                    publishProgress(editedItems += 1);
                }
            }
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        progressDialogBatch.setProgress(values[0]);
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);

        if (!isCancelled() && fragmentWeakReference.get() != null) {
            Fragment fragment = fragmentWeakReference.get();

            if (!successful) {
                if (editMode.equals(EditBookmarksType.MOVE_BOOKMARKS)) {
                    Toast.makeText(fragment.requireContext(), "Moving bookmarks to collection " + category + " failed", Toast.LENGTH_SHORT).show();
                }
                if (editMode.equals(EditBookmarksType.RENAME_COLLECTION)) {
                    Toast.makeText(fragment.requireContext(), "Renaming collection to " + category + " failed", Toast.LENGTH_SHORT).show();
                }
                if (editMode.equals(EditBookmarksType.REMOVE_FROM_THIS_COLLECTION)) {
                    Toast.makeText(fragment.requireContext(), "Removing bookmarks from this collection failed", Toast.LENGTH_SHORT).show();
                }
                if (editMode.equals(EditBookmarksType.REMOVE_FROM_MULTIPLE_COLLECTIONS)) {
                    Toast.makeText(fragment.requireContext(), "Removing bookmarks from selected collections failed", Toast.LENGTH_SHORT).show();
                }
            } else {
                if (editMode.equals(EditBookmarksType.MOVE_BOOKMARKS)) {
                    Toast.makeText(fragment.requireContext(), "Successfully moved bookmarks to collection " + category, Toast.LENGTH_SHORT).show();
                }
                if (editMode.equals(EditBookmarksType.RENAME_COLLECTION)) {
                    Toast.makeText(fragment.requireContext(), "Successfully renamed collection to " + category, Toast.LENGTH_SHORT).show();
                }
                if (editMode.equals(EditBookmarksType.REMOVE_FROM_THIS_COLLECTION)) {
                    Toast.makeText(fragment.requireContext(), "Successfully removed bookmarks from this collection", Toast.LENGTH_SHORT).show();
                }
                if (editMode.equals(EditBookmarksType.REMOVE_FROM_MULTIPLE_COLLECTIONS)) {
                    Toast.makeText(fragment.requireContext(), "Successfully removed bookmarks from selected collections", Toast.LENGTH_SHORT).show();
                }
            }

            if (callback != null) {
                callback.refreshAdapterCallback();
            }

            progressDialogBatch.dismiss();
        }
    }

    /**
     * Initializes and shows a progressDialog with proper message
     */
    private void showProgressDialog() {
        if (!isCancelled() && fragmentWeakReference.get() != null) {
            Fragment fragment = fragmentWeakReference.get();

            progressDialogBatch = new ProgressDialog(fragment.requireContext());

            if (editMode.equals(EditBookmarksType.MOVE_BOOKMARKS)) {
                progressDialogBatch.setTitle(fragment.requireContext().getString(R.string.progress_dialog_title_move_selected_posts));
                progressDialogBatch.setMessage(fragment.requireContext().getString(R.string.progress_dialog_message_move_selected_posts));
            }
            if (editMode.equals(EditBookmarksType.RENAME_COLLECTION)) {
                progressDialogBatch.setTitle(fragment.requireContext().getString(R.string.progress_dialog_title_rename_collection));
                progressDialogBatch.setMessage(fragment.requireContext().getString(R.string.progress_dialog_message_rename_collection));
            }
            if (editMode.equals(EditBookmarksType.REMOVE_FROM_THIS_COLLECTION)) {
                progressDialogBatch.setTitle(fragment.requireContext().getString(R.string.dialog_title_remove_from_this_coll));
                progressDialogBatch.setMessage(fragment.requireContext().getString(R.string.dialog_message_remove_from_this_coll));
            }
            if (editMode.equals(EditBookmarksType.REMOVE_FROM_MULTIPLE_COLLECTIONS)) {
                progressDialogBatch.setTitle(fragment.requireContext().getString(R.string.progress_dialog_title_remove_only_collection));
                progressDialogBatch.setMessage(fragment.requireContext().getString(R.string.progress_dialog_message_remove_only_collection));
            }

            progressDialogBatch.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialogBatch.setProgress(0);
            progressDialogBatch.show();
        }
    }
}
