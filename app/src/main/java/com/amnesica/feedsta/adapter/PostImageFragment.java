package com.amnesica.feedsta.adapter;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.amnesica.feedsta.R;
import com.amnesica.feedsta.fragments.FullscreenImagePostFragment;
import com.amnesica.feedsta.helper.FragmentHelper;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

/**
 * Displays an image in a sidecar post
 */
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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_viewpager_image_post, container, false);
        ImageView imagePost = view.findViewById(R.id.imagePost);

        // get imageUrl from arguments
        String imageUrl = null;
        if (getArguments() != null) {
            imageUrl = getArguments().getString("imageUrl");
        }

        // load image in imageView but don't cache or store it
        Glide.with(view).load(imageUrl).error(R.drawable.placeholder_image_post_error).centerInside()
                .diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).dontAnimate().into(
                imagePost);

        // copy imageUrl to temp final variable
        final String finalImageUrl = imageUrl;

        // on click listener to show post image fullscreen to zoom into image
        imagePost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // start fullscreen image post fragment
                FullscreenImagePostFragment fullscreenImagePostFragment =
                        FullscreenImagePostFragment.newInstance(finalImageUrl);

                // add fullscreenImagePostFragment to FragmentManager
                FragmentHelper.addFragmentToContainer(fullscreenImagePostFragment,
                                                      requireActivity().getSupportFragmentManager());
            }
        });
        return view;
    }
}
