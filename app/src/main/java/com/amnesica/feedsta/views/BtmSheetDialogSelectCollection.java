package com.amnesica.feedsta.views;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amnesica.feedsta.R;
import com.amnesica.feedsta.adapter.RecViewAdapterSelectCollection;
import com.amnesica.feedsta.helper.EditBookmarksType;
import com.amnesica.feedsta.helper.FragmentHelper;
import com.amnesica.feedsta.interfaces.SelectOrAddCollectionCallback;
import com.amnesica.feedsta.models.Collection;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Fragment for selecting a collection of bookmarks (used in PostFragment and
 * SingleCollectionFragment)
 */
public class BtmSheetDialogSelectCollection extends BottomSheetDialogFragment {

  private RecViewAdapterSelectCollection adapter;

  private final EditBookmarksType editMode;
  private final WeakReference<Fragment> fragmentReference;

  private List<Collection> listCollectionsBookmarked;

  private final SelectOrAddCollectionCallback selectOrAddCollectionCallback;

  public BtmSheetDialogSelectCollection(
      EditBookmarksType editMode,
      Fragment callingFragment,
      SelectOrAddCollectionCallback selectOrAddCollectionCallback) {
    this.editMode = editMode;
    this.fragmentReference = new WeakReference<>(callingFragment);
    this.selectOrAddCollectionCallback = selectOrAddCollectionCallback;
  }

  /**
   * Opens dialog to select existing collection for bookmark
   *
   * @param editMode EditBookmarksType
   * @param callingFragment Fragment
   * @param selectOrAddCollectionCallback SelectCollectionCallback (to set the category for post(s))
   */
  public static void show(
      EditBookmarksType editMode,
      Fragment callingFragment,
      SelectOrAddCollectionCallback selectOrAddCollectionCallback) {
    BtmSheetDialogSelectCollection btmSheetDialogSelectCollection =
        new BtmSheetDialogSelectCollection(
            editMode, callingFragment, selectOrAddCollectionCallback);

    btmSheetDialogSelectCollection.show(
        callingFragment.requireActivity().getSupportFragmentManager(),
        BtmSheetDialogSelectCollection.class.getSimpleName());
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.bottom_sheet_select_collection, container, false);

    setupAddCollectionButton(view);

    setupCancelButton(view);

    setupTitle(view);

    RecyclerView recyclerView = view.findViewById(R.id.recycler_view_collections_bottom_sheet);
    RecyclerView.LayoutManager layoutManager = new GridLayoutManager(requireContext(), 3);
    recyclerView.setLayoutManager(layoutManager);

    // create collections in listCollectionsBookmarked (except "All"-collection)
    listCollectionsBookmarked = getCollectionsExceptAllCollectionFromStorage();

    setupRecViewAdapter(recyclerView);

    return view;
  }

  /**
   * Sets up the adapter with clickListener to select collection
   *
   * @param recyclerView RecyclerView
   */
  private void setupRecViewAdapter(RecyclerView recyclerView) {
    adapter = new RecViewAdapterSelectCollection(listCollectionsBookmarked);
    adapter.setOnItemClickListener(
        position -> {
          selectCollection(position);
          dismiss();
        });
    recyclerView.setAdapter(adapter);
  }

  private void selectCollection(int position) {
    if (selectOrAddCollectionCallback != null) {
      final Fragment fragment = fragmentReference.get();
      if (fragment == null) return;

      String category = listCollectionsBookmarked.get(position).getName();
      selectOrAddCollectionCallback.savePostOrListToCollection(category, editMode, fragment);
    }

    dismiss();
  }

  /**
   * Sets up the title of the bottom sheet to "Move to collection" or "Save to collection" depending
   * on the editMode
   *
   * @param view View
   */
  private void setupTitle(View view) {
    if (view == null) return;

    TextView bottomSheetTitle = view.findViewById(R.id.bottom_sheet_select_title);
    if (bottomSheetTitle != null) {
      if (editMode.equals(EditBookmarksType.MOVE_BOOKMARKS)) {
        bottomSheetTitle.setText(
            getResources().getString(R.string.bottom_sheet_select_collection_title_move));
      } else {
        bottomSheetTitle.setText(
            getResources().getString(R.string.bottom_sheet_select_collection_title));
      }
    }
  }

  /**
   * Sets up the "Add"-button depending on the theme
   *
   * @param view View
   */
  private void setupAddCollectionButton(View view) {
    if (view == null) return;

    ImageView imageAddCollection = view.findViewById(R.id.imageAddCollection);
    imageAddCollection.setBackgroundResource(R.drawable.ic_baseline_add_24dp);

    imageAddCollection.setOnClickListener(
        view1 -> {
          final Fragment fragment = fragmentReference.get();
          if (fragment == null) return;

          if (selectOrAddCollectionCallback != null) {
            selectOrAddCollectionCallback.openAddCollectionBtmSheet();
          }

          dismiss();
        });
  }

  /** Returns all collections in storage except "All"-collection */
  private List<Collection> getCollectionsExceptAllCollectionFromStorage() {
    List<Collection> listCollectionsBookmarked =
        FragmentHelper.createCollectionsFromBookmarks(requireContext());

    // get all collections except "All"-collection
    return listCollectionsBookmarked.stream()
        .filter(collection -> !collection.getName().equals("All"))
        .collect(Collectors.toList());
  }

  /**
   * Sets up the cancel button to dismiss the dialog
   *
   * @param view View
   */
  private void setupCancelButton(View view) {
    if (view == null) return;

    Button buttonCancel = view.findViewById(R.id.bottom_sheet_select_cancel_button);

    // set theme to bottom sheet
    buttonCancel.setTextColor(FragmentHelper.getColorId(requireContext(), R.attr.colorAccent));
    buttonCancel.setBackgroundColor(
        FragmentHelper.getColorId(requireContext(), R.attr.colorPrimary));

    buttonCancel.setOnClickListener(view1 -> dismiss());
  }

  @Override
  public void onResume() {
    super.onResume();
    if (adapter != null) {
      listCollectionsBookmarked = getCollectionsExceptAllCollectionFromStorage();
      adapter.setItems(listCollectionsBookmarked);
      adapter.notifyDataSetChanged();
    }
  }
}
