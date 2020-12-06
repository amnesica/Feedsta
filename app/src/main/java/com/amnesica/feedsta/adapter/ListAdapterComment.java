package com.amnesica.feedsta.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.amnesica.feedsta.R;
import com.amnesica.feedsta.models.Comment;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.text.DateFormat;
import java.util.ArrayList;

import static android.view.View.GONE;

/**
 * Adapter for displaying comments in PostFragment
 */
@SuppressWarnings("NullableProblems")
public class ListAdapterComment extends ArrayAdapter<Comment> {

    public ListAdapterComment(Context context, int resource, ArrayList<Comment> comments) {
        super(context, resource, comments);
    }

    @SuppressLint("InflateParams")
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            assert layoutInflater != null;
            convertView = layoutInflater.inflate(R.layout.list_item_comment, null, true);
        }

        try {
            // get comment at current position
            final Comment comment = getItem(position);

            if (comment != null) {
                // find layout views
                ImageView imageView = convertView.findViewById(R.id.accountProfilePic);
                final TextView username = convertView.findViewById(R.id.textUsername);
                TextView textComment = convertView.findViewById(R.id.textTextComment);
                TextView textDate = convertView.findViewById(R.id.textDate);

                // set profile picture
                if (comment.getOwnerProfilePicUrl() != null) {
                    Glide.with(convertView)
                            .load(comment.getOwnerProfilePicUrl())
                            .error(R.drawable.placeholder_image_post_error)
                            .dontAnimate()
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .into(imageView);
                } else {
                    imageView.setVisibility(GONE);
                }

                // set username
                if (comment.getUsername() != null) {
                    username.setText(comment.getUsername());
                } else {
                    username.setVisibility(GONE);
                }

                // set comment
                if (comment.getText() != null) {
                    textComment.setText(comment.getText());
                } else {
                    textComment.setVisibility(GONE);
                }

                // set date
                if (comment.getCreated_at() != null) {
                    textDate.setText(DateFormat.getDateTimeInstance().format(comment.getCreated_at()));
                } else {
                    textDate.setVisibility(GONE);
                }
            }
        } catch (Exception e) {
            Log.d("ListAdapterComment", Log.getStackTraceString(e));
        }

        return convertView;
    }
}
