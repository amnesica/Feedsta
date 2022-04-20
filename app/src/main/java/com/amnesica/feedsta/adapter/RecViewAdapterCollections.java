package com.amnesica.feedsta.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.util.Base64;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.amnesica.feedsta.R;
import com.amnesica.feedsta.interfaces.OnItemClickListenerColl;
import com.amnesica.feedsta.models.Collection;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Predicate;
import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for the RecyclerView which shows all collections
 */
public class RecViewAdapterCollections extends RecyclerView.Adapter<RecViewAdapterCollections.ViewHolder> {

    public static ActionMode actionMode;
    private final ArrayList<Collection> selectedCollections = new ArrayList<>();
    private Context context;
    private boolean multiSelect = false;
    private OnItemClickListenerColl listener;
    private List<Collection> listCollectionsBookmarked;
    private ArrayList<Collection> listCollectionsToRemove;

    // initialize menu when long clicking on a collection
    private final ActionMode.Callback actionModeCallbacks = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            multiSelect = true;
            MenuInflater menuInflater = mode.getMenuInflater();
            menuInflater.inflate(R.menu.menu_contextual_actionbar_collection, menu);
            actionMode = mode;
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_remove_collection_bookmarks) {
                // get all checked collections in list and save to new list to remove items
                listCollectionsToRemove = new ArrayList<>();
                for (Collection collection : selectedCollections) {
                    if (collection.isChecked()) {
                        listCollectionsToRemove.add(collection);
                    }
                }

                // check if list is not empty
                if (!listCollectionsToRemove.isEmpty()) {
                    removeCollectionWithDialogFirst();
                }

                // exit contextual action menu
                mode.finish();
            } else if (itemId == R.id.menu_rename_category_bookmarks) {
                if (listener != null && !selectedCollections.isEmpty()) {
                    // temporary list with all selected collections
                    List<Collection> listTmpSelectedCollections = new ArrayList<>(selectedCollections);

                    // get all collections and see if collection "All" is selected
                    List<Collection> listWithoutAllCollection;
                    listWithoutAllCollection = Stream.of(listTmpSelectedCollections).filter(
                            new Predicate<Collection>() {
                                @Override
                                public boolean test(Collection c) {
                                    return (c.getName() != null) && c.getName().equals("All");
                                }
                            }).collect(Collectors.<Collection>toList());

                    if (listWithoutAllCollection != null && !listWithoutAllCollection.isEmpty()) {
                        if (context != null) {
                            Toast.makeText(context,
                                           context.getString(R.string.collection_all_cannot_be_renamed),
                                           Toast.LENGTH_SHORT).show();
                        }

                        // remove collection "All" from selectedCollections
                        listTmpSelectedCollections.removeAll(listWithoutAllCollection);
                    }

                    // check if there is more than one collection to be renamed -> cancel
                    if (listWithoutAllCollection != null && listWithoutAllCollection.size() > 1) {
                        if (context != null) {
                            Toast.makeText(context, context.getString(
                                    R.string.only_one_collection_can_renamed_at_time), Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }

                    // change category name
                    if (listTmpSelectedCollections.size() == 1) {
                        listener.renameCategory(listTmpSelectedCollections);
                    } else {
                        // more than one collection is selected
                        if (context != null) {
                            Toast.makeText(context, context.getString(
                                    R.string.only_one_collection_can_renamed_at_time), Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }
                }

                // exit contextual action menu
                mode.finish();
            } else if (itemId == R.id.menu_download_collection_bookmarks) {
                // download selected bookmarks
                if (listener != null && !selectedCollections.isEmpty()) {
                    listener.downloadSelectedCollections(selectedCollections);
                }

                // exit contextual action menu
                mode.finish();
            } else if (itemId == R.id.menu_remove_only_collection) {
                if (listener != null && !selectedCollections.isEmpty()) {
                    // temporary list with all selected collections
                    List<Collection> listTmpSelectedCollections = new ArrayList<>(selectedCollections);

                    // if "All" collection is selected -> cancel operation
                    // (will result in loss of all collections)
                    List<Collection> listWithAllCollection;
                    listWithAllCollection = Stream.of(listTmpSelectedCollections).filter(
                            new Predicate<Collection>() {
                                @Override
                                public boolean test(Collection c) {
                                    return (c.getName() != null) && c.getName().equals("All");
                                }
                            }).collect(Collectors.<Collection>toList());

                    if (listWithAllCollection != null && !listWithAllCollection.isEmpty()) {
                        if (context != null) {
                            Toast.makeText(context, context.getString(
                                    R.string.operation_cannot_be_applied_to_coll_all_cancelled),
                                           Toast.LENGTH_SHORT).show();
                        }

                        // exit contextual action menu because "All" is selected
                        mode.finish();
                        return true;
                    }

                    // change category name to null if "All" is not selected
                    if (listTmpSelectedCollections.size() >= 1 && listener != null) {
                        listener.resetCollectionInAllSelectedBookmarksOfCategory(listTmpSelectedCollections);
                    } else {
                        // more than one collection is selected
                        if (context != null) {
                            Toast.makeText(context, context.getString(R.string.select_at_least_one_coll),
                                           Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    if (context != null) {
                        Toast.makeText(context, context.getString(R.string.select_at_least_one_coll),
                                       Toast.LENGTH_SHORT).show();
                    }
                }

                // exit contextual action menu
                mode.finish();
            } else if (itemId == R.id.menu_select_all_bookmarks) {
                for (Collection collection : listCollectionsBookmarked) {
                    if (!collection.isChecked()) {
                        selectedCollections.add(collection);
                        collection.toggleChecked();
                    }
                }
                notifyDataSetChanged();
                // hint: no mode finish
            }
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            unselectAllItems();
            multiSelect = false;
            selectedCollections.clear();
            notifyDataSetChanged();
            actionMode = null;
        }
    };

    public RecViewAdapterCollections(List<Collection> listCollectionsBookmarked) {
        this.listCollectionsBookmarked = listCollectionsBookmarked;
    }

    public void setOnItemClickListener(OnItemClickListenerColl listener) {
        this.listener = listener;
    }

    /**
     * Unselects all collections
     */
    private void unselectAllItems() {
        if (multiSelect) {
            for (Collection collection : selectedCollections) {
                collection.toggleChecked();
            }
        }
    }

    /**
     * Shows a dialog when trying to remove selected collections
     */
    private void removeCollectionWithDialogFirst() {
        MaterialAlertDialogBuilder alertDialogBuilder;
        // create alertDialog
        alertDialogBuilder = new MaterialAlertDialogBuilder(context).setTitle(
                R.string.remove_collection_confirm_dialog_title).setMessage(
                R.string.remove_collection_confirm_dialog_message).setPositiveButton(
                context.getResources().getString(R.string.button_continue),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        //  Continue with remove operation
                        if (listener != null && listCollectionsToRemove != null &&
                            !listCollectionsToRemove.isEmpty()) {
                            listener.removeBookmarksOfCollectionFromStorage(listCollectionsToRemove);
                        }
                    }
                }).setNegativeButton(context.getResources().getString(R.string.CANCEL),
                                     new DialogInterface.OnClickListener() {
                                         @Override
                                         public void onClick(DialogInterface dialog, int which) {
                                             selectedCollections.clear();
                                             if (listCollectionsToRemove != null) {
                                                 listCollectionsToRemove.clear();
                                                 listCollectionsToRemove = null;
                                             }
                                         }
                                     })

                // get the click outside the dialog to set the behaviour like the negative button
                // was clicked
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        selectedCollections.clear();
                        if (listCollectionsToRemove != null) {
                            listCollectionsToRemove.clear();
                            listCollectionsToRemove = null;
                        }
                    }
                });

        final MaterialAlertDialogBuilder finalAlertDialogBuilder = alertDialogBuilder;

        // get color for button texts
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(R.attr.colorAccent, typedValue, true);
        @ColorInt final int color = typedValue.data;

        // create alertDialog
        AlertDialog alertDialog = finalAlertDialogBuilder.create();
        alertDialog.setCanceledOnTouchOutside(true);
        alertDialog.show();
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color);
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(color);
    }

    // create new views (invoked by the layout manager)
    @NonNull
    @Override
    public RecViewAdapterCollections.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        // create a new view
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.bookmarks_directory_item_grid, parent, false);
        context = parent.getContext();

        return new ViewHolder(view);
    }

    /**
     * Replaces the contents of a view (invoked by the layout manager)
     *
     * @param holder   ViewHolder
     * @param position int
     */
    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        Collection collection = listCollectionsBookmarked.get(position);
        holder.textViewDirName.setText(listCollectionsBookmarked.get(position).getName());

        // load base64 encoded image into view
        if (collection.getImageThumbnail() != null) {
            // load image into view
            Glide.with(context).asBitmap().load(Base64.decode(collection.getImageThumbnail(), Base64.DEFAULT))
                    .placeholder(ContextCompat.getDrawable(context, R.drawable.placeholder_image)).error(
                    ContextCompat.getDrawable(context, R.drawable.placeholder_image_error))
                    .dontAnimate().centerCrop().into(holder.imageViewDir);
        } else {
            // load image with url into view
            Glide.with(context).load(collection.getThumbnailUrl()).placeholder(
                    ContextCompat.getDrawable(context, R.drawable.placeholder_image)).error(
                    ContextCompat.getDrawable(context, R.drawable.placeholder_image_error)).dontAnimate()
                    .centerCrop().into(holder.imageViewDir);
        }
        holder.update(listCollectionsBookmarked.get(position));
    }

    @Override
    public int getItemCount() {
        return listCollectionsBookmarked.size();
    }

    public void setItems(List<Collection> listCollectionsBookmarked) {
        this.listCollectionsBookmarked = listCollectionsBookmarked;
    }

    // Provide a reference to the views for each data item. Complex data items may need more than
    // one view per item, and you provide access to all the views for a data item in a view holder
    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView textViewDirName;
        public ImageView imageViewDir;
        public View layout;
        FrameLayout frameLayout;

        public ViewHolder(View v) {
            super(v);
            layout = v;
            textViewDirName = v.findViewById(R.id.dir_name);
            imageViewDir = v.findViewById(R.id.dir_thumbnail);
            frameLayout = v.findViewById(R.id.frameLayoutImageViewCollections);
        }

        /**
         * Selects a collection and shows the "checked" icon in the upper right corner
         *
         * @param collection Collection
         */
        private void selectItem(Collection collection) {
            if (multiSelect) {
                if (selectedCollections.contains(collection)) {
                    selectedCollections.remove(collection);
                } else {
                    selectedCollections.add(collection);
                }

                collection.toggleChecked();
                showIconInCornerToSelectCollection(collection);
            }
        }

        /**
         * Updates the current collection
         *
         * @param collection Collection
         */
        private void update(final Collection collection) {
            showIconInCornerToSelectCollection(collection);

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    ((AppCompatActivity) view.getContext()).startSupportActionMode(actionModeCallbacks);
                    selectItem(collection);
                    return true;
                }
            });

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (multiSelect) {
                        selectItem(collection);
                    } else {
                        if (listener == null) return;
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onItemClick(position);
                        }
                    }
                }
            });
        }

        /**
         * Shows an icon in the corner of the thumbnail to mark collection as selected
         *
         * @param collection Collection
         */
        private void showIconInCornerToSelectCollection(Collection collection) {
            if (frameLayout == null) return;

            // when collection is checked show check mark
            if (collection.isChecked()) {

                // set checked icon
                frameLayout.setForeground(
                        ContextCompat.getDrawable(context, R.drawable.ic_baseline_check_circle_24dp));

                // top right corner
                frameLayout.setForegroundGravity(Gravity.END | Gravity.TOP);
            } else {
                // image overlay thumbnail (no overlay)
                frameLayout.setForeground(null);
            }
        }
    }
}
