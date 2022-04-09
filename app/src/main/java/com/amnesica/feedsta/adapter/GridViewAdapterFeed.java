package com.amnesica.feedsta.adapter;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import com.amnesica.feedsta.R;
import com.amnesica.feedsta.models.Post;
import com.bumptech.glide.Glide;

import java.util.ArrayList;

/**
 * Adapter for displaying the feed with posts in a gridView layout (used in FeedFragment)
 */
public class GridViewAdapterFeed extends ArrayAdapter<Post> {

    private final ArrayList<Post> posts;
    private final Context context;
    private final int resource;

    public GridViewAdapterFeed(final Context context, int resource, ArrayList<Post> posts) {
        super(context, resource, posts);
        this.context = context;
        this.posts = posts;
        this.resource = resource;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        final ViewHolder holder;
        if (row == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(
                    Activity.LAYOUT_INFLATER_SERVICE);
            assert inflater != null;
            row = inflater.inflate(resource, parent, false);
            holder = new ViewHolder();
            holder.item_imageView = row.findViewById(R.id.gridView_item_image);
            row.setTag(holder);
        } else {
            holder = (ViewHolder) row.getTag();
        }

        try {
            // get the post at the current position
            Post post = getItem(position);
            assert post != null;

            FrameLayout frameLayout = row.findViewById(R.id.frameLayoutImageView);

            // set image overlay for sidecar and video posts
            if (frameLayout != null) {
                if (post.getIs_sideCar()) {
                    // sidecar overlay thumbnail
                    frameLayout.setForeground(ContextCompat.getDrawable(context, R.drawable.ic_sidecar_24dp));

                    // top right corner
                    frameLayout.setForegroundGravity(Gravity.END | Gravity.TOP);
                } else if (post.getIs_video()) {
                    // video overlay thumbnail
                    frameLayout.setForeground(
                            ContextCompat.getDrawable(context, R.drawable.ic_play_circle_outline_24dp));

                    // top right corner
                    frameLayout.setForegroundGravity(Gravity.END | Gravity.TOP);
                } else {
                    // image overlay thumbnail (no overlay)
                    frameLayout.setForeground(null);
                }
            }

            // load image into view
            Glide.with(row).load(post.getImageUrlThumbnail()).placeholder(R.drawable.placeholder_image).error(
                    R.drawable.placeholder_image_post_error).centerCrop().dontAnimate().into(
                    holder.item_imageView);
        } catch (Exception e) {
            Log.d("GridViewAdapterFeed", Log.getStackTraceString(e));
        }
        return row;
    }

    @Override
    public int getCount() {
        if (posts == null) return 0;
        return posts.size();
    }

    @Override
    public Post getItem(int position) {
        // hint: fix OutOfBoundsException
        try {
            return posts.get(position);
        } catch (Exception e) {
            Log.d("GridViewAdapterFeed", Log.getStackTraceString(e));
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    static class ViewHolder {
        ImageView item_imageView;
    }
}
