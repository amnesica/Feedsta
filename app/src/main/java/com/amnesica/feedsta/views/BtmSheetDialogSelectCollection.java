package com.amnesica.feedsta.views;

import android.app.ProgressDialog;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amnesica.feedsta.R;
import com.amnesica.feedsta.adapter.RecViewAdapterSelectCollection;
import com.amnesica.feedsta.helper.EditBookmarksType;
import com.amnesica.feedsta.helper.FragmentHelper;
import com.amnesica.feedsta.interfaces.FragmentCallback;
import com.amnesica.feedsta.interfaces.OnItemClickListenerSelectColl;
import com.amnesica.feedsta.models.Collection;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Predicate;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

/**
 * Fragment for selecting a collection of bookmarks
 */
public class BtmSheetDialogSelectCollection extends BottomSheetDialogFragment {

    // view stuff
    private TextView bottomSheetTitle;
    private Button buttonCancel;
    private ImageView imageAddCollection;
    private ProgressDialog progressDialogBatch;

    private RecViewAdapterSelectCollection adapter;
    private List<Collection> listCollectionsBookmarked;
    private final EditBookmarksType editMode;
    private FragmentCallback callback;

    public BtmSheetDialogSelectCollection(EditBookmarksType editMode) {
        this.editMode = editMode;
    }

    public void setOnFragmentCallbackListener(FragmentCallback callback) {
        this.callback = callback;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable
            ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.bottom_sheet_select_collection,
                container, false);

        imageAddCollection = v.findViewById(R.id.imageAddCollection);
        setupAddCollectionButton();

        buttonCancel = v.findViewById(R.id.bottom_sheet_select_cancel_button);
        setupCancelButton();

        bottomSheetTitle = v.findViewById(R.id.bottom_sheet_select_title);
        setupTitle();

        RecyclerView recyclerView = v.findViewById(R.id.recycler_view_collections_bottom_sheet);
        // use this setting to
        // improve performance if you know that changes
        // in content do not change the layout size
        // of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(requireContext(), 3);
        recyclerView.setLayoutManager(layoutManager);

        // create collections in listCollectionsBookmarked (except "All"-collection)
        listCollectionsBookmarked = getCollectionsExceptAllCollectionFromStorage();

        // define an adapter
        adapter = new RecViewAdapterSelectCollection(listCollectionsBookmarked);
        recyclerView.setAdapter(adapter);

        // set onClickListener to save selected category to post
        adapter.setOnItemClickListener(new OnItemClickListenerSelectColl() {
            @Override
            public void onItemClick(int position) {
                String category = listCollectionsBookmarked.get(position).getName();

                if (callback != null && category != null) {
                    callback.savePostOrListToCollection(category, editMode);
                }

                dismiss();
            }
        });

        return v;
    }

    /**
     * Sets up the title of the bottom sheet to "Move to collection" or
     * "Save to collection" depending on the editMode
     */
    private void setupTitle() {
        if (bottomSheetTitle != null) {
            if (editMode.equals(EditBookmarksType.MOVE_BOOKMARKS)) {
                bottomSheetTitle.setText(getResources().getString(R.string.bottom_sheet_select_collection_title_move));
            } else {
                bottomSheetTitle.setText(getResources().getString(R.string.bottom_sheet_select_collection_title));
            }
        }
    }

    /**
     * Sets up the "Add"-button depending on the theme
     */
    private void setupAddCollectionButton() {
        if (FragmentHelper.getThemeIsDarkTheme(getContext())) {
            imageAddCollection.setBackgroundResource(R.drawable.ic_baseline_add_white_24);
        } else {
            imageAddCollection.setBackgroundResource(R.drawable.ic_baseline_add_black_24);
        }

        imageAddCollection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (callback != null) {
                    callback.openBtmSheetDialogAddCollection(editMode);
                }

                dismiss();
            }
        });
    }

    /**
     * Returns all collections in storage except "All"-collection
     */
    private List<Collection> getCollectionsExceptAllCollectionFromStorage() {
        List<Collection> listCollectionsBookmarked = FragmentHelper.createCollectionsFromBookmarks(requireContext());

        // get all collections except "All"-collection
        return Stream.of(listCollectionsBookmarked).filter(new Predicate<Collection>() {
            @Override
            public boolean test(Collection c) {
                return !c.getName().equals("All");
            }
        }).collect(Collectors.<Collection>toList());
    }

    /**
     * Sets up the cancel button to dismiss the operation
     */
    private void setupCancelButton() {
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
