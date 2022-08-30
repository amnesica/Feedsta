package com.amnesica.feedsta.fragments.bottomsheets;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.amnesica.feedsta.R;
import com.amnesica.feedsta.helper.FragmentHelper;
import com.amnesica.feedsta.helper.collections.EditBookmarksType;
import com.amnesica.feedsta.interfaces.collections.SelectOrAddCollectionCallback;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.lang.ref.WeakReference;

/**
 * Fragment for adding a collection of bookmarks (used in CollectionsFragment,
 * SingleCollectionFragment and PostFragment)
 */
public class BtmSheetDialogAddCollection extends BottomSheetDialogFragment {

  private final EditBookmarksType editMode;
  private String inputString = null;
  private final WeakReference<Fragment> fragmentReference;
  private final SelectOrAddCollectionCallback selectOrAddCollectionCallback;

  // case when collection should be renamed -> initial text in inputField
  private final String initialCollectionName;

  public BtmSheetDialogAddCollection(
      String currentCategoryName,
      Fragment callingFragment,
      EditBookmarksType editMode,
      SelectOrAddCollectionCallback selectOrAddCollectionCallback) {
    initialCollectionName = currentCategoryName;
    this.editMode = editMode;
    this.fragmentReference = new WeakReference<>(callingFragment);
    this.selectOrAddCollectionCallback = selectOrAddCollectionCallback;
  }

  public static void show(
      String currentCategoryName,
      Fragment callingFragment,
      EditBookmarksType editMode,
      SelectOrAddCollectionCallback selectOrAddCollectionCallback) {
    BtmSheetDialogAddCollection btmSheetDialogAddCollection =
        new BtmSheetDialogAddCollection(
            currentCategoryName, callingFragment, editMode, selectOrAddCollectionCallback);

    btmSheetDialogAddCollection.show(
        callingFragment.requireActivity().getSupportFragmentManager(),
        BtmSheetDialogAddCollection.class.getSimpleName());
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.bottom_sheet_add_collection, container, false);

    setupCancelButton(view);

    setupTextInputField(view);

    setupBottomSheetTitle(view);

    return view;
  }

  private void setupBottomSheetTitle(View view) {
    TextView bottomSheetTitle = view.findViewById(R.id.bottom_sheet_title);

    // set initial category if string is not null
    if (initialCollectionName != null) {
      bottomSheetTitle.setText(R.string.bottom_sheet_rename_collection_title);
    }

    if (editMode.equals(EditBookmarksType.MOVE_BOOKMARKS)) {
      bottomSheetTitle.setText(R.string.bottom_sheet_move_bookmarks_new_coll);
    }
  }

  private void setupTextInputField(View view) {
    TextView textInputField = view.findViewById(R.id.bottom_sheet_editText);

    // set initial category if string is not null
    if (initialCollectionName != null) {
      textInputField.setText(initialCollectionName);
    }

    textInputField.setOnEditorActionListener(
        (textView, i, keyEvent) -> {
          if (i == EditorInfo.IME_ACTION_DONE) {
            final Fragment fragment = fragmentReference.get();
            if (fragment == null) return false;

            inputString = textInputField.getText().toString();

            if (inputStringIsValid(inputString)) {
              // clear textField
              textInputField.setText(null);

              if (!FragmentHelper.collectionWithNameDoesExist(inputString, requireContext())) {
                if (selectOrAddCollectionCallback != null) {
                  selectOrAddCollectionCallback.savePostOrListToCollection(
                      inputString, editMode, fragment);
                }

              } else {
                FragmentHelper.showToast(
                    requireContext().getString(R.string.collection_with_name_already_exists),
                    requireActivity(),
                    requireContext());
              }

              hideKeyboard(view);

              dismiss();
            }
            return true;
          }
          return false;
        });
  }

  /**
   * Sets up the cancel button to dismiss the dialog
   *
   * @param view View
   */
  private void setupCancelButton(View view) {
    if (view == null) return;

    Button buttonCancel = view.findViewById(R.id.bottom_sheet_cancel_button);

    // set theme to bottom sheet
    buttonCancel.setTextColor(FragmentHelper.getColorId(requireContext(), R.attr.colorAccent));
    buttonCancel.setBackgroundColor(
        FragmentHelper.getColorId(requireContext(), R.attr.colorPrimary));

    buttonCancel.setOnClickListener(view1 -> dismiss());
  }

  private void hideKeyboard(View view) {
    InputMethodManager imm =
        (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    assert imm != null;
    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
  }

  /**
   * Validates input string of inputField
   *
   * @param inputString String
   * @return boolean
   */
  private boolean inputStringIsValid(String inputString) {
    return inputString != null && !inputString.isEmpty();
  }
}
