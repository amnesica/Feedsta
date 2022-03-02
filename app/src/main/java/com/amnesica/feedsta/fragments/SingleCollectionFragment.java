package com.amnesica.feedsta.fragments;

import static com.amnesica.feedsta.helper.StaticIdentifier.permsRequestCode;
import static com.amnesica.feedsta.helper.StaticIdentifier.permsWriteOnly;

import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amnesica.feedsta.R;
import com.amnesica.feedsta.adapter.RecViewAdapterSingleCollection;
import com.amnesica.feedsta.asynctasks.BatchDownloadPosts;
import com.amnesica.feedsta.asynctasks.SetCategoryToListOfPosts;
import com.amnesica.feedsta.helper.EditBookmarksType;
import com.amnesica.feedsta.helper.FragmentHelper;
import com.amnesica.feedsta.helper.StorageHelper;
import com.amnesica.feedsta.interfaces.FragmentCallback;
import com.amnesica.feedsta.interfaces.FragmentRefreshCallback;
import com.amnesica.feedsta.interfaces.OnItemClickListSingleColl;
import com.amnesica.feedsta.models.Post;
import com.amnesica.feedsta.views.BtmSheetDialogAddCollection;
import com.amnesica.feedsta.views.BtmSheetDialogSelectCollection;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for displaying bookmarked posts in a collection
 */
public class SingleCollectionFragment extends Fragment implements FragmentCallback, FragmentRefreshCallback {

    // view stuff
    private RecyclerView recyclerView;
    private RecViewAdapterSingleCollection adapter;
    private TextView textViewNoBookmarksInSingleCollection;
    private ProgressDialog progressDialogBatch;

    // list with bookmarks of collection
    private List<Post> listPostsInCollectionBookmarked;

    // name of collection
    private String category;

    // download, move or reset posts
    private List<Post> postsToDownload;
    private List<Post> postsToMove;
    private List<Post> postsToReset;

    public SingleCollectionFragment() {
        // Required empty public constructor
    }

    static SingleCollectionFragment newInstance(List<Post> listPostsInCollectionBookmarked, String category) {
        Bundle args = new Bundle();
        args.putSerializable("bookmarksInCollection", (Serializable) listPostsInCollectionBookmarked);
        args.putSerializable("category", category);
        SingleCollectionFragment fragment = new SingleCollectionFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_single_collection, container, false);

        textViewNoBookmarksInSingleCollection = v.findViewById(R.id.textNoBookmarksInSingleCollection);
        recyclerView = v.findViewById(R.id.recycler_view_single_collection);
        // use this setting to
        // improve performance if you know that changes
        // in content do not change the layout size
        // of the RecyclerView
        // recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(requireContext(), 3);
        recyclerView.setLayoutManager(layoutManager);

        //  Retrieve listPostsInCollectionBookmarked
        if (this.getArguments() != null) {
            if (getArguments().getSerializable("bookmarksInCollection") != null) {
                listPostsInCollectionBookmarked = (List<Post>) getArguments().getSerializable("bookmarksInCollection");
                category = (String) getArguments().getSerializable("category");
            }
        }

        // display no bookmarks textView
        showTextNoBookmarksIfNoPosts();

        // define an adapter
        adapter = new RecViewAdapterSingleCollection(listPostsInCollectionBookmarked);
        recyclerView.setAdapter(adapter);

