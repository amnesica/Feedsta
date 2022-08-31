package com.amnesica.feedsta.fragments.fullscreenimages;

import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.amnesica.feedsta.R;
import com.bumptech.glide.Glide;

/**
 * Fragment for displaying an profile image fullscreen (used in ListAdapterSearch, HashtagFragment,
 * ProfileFragment)
 */
public class FullscreenProfileImageFragment extends Fragment {

  public static FullscreenProfileImageFragment newInstance(String imageUrl) {
    Bundle args = new Bundle();
    args.putString("ImageUrl", imageUrl);
    FullscreenProfileImageFragment fragment = new FullscreenProfileImageFragment();
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    // inflate the layout for this fragment
    View view = inflater.inflate(R.layout.fragment_fullscreen_image, container, false);

    // to retrieve object in fragment
    String profileImageUrl = null;
    if (getArguments() != null) {
      profileImageUrl = (String) getArguments().getSerializable("ImageUrl");
    }

    // get image view
    final ImageView imageProfilePicFullscreen = view.findViewById(R.id.imageProfilePicFullscreen);

    // load image with url or from base64 encoded string glide
    assert profileImageUrl != null;
    if (!profileImageUrl.startsWith("https://")) {
      // load image into view
      Glide.with(this)
          .asBitmap()
          .load(Base64.decode(profileImageUrl, Base64.DEFAULT))
          .error(ContextCompat.getDrawable(requireContext(), R.drawable.placeholder_image_error))
          .dontAnimate()
          .into(imageProfilePicFullscreen);
    } else {
      // load image with url into view (url starts with "https://")
      Glide.with(this)
          .load(profileImageUrl)
          .error(ContextCompat.getDrawable(requireContext(), R.drawable.placeholder_image_error))
          .dontAnimate()
          .into(imageProfilePicFullscreen);
    }

    // set onClickListener for closing fragment
    imageProfilePicFullscreen.setOnClickListener(
        view1 -> {
          // close fragment
          requireActivity().onBackPressed();
        });

    return view;
  }
}
