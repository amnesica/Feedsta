package com.amnesica.feedsta.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.amnesica.feedsta.R;
import com.amnesica.feedsta.interfaces.OnItemClickListenerSelectColl;
import com.amnesica.feedsta.models.Collection;
import com.bumptech.glide.Glide;

import java.util.List;

/**
 * Adapter for the RecyclerView which shows all collections except
 * "All" in the BtmSheetDialogSelectCollection
 */
public class RecViewAdapterSelectCollection extends RecyclerView.Adapter<RecViewAdapterSelectCollection.ViewHolder> {

    private List<Collection> listCollectionsBookmarked;
    private Context context;
    private OnItemClickListenerSelectColl listener;

    // provide a suitable constructor
    public RecViewAdapterSelectCollection(List<Collection> listCollectionsBookmarked) {
        this.listCollectionsBookmarked = listCollectionsBookmarked;
    }

    public void setOnItemClickListener(OnItemClickListenerSelectColl listener) {
        this.listener = listener;
    }

    // create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        // create a new view
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(R.layout.bookmarks_directory_item_grid, parent, false);
        context = parent.getContext();

        // set the view's size, margins, paddings and layout parameters
        return new ViewHolder(v, listener);
    }

    // replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        String url = listCollectionsBookmarked.get(position).getThumbnailUrl();
        holder.textViewDirName.setText(listCollectionsBookmarked.get(position).getName());

        // load image into view
        Glide.with(context)
                .load(url)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image_post_error)
                .dontAnimate()
                .centerCrop()
                .into(holder.imageViewDir);

        holder.update(listCollectionsBookmarked.get(position));
    }

    @Override
    public int getItemCount() {
        return listCollectionsBookmarked.size();
    }

    public void setItems(List<Collection> listCollectionsBookmarked) {
        this.listCollectionsBookmarked = listCollectionsBookmarked;
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView textViewDirName;
        public ImageView imageViewDir;
        public View layout;

        public ViewHolder(View v, final OnItemClickListenerSelectColl listener) {
            super(v);
            layout = v;
            textViewDirName = v.findViewById(R.id.dir_name);
            imageViewDir = v.findViewById(R.id.dir_thumbnail);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onItemClick(position);
                        }
                    }
                }
            });
        }

        private void update(final Collection collection) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onItemClick(position);
                        }
                    }
                }
            });
        }
    }
}