        // set onClickListener to open collection in new fragment
        adapter.setOnItemClickListener(new OnItemClickListSingleColl() {
            @Override
            public void onItemClick(int position) {
                // go to bookmark post
                Post postToSend = listPostsInCollectionBookmarked.get(position);

                if (postToSend != null) {
                    // new postFragment
                    PostFragment postFragment = PostFragment.newInstance(postToSend);

                    // add fragment to container
                    FragmentHelper.addFragmentToContainer(postFragment, requireActivity().getSupportFragmentManager());
                }
            }

            @Override
            public void removeBookmarkedPostsFromStorage(List<Post> listPostsToRemove) {
                removeBookmarkedPostsInList(listPostsToRemove);
            }

            @Override
            public void moveBookmarksToOtherCollection(List<Post> selectedPosts) {
                if (selectedPosts != null && !selectedPosts.isEmpty()) {
                    postsToMove = new ArrayList<>();
                    postsToMove.addAll(selectedPosts);

                    if (FragmentHelper.collectionsAlreadyExist(requireContext())) {
                        // open dialog to select existing collection for bookmark
                        BtmSheetDialogSelectCollection btmSheetDialogSelectCollection = new BtmSheetDialogSelectCollection(EditBookmarksType.MOVE_BOOKMARKS);

                        // set listener to get selected category
                        btmSheetDialogSelectCollection.setOnFragmentCallbackListener(SingleCollectionFragment.this);

                        // show bottom sheet
                        btmSheetDialogSelectCollection.show(requireActivity().getSupportFragmentManager(), BtmSheetDialogSelectCollection.class.getSimpleName());

                    } else {
                        // open dialog to add bookmark to collection
                        BtmSheetDialogAddCollection bottomSheetAddCollection = new BtmSheetDialogAddCollection(null, EditBookmarksType.MOVE_BOOKMARKS);

                        // set listener to get selected category
                        bottomSheetAddCollection.setOnFragmentCallbackListener(SingleCollectionFragment.this);

                        // show bottom sheet
                        bottomSheetAddCollection.show(requireActivity().getSupportFragmentManager(), BtmSheetDialogAddCollection.class.getSimpleName());
                    }
                }
            }

            @Override
            public void downloadSelectedBookmarks(List<Post> selectedPosts) {
                if (selectedPosts != null && !selectedPosts.isEmpty()) {
                    postsToDownload = new ArrayList<>();
                    postsToDownload.addAll(selectedPosts);

                    // check permissions and start batch download
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(permsWriteOnly, permsRequestCode);
                    }
                }
            }

            @Override
            public void resetCategoryOnSelectedBookmarks(List<Post> selectedPosts) {
                if (selectedPosts != null && !selectedPosts.isEmpty()) {
                    postsToReset = new ArrayList<>();
                    postsToReset.addAll(selectedPosts);

                    SetCategoryToListOfPosts setCategoryToListOfPosts =
                            new SetCategoryToListOfPosts(SingleCollectionFragment.this, postsToReset,
                                    null, progressDialogBatch,
                                    EditBookmarksType.REMOVE_FROM_THIS_COLLECTION);
                    setCategoryToListOfPosts.setOnFragmentRefreshCallback(SingleCollectionFragment.this);
                    setCategoryToListOfPosts.execute();
                }
            }
        });

        // setup toolbar with title from category
        Toolbar toolbar = v.findViewById(R.id.toolbar);
        toolbar.setTitle(category);
        FragmentHelper.setupToolbarWithBackButton(toolbar, new WeakReference<>(SingleCollectionFragment.this));

        return v;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (permsRequestCode == 200) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted. Continue the action or workflow in your app.
                // start BatchDownloadPosts async task
                new BatchDownloadPosts(SingleCollectionFragment.this, postsToDownload, progressDialogBatch).execute();
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
     * Shows the textView "No posts in collection" if collection is empty
     */
    private void showTextNoBookmarksIfNoPosts() {
        if (listPostsInCollectionBookmarked == null || listPostsInCollectionBookmarked.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            textViewNoBookmarksInSingleCollection.setVisibility(View.VISIBLE);
            textViewNoBookmarksInSingleCollection.setText(R.string.no_bookmarks_in_collection);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            textViewNoBookmarksInSingleCollection.setVisibility(View.GONE);
        }
    }

    /**
     * Removes all posts in list postsToRemove from bookmarks in storage
     *
     * @param listPostsToRemove ArrayList<Post> posts to be removed from bookmarks
     */
    private void removeBookmarkedPostsInList(List<Post> listPostsToRemove) {
        RemoveBookmarksInList removeBookmarksInList =
                new RemoveBookmarksInList(SingleCollectionFragment.this, listPostsToRemove);
        removeBookmarksInList.execute();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            // update adapter
            refreshAdapter();

            // set highlighted item on nav bar to "bookmarks"
            FragmentHelper.setBottomNavViewSelectElem(getActivity(), R.id.navigation_collections);
        } else {
            // close contextual menu
            if (adapter != null && RecViewAdapterSingleCollection.actionMode != null) {
                RecViewAdapterSingleCollection.actionMode.finish();
            }
        }
    }

    /**
     * Refreshes adapter to display changes to data in view
     */
    private void refreshAdapter() {
        if (category != null && adapter != null) {
            listPostsInCollectionBookmarked = FragmentHelper.getAllBookmarkedPostsOfCollection(category, requireContext());
            adapter.setItems(listPostsInCollectionBookmarked);
            adapter.notifyDataSetChanged();

            showTextNoBookmarksIfNoPosts();
        }
    }

    /**
     * Moves bookmarks to other collection (other category)
     *
     * @param category name of new collection
     * @param editMode EditBookmarksType
     */
    @Override
    public void savePostOrListToCollection(String category, EditBookmarksType editMode) {
        if (postsToMove != null && !postsToMove.isEmpty() &&
                category != null && !category.isEmpty()) {
            SetCategoryToListOfPosts setCategoryToListOfPosts = new SetCategoryToListOfPosts(SingleCollectionFragment.this,
                    postsToMove, category, progressDialogBatch, editMode);
            setCategoryToListOfPosts.setOnFragmentRefreshCallback(SingleCollectionFragment.this);
            setCategoryToListOfPosts.execute();
        }
    }

    @Override
    public void openBtmSheetDialogAddCollection(EditBookmarksType editMode) {
        // open BottomSheetDialogAddCollection
        BtmSheetDialogAddCollection bottomSheetAddCollection = new BtmSheetDialogAddCollection(null, editMode);

        // set FragmentCallbackListener
        bottomSheetAddCollection.setOnFragmentCallbackListener(SingleCollectionFragment.this);

        //show Fragment
        bottomSheetAddCollection.show(requireActivity().getSupportFragmentManager(), BtmSheetDialogAddCollection.class.getSimpleName());
    }

    @Override
    public void refreshAdapterCallback() {
        refreshAdapter();
    }

    /**
     * Async task to remove all bookmarks in list
     */
    private static class RemoveBookmarksInList extends AsyncTask<Void, Integer, Void> {

        private final WeakReference<SingleCollectionFragment> fragmentReference;

        private final List<Post> listPostsToRemove;
        private ProgressDialog progressDialogBatch;
        private int editedItems = 0;
        private boolean removingSuccessful = false;

        // constructor
        RemoveBookmarksInList(SingleCollectionFragment context, List<Post> listPostsToRemove) {
            fragmentReference = new WeakReference<>(context);
            this.listPostsToRemove = listPostsToRemove;
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
                final SingleCollectionFragment fragment = fragmentReference.get();
                if (fragment != null) {

                    if (listPostsToRemove != null && !listPostsToRemove.isEmpty()) {
                        // set initial max size to zero
                        progressDialogBatch.setMax(0);

                        // set max size of progressDialog
                        int progressMaxSize = listPostsToRemove.size();

                        // set length of progressDialog
                        progressDialogBatch.setMax(progressMaxSize);

                        for (Post post : listPostsToRemove) {
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
                final SingleCollectionFragment fragment = fragmentReference.get();
                if (fragment != null) {
                    if (removingSuccessful) {
                        FragmentHelper.showToast(fragment.requireContext().getString(R.string.selected_bookmarks_removed_success), fragment.requireActivity(), fragment.requireContext());

                        // refresh bookmarked posts
                        fragment.refreshAdapter();
                    } else {
                        FragmentHelper.showToast(fragment.requireContext().getString(R.string.selected_bookmarks_removed_fail), fragment.requireActivity(), fragment.requireContext());
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
                    final SingleCollectionFragment fragment = fragmentReference.get();

                    if (fragment != null) {
                        progressDialogBatch = new ProgressDialog(fragment.requireContext());
                        progressDialogBatch.setTitle(fragment.requireContext().getString(R.string.remove_bookmarks_confirm_dialog_title));
                        progressDialogBatch.setMessage(fragment.requireContext().getString(R.string.remove_bookmarks_list_message));
                        progressDialogBatch.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                        progressDialogBatch.setProgress(0);
                        progressDialogBatch.show();
                    }
                }
            }
        }
    }
}