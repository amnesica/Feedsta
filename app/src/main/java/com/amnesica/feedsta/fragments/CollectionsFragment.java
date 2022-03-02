package com.amnesica.feedsta.fragments;

import static com.amnesica.feedsta.helper.StaticIdentifier.permsRequestCode;
import static com.amnesica.feedsta.helper.StaticIdentifier.permsWriteOnly;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.amnesica.feedsta.Post;
import com.amnesica.feedsta.R;
import com.amnesica.feedsta.adapter.RecViewAdapterCollections;
import com.amnesica.feedsta.asynctasks.BatchDownloadPosts;
import com.amnesica.feedsta.asynctasks.SetCategoryToListOfPosts;
import com.amnesica.feedsta.helper.EditBookmarksType;
import com.amnesica.feedsta.helper.Error;
import com.amnesica.feedsta.helper.FragmentHelper;
import com.amnesica.feedsta.helper.NetworkHandler;
import com.amnesica.feedsta.helper.StorageHelper;
import com.amnesica.feedsta.interfaces.FragmentCallback;
import com.amnesica.feedsta.interfaces.FragmentRefreshCallback;
import com.amnesica.feedsta.interfaces.OnItemClickListenerColl;
import com.amnesica.feedsta.models.Collection;
import com.amnesica.feedsta.views.BtmSheetDialogAddCollection;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fragment for displaying all collections of all bookmarked posts
 */
public class CollectionsFragment extends Fragment implements FragmentCallback, FragmentRefreshCallback {

    // view stuff
    private RecyclerView recyclerView;
    private RecViewAdapterCollections adapter;
    private TextView textNoBookmarks;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressDialog progressDialogBatch;

    // download posts
    List<Post> postsToDownload;
    List<Post> postsToRenameCollection;

    // list with collections
    private List<Collection> listCollectionsBookmarked;

    // update thumbnail urls
    // with new list of bookmarked posts with updated thumbnailUrls
    private ArrayList<Post> listPostsBookmarked;
    private ArrayList<Post> listPostFailedRefresh;
    private ArrayList<Post> listPostsUpdatedBookmarked;
    private boolean somethingWentWrong = false;

