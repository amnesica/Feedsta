package com.amnesica.feedsta.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.amnesica.feedsta.R;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.github.piasy.biv.BigImageViewer;
import com.github.piasy.biv.loader.ImageLoader;
import com.github.piasy.biv.loader.glide.GlideImageLoader;
import com.github.piasy.biv.view.BigImageView;

import java.io.File;

/**
 * Fragment to show an image of a post fullscreen (used in PostFragment and PostImageFragment)
 */
public class FullscreenImagePostFragment extends Fragment {

    public static FullscreenImagePostFragment newInstance(String imageUrl) {
        Bundle args = new Bundle();
        args.putString("ImageUrl", imageUrl);
        FullscreenImagePostFragment fragment = new FullscreenImagePostFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // load image with glide
        BigImageViewer.initialize(GlideImageLoader.with(requireContext()));

        // inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_fullscreen_post_image, container, false);

        // to retrieve object in fragment
        String imageUrl = null;
        if (getArguments() != null) {
            imageUrl = (String) getArguments().getSerializable("ImageUrl");
        }

        // get image view
        final BigImageView imagePostFullscreen = view.findViewById(R.id.imagePostFullscreen);

        // show image
        imagePostFullscreen.showImage(Uri.parse(imageUrl));

        // Set minimum dpi to zoom very deep
        // source: https://github.com/Piasy/BigImageViewer/issues/84
        imagePostFullscreen.setImageLoaderCallback(new ImageLoader.Callback() {
            @Override
            public void onCacheHit(int imageType, File image) {
            }

            @Override
            public void onCacheMiss(int imageType, File image) {
            }

            @Override
            public void onStart() {
            }

            @Override
            public void onProgress(int progress) {
            }

            @Override
            public void onFinish() {
            }

            @Override
            public void onSuccess(File image) {
                final SubsamplingScaleImageView view = imagePostFullscreen.getSSIV();

                if (view != null) {
                    view.setMinimumDpi(30);
                    view.setMinScale(0f);
                }
            }

            @Override
            public void onFail(Exception error) {
            }
        });

        return view;
    }
}
