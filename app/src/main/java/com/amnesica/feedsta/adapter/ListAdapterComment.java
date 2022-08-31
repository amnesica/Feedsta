package com.amnesica.feedsta.adapter;

import static android.view.View.GONE;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.amnesica.feedsta.R;
import com.amnesica.feedsta.helper.FragmentHelper;
import com.amnesica.feedsta.models.Comment;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.text.DateFormat;
import java.util.ArrayList;

/** Adapter for displaying comments in PostFragment */
public class ListAdapterComment extends ArrayAdapter<Comment> {

  final Fragment fragment;

  public ListAdapterComment(
      Context context, int resource, ArrayList<Comment> comments, Fragment fragment) {
    super(context, resource, comments);
    this.fragment = fragment;
  }

  public View getView(int position, View convertView, ViewGroup parent) {

    if (convertView == null) {
      LayoutInflater layoutInflater =
          (LayoutInflater) getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
      assert layoutInflater != null;
      convertView = layoutInflater.inflate(R.layout.list_item_comment, null, true);
    }

    try {
      // get comment at current position
      final Comment comment = getItem(position);

      if (comment != null) {
        // find layout views
        final ImageView imageView = convertView.findViewById(R.id.accountProfilePic);
        final TextView username = convertView.findViewById(R.id.textUsername);
        final TextView textComment = convertView.findViewById(R.id.textTextComment);
        final TextView textDate = convertView.findViewById(R.id.textDate);

        // set profile picture of author/owner
        if (comment.getOwnerProfilePicUrl() != null) {
          Glide.with(convertView)
              .load(comment.getOwnerProfilePicUrl())
              .error(ContextCompat.getDrawable(getContext(), R.drawable.placeholder_image_error))
              .dontAnimate()
              .diskCacheStrategy(DiskCacheStrategy.NONE)
              .skipMemoryCache(true)
              .into(imageView);
        } else {
          imageView.setVisibility(GONE);
        }

        // set username of author/owner
        if (comment.getUsername() != null) {
          username.setText(comment.getUsername());
        } else {
          username.setVisibility(GONE);
        }

        // set comment/text
        if (comment.getText() != null) {
          // make links clickable if necessary
          textComment.setText(
              FragmentHelper.createSpannableStringWithClickableLinks(comment.getText(), fragment));
        } else {
          textComment.setVisibility(GONE);
        }

        // set date of comment
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
