package com.amnesica.feedsta.helper.snackbar;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.amnesica.feedsta.R;
import com.amnesica.feedsta.fragments.bottomsheets.BtmSheetDialogAddCollection;
import com.amnesica.feedsta.fragments.bottomsheets.BtmSheetDialogSelectCollection;
import com.amnesica.feedsta.helper.FragmentHelper;
import com.amnesica.feedsta.helper.StorageHelper;
import com.amnesica.feedsta.helper.collections.EditBookmarksType;
import com.amnesica.feedsta.interfaces.collections.SelectOrAddCollectionCallback;
import com.amnesica.feedsta.models.Post;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.lang.ref.WeakReference;

public class SnackbarHelper {

  private final Fragment callingFragment;
  private final SnackbarAlertType snackbarAlertType;

  public SnackbarHelper(Fragment callingFragment, SnackbarAlertType snackbarAlertType) {
    this.callingFragment = callingFragment;
    this.snackbarAlertType = snackbarAlertType;

    final Fragment fragment = new WeakReference<>(callingFragment).get();
    if (fragment == null) return;

    if (snackbarAlertType.equals(SnackbarAlertType.SNACKBAR_ON_MAIN_ACTIVITY)) {
      // TODO Snackbar should be displayed on main activity -> internet connection lost etc.
    } else if (snackbarAlertType.equals(SnackbarAlertType.SNACKBAR_ON_POST)) {
      // TODO Snackbar should be displayed on post -> bookmark saved etc.
    }
  }

  /** Shows a custom snackbar on top of the bottom nav view */
  public void showSnackBarWithSpecificText(final String text) {
    final Fragment fragment = new WeakReference<>(callingFragment).get();
    if (fragment == null) return;

    Snackbar snackbar =
        Snackbar.make(
            fragment.requireActivity().findViewById(R.id.conLayCustomSnackBarAlert),
            text,
            Snackbar.LENGTH_LONG);
    snackbar.setAction(R.string.okay, v -> snackbar.dismiss());

    setSnackbarStyle(snackbar, fragment);

    snackbar.show();
  }

  private void setSnackbarStyle(@NonNull Snackbar snackbar, @NonNull Fragment fragment) {
    snackbar.getView().setElevation(0);
    snackbar.getView().setBackgroundResource(R.drawable.background_round_corners_success);
    snackbar.setBackgroundTint(
        FragmentHelper.getColorId(fragment.requireContext(), R.attr.colorPrimaryContainer));
    snackbar.setTextColor(FragmentHelper.getColorId(fragment.requireContext(), R.attr.colorAccent));
    snackbar.setActionTextColor(
        FragmentHelper.getColorId(fragment.requireContext(), R.attr.colorAccent));

    BottomNavigationView bottomNavigationView =
        fragment.requireActivity().findViewById(R.id.nav_view);
    if (bottomNavigationView == null) return;
    snackbar.setAnchorView(bottomNavigationView);
  }

  public void showSnackBarToSaveInCollection(
      Post post, final SelectOrAddCollectionCallback selectOrAddCollectionCallback) {
    final Fragment fragment = new WeakReference<>(callingFragment).get();
    if (fragment == null || post == null) return;

    Snackbar snackbar =
        Snackbar.make(
            fragment.requireActivity().findViewById(R.id.conLayCustomSnackBarAlert),
            fragment.getString(R.string.snackbar_bookmark_saved),
            Snackbar.LENGTH_LONG);
    snackbar.setAction(
        R.string.snackbar_action_save_to_collection,
        v -> {
          showSelectOrAddCollectionBtmDialog(
              callingFragment,
              post,
              EditBookmarksType.SAVE_BOOKMARKS,
              selectOrAddCollectionCallback);
          snackbar.dismiss();
        });

    setSnackbarStyle(snackbar, fragment);

    snackbar.show();
  }

  public void showSelectOrAddCollectionBtmDialog(
      @NonNull Fragment callingFragment,
      Post post,
      EditBookmarksType editMode,
      SelectOrAddCollectionCallback selectOrAddCollectionCallback) {
    // set action but check first if there are collections
    if (FragmentHelper.collectionsAlreadyExist(callingFragment.requireContext())) {
      if (StorageHelper.checkIfAccountOrPostIsInFile(
          post, StorageHelper.FILENAME_BOOKMARKS, callingFragment.requireContext())) {
        // open dialog to select existing collection for bookmark
        BtmSheetDialogSelectCollection.show(
            editMode, callingFragment, selectOrAddCollectionCallback);
      } else {
        showToastPostIsNotBookmarkedYet(callingFragment);
      }
    } else {
      if (StorageHelper.checkIfAccountOrPostIsInFile(
          post, StorageHelper.FILENAME_BOOKMARKS, callingFragment.requireContext())) {
        // open dialog to add bookmark to collection
        BtmSheetDialogAddCollection.show(
            post.getCategory(), callingFragment, editMode, selectOrAddCollectionCallback);
      } else {
        showToastPostIsNotBookmarkedYet(callingFragment);
      }
    }
  }

  private void showToastPostIsNotBookmarkedYet(@NonNull Fragment callingFragment) {
    FragmentHelper.showToast(
        callingFragment.getString(R.string.post_not_bookmarked_yet),
        callingFragment.requireActivity(),
        callingFragment.requireContext());
  }
}
