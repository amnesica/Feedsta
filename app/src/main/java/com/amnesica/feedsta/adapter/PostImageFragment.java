package com.amnesica.feedsta.adapter;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.amnesica.feedsta.R;
import com.amnesica.feedsta.helper.ImageHelper;

/** Displays an image in a sidecar post */
public class PostImageFragment extends Fragment {

  static Fragment newInstance(String imageUrl) {
    PostImageFragment f = new PostImageFragment();
    Bundle b = new Bundle();

    // put imageUrl as argument
    b.putString("imageUrl", imageUrl);
    f.setArguments(b);
    return f;
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_viewpager_image_post, container, false);
    ImageView imageView = view.findViewById(R.id.imagePost);

    // get imageUrl from arguments
    String imageUrl = null;
    if (getArguments() != null) {
      imageUrl = getArguments().getString("imageUrl");
    }

    // load image in imageView but don't cache or store it
    ImageHelper.loadImageWithGlide(
        imageView, imageUrl, R.drawable.placeholder_image_error, requireContext());

    // on click listener to show post image fullscreen to zoom into image
    ImageHelper.setupListenerToShowFullscreenPostImage(
        (AppCompatActivity) requireContext(), imageUrl, imageView);

    return view;
  }
}
