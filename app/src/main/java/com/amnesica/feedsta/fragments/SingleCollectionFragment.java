package com.amnesica.feedsta.fragments;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amnesica.feedsta.R;
import com.amnesica.feedsta.adapter.collections.RecViewAdapterSingleCollection;
import com.amnesica.feedsta.asynctasks.SetCategoryToListOfPosts;
import com.amnesica.feedsta.asynctasks.download.DownloadPostsBatch;
import com.amnesica.feedsta.fragments.bottomsheets.BtmSheetDialogAddCollection;
import com.amnesica.feedsta.fragments.bottomsheets.BtmSheetDialogSelectCollection;
import com.amnesica.feedsta.helper.FragmentHelper;
import com.amnesica.feedsta.helper.StorageHelper;
import com.amnesica.feedsta.helper.collections.EditBookmarksType;
import com.amnesica.feedsta.interfaces.collections.FragmentRefreshCallback;
import com.amnesica.feedsta.interfaces.collections.OnItemClickListSingleColl;
import com.amnesica.feedsta.interfaces.collections.SelectOrAddCollectionCallback;
import com.amnesica.feedsta.models.Post;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/** Fragment for displaying bookmarked posts in a collection */
public class SingleCollectionFragment extends Fragment
    implements SelectOrAddCollectionCallback, FragmentRefreshCallback {

  // view stuff
  private RecyclerView recyclerView;
  private RecViewAdapterSingleCollection adapter;
  private TextView textViewNoBookmarksInSingleCollection;
  private ProgressDialog progressDialogBatch;

  // list with bookmarks of collection
  private List<Post> listPostsInCollectionBookmarked;

  // name of collection
  private String category;

  private List<Post> postsToMove;

  public SingleCollectionFragment() {
    // Required empty public constructor
  }

  static SingleCollectionFragment newInstance(
      List<Post> listPostsInCollectionBookmarked, String category) {
    Bundle args = new Bundle();
    args.putSerializable("bookmarksInCollection", (Serializable) listPostsInCollectionBookmarked);
    args.putSerializable("category", category);
    SingleCollectionFragment fragment = new SingleCollectionFragment();
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    // inflate the layout for this fragment
    View view = inflater.inflate(R.layout.fragment_single_collection, container, false);

    getBookmarksAndCategoryFromArguments();

    setupToolbarWithCategoryAsTitle(view);

    recyclerView = view.findViewById(R.id.recycler_view_single_collection);
    RecyclerView.LayoutManager layoutManager = new GridLayoutManager(requireContext(), 3);
    recyclerView.setLayoutManager(layoutManager);

    textViewNoBookmarksInSingleCollection =
        view.findViewById(R.id.textNoBookmarksInSingleCollection);
    showTextNoBookmarksIfNoPosts();

    setupAdapter();

    return view;
  }

  private void getBookmarksAndCategoryFromArguments() {
    if (this.getArguments() != null) {
      if (getArguments().getSerializable("bookmarksInCollection") != null) {
        listPostsInCollectionBookmarked =
            (List<Post>) getArguments().getSerializable("bookmarksInCollection");
        category = (String) getArguments().getSerializable("category");
      }
    }
  }

  /**
   * Sets up toolbar with with title and back button
   *
   * @param view View
   */
  private void setupToolbarWithCategoryAsTitle(View view) {
    Toolbar toolbar = view.findViewById(R.id.toolbar);
    toolbar.setTitle(category);
    FragmentHelper.setupToolbarWithBackButton(
        toolbar, new WeakReference<>(SingleCollectionFragment.this));
  }

  /**
   * Sets up itemClickListener to go to postFragment and show actions on long click on items in
   * collection
   */
  private void setupAdapter() {
    adapter = new RecViewAdapterSingleCollection(listPostsInCollectionBookmarked);
    recyclerView.setAdapter(adapter);
    adapter.setOnItemClickListener(
        new OnItemClickListSingleColl() {
          @Override
          public void onItemClick(int position) {
            showPost(position);
          }

          @Override
          public void removeBookmarkedPostsFromStorage(List<Post> listPostsToRemove) {
            removeBookmarkedPostsInList(listPostsToRemove);
          }

          @Override
          public void moveBookmarksToOtherCollection(List<Post> selectedPosts) {
            moveToOtherCollection(selectedPosts);
          }

          @Override
          public void downloadSelectedBookmarks(List<Post> selectedPosts) {
            downloadBookmarks(selectedPosts);
          }

          @Override
          public void resetCategoryOnSelectedBookmarks(List<Post> selectedPosts) {
            resetCategorySelectedBookmarks(selectedPosts);
          }
        });
  }

  private void resetCategorySelectedBookmarks(List<Post> selectedPosts) {
    if (selectedPosts != null && !selectedPosts.isEmpty()) {
      List<Post> postsToReset = new ArrayList<>();
      postsToReset.addAll(selectedPosts);

      SetCategoryToListOfPosts setCategoryToListOfPosts =
          new SetCategoryToListOfPosts(
              SingleCollectionFragment.this,
              postsToReset,
              null,
              progressDialogBatch,
              EditBookmarksType.REMOVE_FROM_THIS_COLLECTION);
      setCategoryToListOfPosts.setOnFragmentRefreshCallback(SingleCollectionFragment.this);
      setCategoryToListOfPosts.execute();
    }
  }

  private void downloadBookmarks(List<Post> selectedPosts) {
    if (selectedPosts != null && !selectedPosts.isEmpty()) {
      List<Post> postsToDownload = new ArrayList<>(selectedPosts);

      new DownloadPostsBatch(SingleCollectionFragment.this, postsToDownload, progressDialogBatch)
          .execute();
    }
  }

  private void moveToOtherCollection(List<Post> selectedPosts) {
    if (selectedPosts != null && !selectedPosts.isEmpty()) {
      postsToMove = new ArrayList<>();
      postsToMove.addAll(selectedPosts);

      if (FragmentHelper.collectionsAlreadyExist(requireContext())) {
        BtmSheetDialogSelectCollection.show(
            EditBookmarksType.MOVE_BOOKMARKS,
            SingleCollectionFragment.this,
            SingleCollectionFragment.this);
      } else {
        BtmSheetDialogAddCollection.show(
            null,
            SingleCollectionFragment.this,
            EditBookmarksType.MOVE_BOOKMARKS,
            SingleCollectionFragment.this);
      }
    }
  }

  private void showPost(int position) {
    // go to bookmark post
    Post postToSend = listPostsInCollectionBookmarked.get(position);

    if (postToSend != null) {
      // new postFragment
      PostFragment postFragment = PostFragment.newInstanceWithBookmarkedPost(postToSend);

      // add fragment to container
      FragmentHelper.addFragmentToContainer(
          postFragment, requireActivity().getSupportFragmentManager());
    }
  }

  /** Shows the textView "No posts in collection" if collection is empty */
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
   * @param listPostsToRemove List<Post>
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

  /** Refreshes adapter to display changes to data in view */
  private void refreshAdapter() {
    if (category != null && adapter != null) {
      listPostsInCollectionBookmarked =
          FragmentHelper.getAllBookmarkedPostsOfCollection(category, requireContext());
      adapter.setItems(listPostsInCollectionBookmarked);
      adapter.notifyDataSetChanged();

      showTextNoBookmarksIfNoPosts();
    }
  }

  @Override
  public void refreshAdapterFromCallback() {
    refreshAdapter();
  }

  /** Moves bookmarks to other collection (other category) */
  @Override
  public void savePostOrListToCollection(
      String category, EditBookmarksType editMode, Fragment fragment) {
    if (postsToMove != null && !postsToMove.isEmpty() && category != null && !category.isEmpty()) {
      SetCategoryToListOfPosts setCategoryToListOfPosts =
          new SetCategoryToListOfPosts(
              SingleCollectionFragment.this, postsToMove, category, progressDialogBatch, editMode);

      setCategoryToListOfPosts.setOnFragmentRefreshCallback(SingleCollectionFragment.this);
      setCategoryToListOfPosts.execute();
    }
  }

  @Override
  public void openAddCollectionBtmSheet() {
    BtmSheetDialogAddCollection.show(
        category,
        SingleCollectionFragment.this,
        EditBookmarksType.MOVE_BOOKMARKS,
        SingleCollectionFragment.this);
  }

  /** Async task to remove all bookmarks in list */
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
      if (isCancelled()) return null;

      // get reference from fragment
      final SingleCollectionFragment fragment = fragmentReference.get();
      if (fragment == null) return null;

      if (listPostsToRemove != null && !listPostsToRemove.isEmpty()) {
        // set initial max size to zero
        progressDialogBatch.setMax(0);

        // set max size of progressDialog
        int progressMaxSize = listPostsToRemove.size();

        // set length of progressDialog
        progressDialogBatch.setMax(progressMaxSize);

        for (Post post : listPostsToRemove) {
          removingSuccessful =
              StorageHelper.removePostFromInternalStorage(
                  post, fragment.requireContext(), StorageHelper.FILENAME_BOOKMARKS);

          if (!removingSuccessful) {
            return null;
          }

          publishProgress(editedItems += 1);
        }
      }
      return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
      super.onPostExecute(aVoid);
      if (isCancelled()) return;

      // dismiss progress dialog
      progressDialogBatch.dismiss();

      // get reference from fragment
      final SingleCollectionFragment fragment = fragmentReference.get();
      if (fragment == null) return;

      if (removingSuccessful) {
        FragmentHelper.showToast(
            fragment.requireContext().getString(R.string.selected_bookmarks_removed_success),
            fragment.requireActivity(),
            fragment.requireContext());

        // refresh bookmarked posts
        fragment.refreshAdapter();
      } else {
        FragmentHelper.showToast(
            fragment.requireContext().getString(R.string.selected_bookmarks_removed_fail),
            fragment.requireActivity(),
            fragment.requireContext());
      }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
      super.onProgressUpdate(values);
      progressDialogBatch.setProgress(values[0]);
    }

    private void showProgressDialog() {
      if (isCancelled()) return;

      // get reference from fragment
      final SingleCollectionFragment fragment = fragmentReference.get();

      if (fragment == null) return;
      progressDialogBatch = new ProgressDialog(fragment.requireContext());
      progressDialogBatch.setTitle(
          fragment.requireContext().getString(R.string.remove_bookmarks_confirm_dialog_title));
      progressDialogBatch.setMessage(
          fragment.requireContext().getString(R.string.remove_bookmarks_list_message));
      progressDialogBatch.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
      progressDialogBatch.setProgress(0);
      progressDialogBatch.show();
    }
  }
}
