package com.amnesica.feedsta.adapter;

import android.content.Context;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.amnesica.feedsta.R;
import com.amnesica.feedsta.interfaces.OnItemClickListenerSelectColl;
import com.amnesica.feedsta.models.Collection;
import com.bumptech.glide.Glide;

import java.util.List;

/**
 * Adapter for the RecyclerView which shows all collections except "All" in the
 * BtmSheetDialogSelectCollection
 */
public class RecViewAdapterSelectCollection
        extends RecyclerView.Adapter<RecViewAdapterSelectCollection.ViewHolder> {

    private List<Collection> listCollectionsBookmarked;
    private Context context;
    private OnItemClickListenerSelectColl listener;

    public RecViewAdapterSelectCollection(List<Collection> listCollectionsBookmarked) {
        this.listCollectionsBookmarked = listCollectionsBookmarked;
    }

    public void setOnItemClickListener(OnItemClickListenerSelectColl listener) {
        this.listener = listener;
    }

    // create new views (invoked by the layout manager)
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        // create a new view
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.bookmarks_directory_item_grid, parent, false);
        context = parent.getContext();

        // set the view's size, margins, paddings and layout parameters
        return new ViewHolder(view, listener);
    }

    // replace the contents of a view (invoked by the layout manager)
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

        public ViewHolder(View view, final OnItemClickListenerSelectColl listener) {
            super(view);
            layout = view;
            textViewDirName = view.findViewById(R.id.dir_name);
            imageViewDir = view.findViewById(R.id.dir_thumbnail);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener == null) return;
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onItemClick(position);
                    }
                }
            });
        }

        private void update(final Collection collection) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener == null) return;
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onItemClick(position);
                    }
                }
            });
        }
    }
}
