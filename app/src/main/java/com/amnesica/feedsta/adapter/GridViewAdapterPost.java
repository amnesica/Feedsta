package com.amnesica.feedsta.adapter;

import android.annotation.SuppressLint;
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

import com.amnesica.feedsta.Post;
import com.amnesica.feedsta.R;
import com.bumptech.glide.Glide;

import java.util.ArrayList;

/**
 * Adapter for displaying posts in a gridView layout
 */
@SuppressWarnings({"CanBeFinal", "NullableProblems"})
public class GridViewAdapterPost extends ArrayAdapter<Post> {
    private final Context context;
    private final int resource;
    private final ArrayList<Post> posts;

    public GridViewAdapterPost(Context context, int resource, ArrayList<Post> posts) {
        super(context, resource, posts);
        this.context = context;
        this.resource = resource;
        this.posts = posts;
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        final ViewHolder holder;

        if (row == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            assert inflater != null;
            row = inflater.inflate(resource, parent, false);
            holder = new ViewHolder();
            holder.item_imageView = row.findViewById(R.id.gridView_item_image);
            row.setTag(holder);
        } else {
            holder = (ViewHolder) row.getTag();
        }

        if (getItem(position) != null) {
            try {
                // get the post for the current position.
                Post post = getItem(position);

                FrameLayout frameLayout = row.findViewById(R.id.frameLayoutImageView);

                assert post != null;

                // set image overlay for sidecar and video posts
                if (frameLayout != null) {
                    // when post is checked show check mark otherwise show image or sidecar overlay
                    if (post.isChecked()) {
                        // set checked icon
                        frameLayout.setForeground(context.getDrawable(R.drawable.ic_baseline_check_circle_white_24));

                        // top right corner
                        frameLayout.setForegroundGravity(Gravity.END | Gravity.TOP);
                    } else {
                        if (post.getIs_sideCar()) {
                            // sidecar overlay thumbnail
                            frameLayout.setForeground(context.getDrawable(R.drawable.ic_sidecar_black_24dp));

                            // top right corner
                            frameLayout.setForegroundGravity(Gravity.END | Gravity.TOP);
                        } else if (post.getIs_video()) {
                            // video overlay thumbnail
                            frameLayout.setForeground(context.getDrawable(R.drawable.ic_play_circle_outline_black_24dp));

                            // top right corner
                            frameLayout.setForegroundGravity(Gravity.END | Gravity.TOP);
                        } else {
                            // image overlay thumbnail (no overlay)
                            frameLayout.setForeground(null);
                        }
                    }
                }

                // load image into view
                Glide.with(row)
                        .load(post.getImageUrlThumbnail())
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.placeholder_image_post_error)
                        .dontAnimate()
                        .centerCrop()
                        .into(holder.item_imageView);
            } catch (NullPointerException e) {
                Log.d("GridViewAdapterPost", Log.getStackTraceString(e));
            }
        }
        return row;
    }

    @Override
    public int getCount() {
        if (posts != null) {
            return posts.size();
        } else {
            return 0;
        }
    }

    @Override
    public Post getItem(int position) {
        return posts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    static class ViewHolder {
        ImageView item_imageView;
    }
}