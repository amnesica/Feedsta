package com.amnesica.feedsta.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.fragment.app.Fragment;

import com.amnesica.feedsta.R;
import com.bumptech.glide.Glide;

/**
 * Fragment for displaying an profile image fullscreen
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_fullscreen_image, container, false);

        // to retrieve object in fragment
        String profileImageUrl = null;
        if (getArguments() != null) {
            profileImageUrl = (String) getArguments().getSerializable("ImageUrl");
        }

        // get image view
        final ImageView imageProfilePicFullscreen = v.findViewById(R.id.imageProfilePicFullscreen);

        // load image with glide
        Glide.with(this)
                .load(profileImageUrl)
                .error(R.drawable.placeholder_image_post_error)
                .dontAnimate()
                .into(imageProfilePicFullscreen);

        // set onClickListener for closing fragment
        imageProfilePicFullscreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // close fragment
                requireActivity().onBackPressed();
            }
        });

        return v;
    }
}