package com.amnesica.feedsta.views;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

import com.amnesica.feedsta.R;
import com.amnesica.feedsta.helper.EditBookmarksType;
import com.amnesica.feedsta.helper.FragmentHelper;
import com.amnesica.feedsta.interfaces.FragmentCallback;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * Fragment for adding a collection of bookmarks (used in CollectionsFragment, SingleCollectionFragment and
 * PostFragment)
 */
public class BtmSheetDialogAddCollection extends BottomSheetDialogFragment {

    // view stuff
    private View view;
    private TextView textInputField;

    private final EditBookmarksType editMode;
    private String inputString = null;
    private FragmentCallback callback;

    // case when collection should be renamed -> initial text in inputField
    private final String initialCollectionName;

    public BtmSheetDialogAddCollection(String currentCategoryName, EditBookmarksType editMode) {
        super();
        initialCollectionName = currentCategoryName;
        this.editMode = editMode;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.bottom_sheet_add_collection, container, false);

        Button buttonCancel = view.findViewById(R.id.bottom_sheet_cancel_button);
        textInputField = view.findViewById(R.id.bottom_sheet_editText);
        TextView bottomSheetTitle = view.findViewById(R.id.bottom_sheet_title);

        // set initial category if string is not null
        if (initialCollectionName != null) {
            textInputField.setText(initialCollectionName);
            bottomSheetTitle.setText(R.string.bottom_sheet_rename_collection_title);
        }

        if (editMode.equals(EditBookmarksType.MOVE_BOOKMARKS)) {
            bottomSheetTitle.setText(R.string.bottom_sheet_move_bookmarks_new_coll);
        }

        // change category of posts with inputString
        textInputField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_DONE) {

                    // get text from inputField
                    inputString = textInputField.getText().toString();

                    // validate string
                    if (inputStringIsValid(inputString)) {

                        // clear textField
                        textInputField.setText(null);

                        // check if collection with name already exists
                        if (!FragmentHelper.collectionWithNameDoesExist(inputString, requireContext())) {

                            if (callback != null) {
                                callback.savePostOrListToCollection(inputString, editMode);
                            }

                        } else {
                            FragmentHelper.showToast(
                                    requireContext().getString(R.string.collection_with_name_already_exists),
                                    requireActivity(), requireContext());
                        }

                        // hide keyboard
                        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(
                                Context.INPUT_METHOD_SERVICE);
                        assert imm != null;
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

                        dismiss();
                    }
                    return true;
                }
                return false;
            }
        });

        // set theme to bottom sheet
        // set text color based on theme
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = requireContext().getTheme();
        theme.resolveAttribute(R.attr.colorAccent, typedValue, true);
        @ColorInt final int textColor = typedValue.data;
        buttonCancel.setTextColor(textColor);

        // set background color based on theme
        theme.resolveAttribute(R.attr.colorPrimary, typedValue, true);
        @ColorInt final int backgroundColor = typedValue.data;
        buttonCancel.setBackgroundColor(backgroundColor);

        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        return view;
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

    public void setOnFragmentCallbackListener(FragmentCallback callback) {
        this.callback = callback;
    }
}