    public CollectionsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_collections, container, false);

        textNoBookmarks = v.findViewById(R.id.textNoBookmarksCollections);
        swipeRefreshLayout = v.findViewById(R.id.swipeRefreshBookmarks);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateBookmarkThumbnails();
            }
        });

        recyclerView = v.findViewById(R.id.recycler_view_collections);
        // use this setting to
        // improve performance if you know that changes
        // in content do not change the layout size
        // of the RecyclerView
        // recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(requireContext(), 3);
        recyclerView.setLayoutManager(layoutManager);

        // create collections in listCollectionsBookmarked
        listCollectionsBookmarked = FragmentHelper.createCollectionsFromBookmarks(requireContext());

        // display no bookmarks textView
        showTextNoBookmarksIfNoPosts();

        //  define an adapter
        adapter = new RecViewAdapterCollections(listCollectionsBookmarked);
        recyclerView.setAdapter(adapter);

        // set onClickListener to open collection in new fragment
        adapter.setOnItemClickListener(new OnItemClickListenerColl() {
            @Override
            public void onItemClick(int position) {
                openSpecificCollection(position);
            }

            @Override
            public void removeBookmarksOfCollectionFromStorage(ArrayList<Collection> listCollectionsToRemove) {
                removeBookmarksOfCollection(listCollectionsToRemove);
            }

            @Override
            public void renameCategory(List<Collection> listCollectionsToRename) {
                renameCategoryInBookmarksOfCollection(listCollectionsToRename);
            }

            @Override
            public void downloadSelectedCollections(ArrayList<Collection> selectedCollections) {
                downloadSelectedBookmarksInCollections(selectedCollections);
            }

            @Override
            public void resetCollectionInAllSelectedBookmarksOfCategory(List<Collection> listCollectionsToReset) {
                resetCollectionInSelectedBookmarks(listCollectionsToReset);
            }
        });

        // setup toolbar with title
        Toolbar toolbar = v.findViewById(R.id.toolbar);
        toolbar.setTitle(getResources().getString(R.string.toolbar_title_bookmarks));

        return v;
    }

    /**
     * Resets the category of all bookmarks in collections in listCollectionsToReset to "null"
     *
     * @param listCollectionsToReset List<Collection>
     */
    private void resetCollectionInSelectedBookmarks(List<Collection> listCollectionsToReset) {
        // use set to remove duplicates
        Set<Post> setPostsToResetCategory = new HashSet<>();

        // get all posts in collections
        for (Collection collection : listCollectionsToReset) {
            String nameOfCollection = collection.getName();
            List<Post> postsInCollection = FragmentHelper.getAllBookmarkedPostsOfCollection(nameOfCollection, requireContext());
            setPostsToResetCategory.addAll(postsInCollection);
        }

        // call async task
        SetCategoryToListOfPosts setCategoryToListOfPosts =
                new SetCategoryToListOfPosts(CollectionsFragment.this, new ArrayList<>(setPostsToResetCategory),
                        null, progressDialogBatch, EditBookmarksType.REMOVE_FROM_MULTIPLE_COLLECTIONS);
        setCategoryToListOfPosts.setOnFragmentRefreshCallback(CollectionsFragment.this);
        setCategoryToListOfPosts.execute();
    }

    private void resetSomethingsWrong() {
        somethingWentWrong = false;
    }

    /**
     * Gets all bookmarks from storage and calls method showDialogAndStartFetching to
     * show dialog and start fetching new thumbnails of bookmarks
     */
    private void updateBookmarkThumbnails() {
        // get all bookmarks from storage in list
        listPostsBookmarked = StorageHelper.readPostsFromInternalStorage(requireContext(), StorageHelper.filename_bookmarks);

        // show dialog that it might take long
        if (listPostsBookmarked != null && !listPostsBookmarked.isEmpty()) {
            showDialogAndStartFetching();
        } else {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    /**
     * Shows a dialog to confirm the refreshing of thumbnails -> starts refreshing of bookmarks
     */
    private void showDialogAndStartFetching() {
        AlertDialog.Builder alertDialogBuilder;
        // create alertDialog
        alertDialogBuilder = new AlertDialog.Builder(requireContext())
                .setTitle(getResources().getString(R.string.title_dialog_refresh_bookmarks))
                .setMessage(getResources().getString(R.string.message_dialog_refresh_bookmarks))
                .setPositiveButton(getResources().getString(R.string.button_continue), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //  Continue with refresh operation
                        new CheckConnectionAndUpdateBookmarks(CollectionsFragment.this).execute();
                    }
                })
                .setNegativeButton(getResources().getString(R.string.CANCEL), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // stop refreshing
                        swipeRefreshLayout.setRefreshing(false);
                    }
                })

                // get the click outside the dialog to set the behaviour like the negative button was clicked
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        // stop refreshing
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });

        final AlertDialog.Builder finalAlertDialogBuilder = alertDialogBuilder;

        // get color for button texts
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = requireContext().getTheme();
        theme.resolveAttribute(R.attr.colorAccent, typedValue, true);
        @ColorInt final int color = typedValue.data;

        // create alertDialog
        requireActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog alertDialog = finalAlertDialogBuilder.create();
                alertDialog.setCanceledOnTouchOutside(true);
                alertDialog.show();
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color);
                alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(color);
            }
        });
    }

    /**
     * Downloads all bookmarks in selected collections
     *
     * @param selectedCollections list with collections
     */
    private void downloadSelectedBookmarksInCollections(ArrayList<Collection> selectedCollections) {
        if (selectedCollections != null && !selectedCollections.isEmpty()) {
            // hint: use set here to avoid duplicates
            Set<Post> setPostsToDownload = new HashSet<>();

            // get all bookmarks from selected collections
            for (Collection collection : selectedCollections) {
                List<Post> postsInCollection = FragmentHelper.getAllBookmarkedPostsOfCollection(collection.getName(), requireContext());
                if (postsInCollection != null) {
                    setPostsToDownload.addAll(postsInCollection);
                }
            }

            // add all bookmarks from set to list to download
            postsToDownload = new ArrayList<>(setPostsToDownload);

            // check permissions and start batch download
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(permsWriteOnly, permsRequestCode);
            }
        }
    }

    /**
     * Renames the collection in all bookmarks of the collection.
     * Hint: The collection "All" is not contained anymore at this point
     *
     * @param listCollectionsToRename list with collections
     */
    private void renameCategoryInBookmarksOfCollection(List<Collection> listCollectionsToRename) {
        String nameOfCollection = listCollectionsToRename.get(0).getName();
        postsToRenameCollection = FragmentHelper.getAllBookmarkedPostsOfCollection(nameOfCollection, requireContext());

        if (postsToRenameCollection != null && !postsToRenameCollection.isEmpty()) {
            // open new BtmSheetDialogAddCollection
            BtmSheetDialogAddCollection bottomSheetAddCollection = new BtmSheetDialogAddCollection(nameOfCollection, EditBookmarksType.RENAME_COLLECTION);

            // set callback listener to refresh view afterwards
            bottomSheetAddCollection.setOnFragmentCallbackListener(CollectionsFragment.this);

            // show dialog
            bottomSheetAddCollection.show(requireActivity().getSupportFragmentManager(), BtmSheetDialogAddCollection.class.getSimpleName());
        }
    }

    /**
     * Removes all bookmarks of selected collections
     *
     * @param listCollectionsToRemove list of collection
     */
    private void removeBookmarksOfCollection(ArrayList<Collection> listCollectionsToRemove) {
        RemoveBookmarksOfCollection removeBookmarksOfCollection =
                new RemoveBookmarksOfCollection(CollectionsFragment.this, listCollectionsToRemove);
        removeBookmarksOfCollection.execute();
    }

    /**
     * Opens a specific collection. It searches for all bookmarks which are in that category and
     * opens a new SingleCollectionFragment
     *
     * @param position position in listCollectionsBookmarked
     */
    private void openSpecificCollection(int position) {
        listCollectionsBookmarked = FragmentHelper.createCollectionsFromBookmarks(requireContext());
        String category = listCollectionsBookmarked.get(position).getName();

        // find all posts of specific collection
        List<Post> listPostsInCollectionBookmarked = FragmentHelper.getAllBookmarkedPostsOfCollection(category, requireContext());

        if (listPostsInCollectionBookmarked != null) {
            // new SingleCollectionFragment
            SingleCollectionFragment singleCollectionFragment = SingleCollectionFragment.newInstance(listPostsInCollectionBookmarked, category);

            // add fragment to container
            FragmentHelper.addFragmentToContainer(singleCollectionFragment, requireActivity().getSupportFragmentManager());
        }
    }

    /**
     * Displays a text that there are no bookmarks (no saved posts) to show
     */
    private void showTextNoBookmarksIfNoPosts() {
        if (listCollectionsBookmarked == null || listCollectionsBookmarked.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            textNoBookmarks.setVisibility(View.VISIBLE);
            textNoBookmarks.setText(R.string.no_bookmarked_posts);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            textNoBookmarks.setVisibility(View.GONE);
        }
    }

    /**
     * Refreshes adapter to display changes to data in view
     */
    private void refreshAdapter() {
        if (adapter != null) {
            listCollectionsBookmarked = FragmentHelper.createCollectionsFromBookmarks(requireContext());
            adapter.setItems(listCollectionsBookmarked);
            adapter.notifyDataSetChanged();

            showTextNoBookmarksIfNoPosts();
        }
    }

    @Override
    public void refreshAdapterCallback() {
        refreshAdapter();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            // updates the bookmarks from storages and creates new categories
            refreshAdapter();

            // set highlighted item on nav bar to "bookmarks"
            FragmentHelper.setBottomNavViewSelectElem(getActivity(), R.id.navigation_collections);
        } else {
            // close contextual menu
            if (adapter != null && RecViewAdapterCollections.actionMode != null) {
                RecViewAdapterCollections.actionMode.finish();
            }
        }
    }

    @Override
    // rename collection to String category
    public void savePostOrListToCollection(String category, EditBookmarksType editMode) {
        SetCategoryToListOfPosts setCategoryToListOfPosts = new SetCategoryToListOfPosts(CollectionsFragment.this, postsToRenameCollection,
                category, progressDialogBatch, editMode);
        setCategoryToListOfPosts.setOnFragmentRefreshCallback(CollectionsFragment.this);
        setCategoryToListOfPosts.execute();
    }

    @Override
    public void openBtmSheetDialogAddCollection(EditBookmarksType editMode) {
        // do nothing here
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (permsRequestCode == 200) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted. Continue the action or workflow in your app.
                // start download of all bookmarks in selected collections
                if (postsToDownload != null && !postsToDownload.isEmpty()) {
                    new BatchDownloadPosts(CollectionsFragment.this, postsToDownload, progressDialogBatch).execute();
                }
            } else {
                // Explain to the user that the feature is unavailable because
                // the features requires a permission that the user has denied.
                // At the same time, respect the user's decision. Don't link to
                // system settings in an effort to convince the user to change
                // their decision.
                FragmentHelper.showToast(getResources().getString(R.string.permission_denied), requireActivity(), requireContext());
            }
        }
    }

    /**
     * Shows the dialog after refreshing thumbnails of bookmarks to
     * remove bookmarks that could not be refreshed or to try again later
     */
    private void showConfirmationDialogAndRemoveBookmarksRefresh() {
        try {
            AlertDialog.Builder alertDialogBuilder;
            // create alertDialog
            alertDialogBuilder = new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.dialog_refresh_bookmarks_problems_title)
                    .setMessage(listPostFailedRefresh.size() + "/" + listPostsBookmarked.size() + " " + getString(R.string.dialog_refresh_bookmarks_problems_message))
                    .setPositiveButton(R.string.dialog_refresh_bookmarks_problems_positive_button, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // override existing bookmarks (store posts) update listView
                            new StoreUpdatedBookmarkedPostsInStorage(CollectionsFragment.this).execute();
                        }
                    })
                    .setNegativeButton(R.string.dialog_refresh_bookmarks_problems_negative_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (listPostFailedRefresh != null) {
                                listPostFailedRefresh.clear();
                                listPostFailedRefresh = null;
                            }
                            resetSomethingsWrong();
                        }
                    })
                    // get the click outside the dialog to set the behaviour like the negative button was clicked
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            if (listPostFailedRefresh != null) {
                                listPostFailedRefresh.clear();
                                listPostFailedRefresh = null;
                            }
                            resetSomethingsWrong();
                        }
                    });

            final AlertDialog.Builder finalAlertDialogBuilder = alertDialogBuilder;

            // get color for button texts
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = requireContext().getTheme();
            theme.resolveAttribute(R.attr.colorAccent, typedValue, true);
            @ColorInt final int color = typedValue.data;

            // create alertDialog
            requireActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog alertDialog = finalAlertDialogBuilder.create();
                    alertDialog.setCanceledOnTouchOutside(true);
                    alertDialog.show();
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color);
                    alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(color);
                }
            });
        } catch (Exception e) {
            Log.d("CollectionsFragment", Log.getStackTraceString(e));
        }
    }

    /**
     * Checks internet connection and notifies user if there is no connection.
     * Updates bookmarks at the end
     */
    @SuppressWarnings("CanBeFinal")
    private static class CheckConnectionAndUpdateBookmarks extends AsyncTask<Void, Void, Void> {

        private final WeakReference<CollectionsFragment> fragmentReference;
        boolean isInternetAvailable = false;

        // constructor
        CheckConnectionAndUpdateBookmarks(CollectionsFragment context) {
            fragmentReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if (!isCancelled()) {
                isInternetAvailable = NetworkHandler.isInternetAvailable();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (!isCancelled()) {
                // get reference from fragment
                final CollectionsFragment fragment = fragmentReference.get();
                if (fragment != null) {
                    if (isInternetAvailable) {
                        // call update method
                        if (fragment.listPostsBookmarked != null &&
                                !fragment.listPostsBookmarked.isEmpty()) {
                            new UpdateThumbnailURL(fragment).execute();
                        } else {
                            fragment.swipeRefreshLayout.setRefreshing(false);
                        }
                    } else {
                        FragmentHelper.notifyUserOfProblem(fragment, Error.NO_INTERNET_CONNECTION);
                        fragment.swipeRefreshLayout.setRefreshing(false);
                    }
                }
            }
        }
    }

    /**
     * Updates the thumbnail url of bookmarked posts because they change over time.
     * Hint/Explanation: With the new approach of storing the thumbnail image as a string,
     * this functionality is deprecated and is not necessary for future installations.
     * However, this functionality is needed for existing installations and existing
     * bookmarks (legacy functionality)
     */
    @Deprecated
    private static class UpdateThumbnailURL extends AsyncTask<Void, Integer, Void> {
        private final WeakReference<CollectionsFragment> fragmentReference;
        NetworkHandler sh;

        private ProgressDialog progressDialogBatch;
        private int editedItems = 0;

        // constructor
        UpdateThumbnailURL(CollectionsFragment context) {
            fragmentReference = new WeakReference<>(context);
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
            if (!isCancelled()) {
                // get reference from fragment
                final CollectionsFragment fragment = fragmentReference.get();
                if (fragment != null) {
                    sh = new NetworkHandler();

                    // set initial max size to zero
                    progressDialogBatch.setMax(0);

                    // set max size of progressDialog
                    int progressMaxSize = fragment.listPostsBookmarked.size();

                    // set length of progressDialog
                    progressDialogBatch.setMax(progressMaxSize);

                    // initialize list for failed posts
                    fragment.listPostFailedRefresh = new ArrayList<>();

                    for (Post bookmarkedPost : fragment.listPostsBookmarked) {
                        if (bookmarkedPost != null && bookmarkedPost.getShortcode() != null) {
                            String url = "https://www.instagram.com/p/" + bookmarkedPost.getShortcode() + "/?__a=1";

                            // get new thumbnail url
                            try {
                                String newThumbnailUrl = getNewThumbnailUrl(url);
                                if (newThumbnailUrl != null) {
                                    if (fragment.listPostsUpdatedBookmarked == null) {
                                        fragment.listPostsUpdatedBookmarked = new ArrayList<>();
                                    }

                                    // get new thumbnail as string with new url
                                    String newImageThumbnail = FragmentHelper.getBase64EncodedImage(newThumbnailUrl);

                                    // copy old bookmark and insert new one in listPostsUpdatedBookmarked
                                    fragment.listPostsUpdatedBookmarked.add(
                                            new Post(bookmarkedPost.getId(),
                                                    bookmarkedPost.getShortcode(),
                                                    bookmarkedPost.getTakenAtDate(),
                                                    bookmarkedPost.getIs_video(),
                                                    newThumbnailUrl,
                                                    bookmarkedPost.getIs_sideCar(),
                                                    bookmarkedPost.getCategory(),
                                                    newImageThumbnail));

                                    // publish progress -> not real progress here -> Saving is missing here
                                    publishProgress(editedItems += 1);

                                } else {
                                    fragment.somethingWentWrong = true;

                                    // add failed post to list
                                    fragment.listPostFailedRefresh.add(bookmarkedPost);
                                }
                            } catch (Exception e) {
                                fragment.somethingWentWrong = true;

                                // add failed post to list (error when trying to download new thumbnail as string)
                                fragment.listPostFailedRefresh.add(bookmarkedPost);

                                Log.d("CollectionsFragment", Log.getStackTraceString(e));
                            }
                        }
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
            if (!isCancelled()) {
                // get reference from fragment
                final CollectionsFragment fragment = fragmentReference.get();
                if (fragment != null) {

                    // dismiss progress dialog
                    progressDialogBatch.dismiss();

                    // if there was no error store updated bookmarks in memory
                    if (!fragment.somethingWentWrong) {

                        // store updated bookmarkedPosts in memory
                        new StoreUpdatedBookmarkedPostsInStorage(fragment).execute();
                    } else {

                        // notify user
                        if (fragment.listPostFailedRefresh != null && !fragment.listPostFailedRefresh.isEmpty()) {
                            // show dialog with amount of failed posts and ask what to do
                            fragment.showConfirmationDialogAndRemoveBookmarksRefresh();
                        } else if (!NetworkHandler.isInternetAvailable()) {
                            // no internet
                            FragmentHelper.notifyUserOfProblem(fragment, Error.NO_INTERNET_CONNECTION);
                        } else {
                            // something else
                            FragmentHelper.notifyUserOfProblem(fragment, Error.SOMETHINGS_WRONG);
                        }

                        fragment.swipeRefreshLayout.setRefreshing(false);

                        fragment.resetSomethingsWrong();
                    }
                }
            }
        }

        /**
         * Get new url for thumbnail of bookmarked post
         *
         * @param url String
         * @return String
         * @throws JSONException JSONException
         */
        private String getNewThumbnailUrl(String url) throws JSONException {
            String newThumbnailUrl = null;
            if (!isCancelled()) {
                // get reference from fragment
                final CollectionsFragment fragment = fragmentReference.get();
                if (fragment != null) {

                    // get json string from url
                    String jsonStr = sh.makeServiceCall(url, this.getClass().getSimpleName());

                    if (jsonStr != null) {
                        // something went wrong -> possible rate limit reached
                        if (!FragmentHelper.checkIfJsonStrIsValid(jsonStr, fragment)) {
                            return null;
                        }
                        // file overall as json object
                        JSONObject jsonObj = new JSONObject(jsonStr);
                        JSONObject graphql = jsonObj.getJSONObject("graphql");
                        JSONObject shortcode_media = graphql.getJSONObject("shortcode_media");
                        // get new new thumbnail url
                        newThumbnailUrl = shortcode_media.getJSONArray("display_resources").getJSONObject(0).getString("src");
                        jsonObj = null;
                    }
                }
            }
            return newThumbnailUrl;
        }

        private void showProgressDialog() {
            if (!isCancelled()) {
                if (!isCancelled()) {
                    // get reference from fragment
                    final CollectionsFragment fragment = fragmentReference.get();

                    if (fragment != null) {
                        progressDialogBatch = new ProgressDialog(fragment.requireContext());
                        progressDialogBatch.setTitle(fragment.requireContext().getString(R.string.title_dialog_refresh_bookmarks));
                        progressDialogBatch.setMessage(fragment.requireContext().getString(R.string.message_dialog_refresh_bookmarks));
                        progressDialogBatch.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                        progressDialogBatch.setProgress(0);
                        progressDialogBatch.show();
                    }
                }
            }
        }
    }

    /**
     * Stores updated bookmarkedPosts in internal storage
     */
    private static class StoreUpdatedBookmarkedPostsInStorage extends AsyncTask<Void, Void, Void> {

        private final WeakReference<CollectionsFragment> fragmentReference;
        private boolean storingSuccessful = false;
        private boolean renamingFilesSuccessful = false;
        private boolean removingSuccessful = false;

        // constructor
        StoreUpdatedBookmarkedPostsInStorage(CollectionsFragment context) {
            fragmentReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if (!isCancelled()) {
                // get reference from fragment
                final CollectionsFragment fragment = fragmentReference.get();
                if (fragment != null && fragment.listPostsUpdatedBookmarked != null &&
                        fragment.getContext() != null) {
                    try {
                        // store posts in storage in proper storage representation in bookmarkedPosts_updated
                        storingSuccessful = StorageHelper.storePostListInInternalStorage(fragment.listPostsUpdatedBookmarked, fragment.requireContext(), StorageHelper.filename_bookmarks_updated);
                    } catch (Exception e) {
                        storingSuccessful = false;
                        Log.d("CollectionsFragment", Log.getStackTraceString(e));
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (!isCancelled()) {
                // get reference from fragment
                final CollectionsFragment fragment = fragmentReference.get();
                if (fragment != null) {
                    // check if storing filename_posts_updated was successful and if the file exists
                    //  and rename filename_posts_updated to old filename_bookmarks
                    if (storingSuccessful && fragment.getContext() != null &&
                            StorageHelper.checkIfFileExists(StorageHelper.filename_bookmarks_updated, fragment.requireContext())) { // old: fragment.listPostsUpdatedBookmarked.size() == fragment.listPostsBookmarked.size

                        try {
                            StorageHelper.renameSpecificFileTo(fragment.requireContext(), StorageHelper.filename_bookmarks, StorageHelper.filename_bookmarks_updated);
                            renamingFilesSuccessful = true;
                        } catch (NullPointerException e) {
                            renamingFilesSuccessful = false;
                            Log.d("CollectionsFragment", Log.getStackTraceString(e));
                        }
                    }// if fetching of all bookmarks failed -> remove them all
                    else if (fragment.listPostsUpdatedBookmarked == null && fragment.getContext() != null &&
                            fragment.listPostFailedRefresh != null && !fragment.listPostFailedRefresh.isEmpty()) {

                        for (Post post : fragment.listPostFailedRefresh) {
                            removingSuccessful = StorageHelper.removePostFromInternalStorage(post, fragment.getContext(), StorageHelper.filename_bookmarks);
                            if (!removingSuccessful) {
                                return;
                            }
                        }
                    }

                    // reset listPostsUpdatedBookmarked and listPostFailedRefresh for next iteration
                    // hint: only after 'else if'
                    if (fragment.listPostsUpdatedBookmarked != null) {
                        fragment.listPostsUpdatedBookmarked.clear();
                        fragment.listPostsUpdatedBookmarked = null;
                    }
                    if (fragment.listPostFailedRefresh != null) {
                        fragment.listPostFailedRefresh.clear();
                        fragment.listPostFailedRefresh = null;
                    }

                    // show toast to display posts updated successful or failed
                    if (fragment.getActivity() != null && storingSuccessful && renamingFilesSuccessful) {
                        FragmentHelper.showToast(fragment.getResources().getString(R.string.posts_updated), fragment.requireActivity(), fragment.requireContext());
                    } else if (removingSuccessful) {
                        FragmentHelper.showToast(fragment.getResources().getString(R.string.posts_removed_successful), fragment.requireActivity(), fragment.requireContext());
                    } else {
                        FragmentHelper.showToast(fragment.getResources().getString(R.string.posts_updated_failed), fragment.requireActivity(), fragment.requireContext());
                    }

                    // set refreshing false
                    fragment.swipeRefreshLayout.setRefreshing(false);

                    fragment.refreshAdapter();

                    fragment.resetSomethingsWrong();
                }
            }
        }
    }

    /**
     * Async task to remove all bookmarks of selected collections
     */
    private static class RemoveBookmarksOfCollection extends AsyncTask<Void, Integer, Void> {

        private final WeakReference<CollectionsFragment> fragmentReference;

        private final ArrayList<Collection> listCollectionsToRemove;
        private ProgressDialog progressDialogBatch;
        private int editedItems = 0;
        private boolean removingSuccessful = false;

        // constructor
        RemoveBookmarksOfCollection(CollectionsFragment context, ArrayList<Collection> listCollectionsToRemove) {
            fragmentReference = new WeakReference<>(context);
            this.listCollectionsToRemove = listCollectionsToRemove;
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
            if (!isCancelled()) {
                // get reference from fragment
                final CollectionsFragment fragment = fragmentReference.get();
                if (fragment != null) {
                    // hint: use set here to avoid duplicates
                    Set<Post> postsToRemove = new HashSet<>();

                    // get all bookmarks in collections that should be removed
                    for (Collection collection : listCollectionsToRemove) {
                        postsToRemove.addAll(FragmentHelper.getAllBookmarkedPostsOfCollection(collection.getName(), fragment.requireContext()));
                    }

                    // set initial max size to zero
                    progressDialogBatch.setMax(0);

                    // set max size of progressDialog
                    int progressMaxSize = postsToRemove.size();

                    // set length of progressDialog
                    progressDialogBatch.setMax(progressMaxSize);

                    // remove all bookmarks in set
                    if (postsToRemove != null && !postsToRemove.isEmpty()) {
                        for (Post post : postsToRemove) {
                            removingSuccessful = StorageHelper.removePostFromInternalStorage(post, fragment.requireContext(), StorageHelper.filename_bookmarks);

                            if (!removingSuccessful) {
                                return null;
                            }

                            publishProgress(editedItems += 1);
                        }
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (!isCancelled()) {

                // dismiss progress dialog
                progressDialogBatch.dismiss();

                // get reference from fragment
                final CollectionsFragment fragment = fragmentReference.get();
                if (fragment != null) {
                    if (removingSuccessful) {
                        FragmentHelper.showToast(fragment.requireContext().getString(R.string.selected_collections_removed_success), fragment.requireActivity(), fragment.requireContext());

                        // refresh bookmarked posts
                        fragment.refreshAdapter();
                    } else {
                        FragmentHelper.showToast(fragment.requireContext().getString(R.string.selected_collections_removed_fail), fragment.requireActivity(), fragment.requireContext());
                    }
                }
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            progressDialogBatch.setProgress(values[0]);
        }

        private void showProgressDialog() {
            if (!isCancelled()) {
                if (!isCancelled()) {
                    // get reference from fragment
                    final CollectionsFragment fragment = fragmentReference.get();

                    if (fragment != null) {
                        progressDialogBatch = new ProgressDialog(fragment.requireContext());
                        progressDialogBatch.setTitle(fragment.requireContext().getString(R.string.dialog_title_removing_collection));
                        progressDialogBatch.setMessage(fragment.requireContext().getString(R.string.dialog_message_removing_collection));
                        progressDialogBatch.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                        progressDialogBatch.setProgress(0);
                        progressDialogBatch.show();
                    }
                }
            }
        }
    }
}