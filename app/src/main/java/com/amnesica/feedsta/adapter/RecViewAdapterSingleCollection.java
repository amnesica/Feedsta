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
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.amnesica.feedsta.R;
import com.amnesica.feedsta.interfaces.OnItemClickListSingleColl;
import com.amnesica.feedsta.models.Post;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Predicate;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for the RecyclerView which shows the bookmarks of a single collection
 */
public class RecViewAdapterSingleCollection
        extends RecyclerView.Adapter<RecViewAdapterSingleCollection.ViewHolder> {

    public static ActionMode actionMode;
    private final ArrayList<Post> selectedPosts = new ArrayList<>();
    private Context context;
    private boolean multiSelect = false;
    private OnItemClickListSingleColl listener;
    private List<Post> listPostsInCollectionBookmarked;
    private ArrayList<Post> listPostsToRemove;

    // initialize menu when long clicking on a bookmark
    private final ActionMode.Callback actionModeCallbacks = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            multiSelect = true;
            MenuInflater menuInflater = mode.getMenuInflater();
            menuInflater.inflate(R.menu.menu_contextual_actionbar_single_collection, menu);
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
            if (itemId == R.id.menu_remove_multiple_bookmarks) {
                // get all checked posts in list and save to new list to remove items
                listPostsToRemove = new ArrayList<>();
                for (Post post : selectedPosts) {
                    if (post.isChecked()) {
                        listPostsToRemove.add(post);
                    }
                }

                removeBookmarksWithDialogFirst();

                // exit contextual action menu
                mode.finish();
            } else if (itemId == R.id.menu_change_category_bookmarks) {
                if (listener != null && !selectedPosts.isEmpty()) {
                    listener.moveBookmarksToOtherCollection(selectedPosts);
                }

                // exit contextual action menu
                mode.finish();
            } else if (itemId == R.id.menu_download_bookmarks) {
                if (listener != null && !selectedPosts.isEmpty()) {
                    listener.downloadSelectedBookmarks(selectedPosts);
                }

                // exit contextual action menu
                mode.finish();
            } else if (itemId == R.id.menu_reset_category_bookmarks) {
                // temporary list with all selected Posts
                List<Post> listTmpSelectedPosts = new ArrayList<>(selectedPosts);

                // if bookmarks in 'All'-collection are selected -> cancel operation
                List<Post> listWithPostsInAllCollection;
                listWithPostsInAllCollection = Stream.of(listTmpSelectedPosts).filter(new Predicate<Post>() {
                    @Override
                    public boolean test(Post p) {
                        return (p != null) && p.getCategory() == null;
                    }
                }).collect(Collectors.<Post>toList());

                if (listWithPostsInAllCollection != null && !listWithPostsInAllCollection.isEmpty()) {
                    if (context != null) {
                        Toast.makeText(context, context.getString(
                                R.string.operation_can_only_be_applied_to_bookmarks_with_coll),
                                       Toast.LENGTH_SHORT).show();
                    }

                    // exit contextual action menu because 'All' is selected
                    mode.finish();
                    return true;
                }

                if (listener != null && !selectedPosts.isEmpty()) {
                    listener.resetCategoryOnSelectedBookmarks(selectedPosts);
                }

                // exit contextual action menu
                mode.finish();
            } else if (itemId == R.id.menu_select_all_bookmarks) {
                for (Post post : listPostsInCollectionBookmarked) {
                    if (!post.isChecked()) {
                        selectedPosts.add(post);
                        post.toggleChecked();
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
            selectedPosts.clear();
            notifyDataSetChanged();
            actionMode = null;
        }
    };

    // provide a suitable constructor
    public RecViewAdapterSingleCollection(List<Post> listPostsInCollectionBookmarked) {
        this.listPostsInCollectionBookmarked = listPostsInCollectionBookmarked;
    }

    public void setOnItemClickListener(OnItemClickListSingleColl listener) {
        this.listener = listener;
    }

    // create new views (invoked by the layout manager)
    @NonNull
    @Override
    public RecViewAdapterSingleCollection.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        // create a new view
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.gridview_item_image, parent, false);
        context = parent.getContext();

        return new ViewHolder(view);
    }

    // replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        final Post post = listPostsInCollectionBookmarked.get(position);

        // load base64 encoded image into view
        if (post.getImageThumbnail() != null) {
            // load image into view
            Glide.with(context).asBitmap().load(Base64.decode(post.getImageThumbnail(), Base64.DEFAULT))
                    .placeholder(R.drawable.placeholder_image).error(R.drawable.placeholder_image_post_error)
                    .dontAnimate().centerCrop().into(holder.imageView);
        } else {
            // load image with url into view
            Glide.with(context).load(post.getImageUrlThumbnail()).placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image_post_error).dontAnimate().centerCrop().into(
                    holder.imageView);
        }
        holder.update(listPostsInCollectionBookmarked.get(position));
    }

    @Override
    public int getItemCount() {
        return listPostsInCollectionBookmarked.size();
    }

    public void setItems(List<Post> listPostsInCollectionBookmarked) {
        this.listPostsInCollectionBookmarked = listPostsInCollectionBookmarked;
    }

    /**
     * Unselects all collections
     */
    private void unselectAllItems() {
        if (multiSelect) {
            for (Post post : selectedPosts) {
                post.toggleChecked();
            }
        }
    }

    /**
     * Dialog confirm remove bookmarks after using contextual menu
     */
    private void removeBookmarksWithDialogFirst() {
        AlertDialog.Builder alertDialogBuilder;
        // create alertDialog
        alertDialogBuilder = new AlertDialog.Builder(context).setTitle(
                R.string.remove_bookmarks_confirm_dialog_title).setMessage(
                R.string.remove_bookmarks_confirm_dialog_message).setPositiveButton(
                context.getResources().getString(R.string.button_continue),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //  Continue with remove operation
                        if (listener != null && listPostsToRemove != null && !listPostsToRemove.isEmpty()) {
                            listener.removeBookmarkedPostsFromStorage(listPostsToRemove);
                        }
                    }
                }).setNegativeButton(context.getResources().getString(R.string.CANCEL),
                                     new DialogInterface.OnClickListener() {
                                         @Override
                                         public void onClick(DialogInterface dialog, int which) {
                                             selectedPosts.clear();
                                             if (listPostsToRemove != null) {
                                                 listPostsToRemove.clear();
                                                 listPostsToRemove = null;
                                             }
                                         }
                                     })

                // get the click outside the dialog to set the behaviour like the negative button
                // was clicked
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        selectedPosts.clear();
                        if (listPostsToRemove != null) {
                            listPostsToRemove.clear();
                            listPostsToRemove = null;
                        }
                    }
                });

        final AlertDialog.Builder finalAlertDialogBuilder = alertDialogBuilder;

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

    // Provide a reference to the views for each data item. Complex data items may need more than
    // one view per item, and you provide access to all the views for a data item in a view holder
    public class ViewHolder extends RecyclerView.ViewHolder {

        public ImageView imageView;
        public View view;
        FrameLayout frameLayout;

        public ViewHolder(View v) {
            super(v);
            view = v;
            imageView = v.findViewById(R.id.gridView_item_image);
            frameLayout = v.findViewById(R.id.frameLayoutImageView);
        }

        /**
         * Selects a bookmark and shows the "checked" icon in the upper right corner
         *
         * @param post Post to be selected
         */
        private void selectItem(Post post) {
            if (multiSelect) {
                if (selectedPosts.contains(post)) {
                    selectedPosts.remove(post);
                } else {
                    selectedPosts.add(post);
                }
                post.toggleChecked();
                showIconToBookmarkInCorner(post);
            }
        }

        /**
         * Updates the current post
         *
         * @param post Post
         */
        private void update(final Post post) {
            showIconToBookmarkInCorner(post);

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    ((AppCompatActivity) view.getContext()).startSupportActionMode(actionModeCallbacks);
                    selectItem(post);
                    return true;
                }
            });

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (multiSelect) {
                        selectItem(post);
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
         * Shows an icon in the top right corner to mark the bookmark as selected or to indicate that bookmark
         * is a video or a sidecar post
         *
         * @param post Post
         */
        private void showIconToBookmarkInCorner(Post post) {
            // set image overlay for sidecar and video posts
            if (frameLayout != null) {

                // when bookmark is checked show check mark otherwise show image or sidecar overlay
                if (post.isChecked()) {
                    // set checked icon
                    frameLayout.setForeground(
                            ContextCompat.getDrawable(context, R.drawable.ic_baseline_check_circle_white_24));

                    // top right corner
                    frameLayout.setForegroundGravity(Gravity.END | Gravity.TOP);
                } else {
                    if (post.getIs_sideCar()) {
                        // sidecar overlay thumbnail
                        frameLayout.setForeground(
                                ContextCompat.getDrawable(context, R.drawable.ic_sidecar_black_24dp));

                        // top right corner
                        frameLayout.setForegroundGravity(Gravity.END | Gravity.TOP);
                    } else if (post.getIs_video()) {
                        // video overlay thumbnail
                        frameLayout.setForeground(ContextCompat.getDrawable(context,
                                                                            R.drawable.ic_play_circle_outline_black_24dp));

                        // top right corner
                        frameLayout.setForegroundGravity(Gravity.END | Gravity.TOP);
                    } else {
                        // image overlay thumbnail (no overlay)
                        frameLayout.setForeground(null);
                    }
                }
            }
        }
    }
}
