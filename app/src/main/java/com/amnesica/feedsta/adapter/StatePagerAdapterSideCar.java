package com.amnesica.feedsta.adapter;

import android.annotation.SuppressLint;
import android.util.Log;
import android.util.SparseArray;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.amnesica.feedsta.Post;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

/**
 * Adapter for displaying a sideCar with multiple images and/or videos
 */
@SuppressWarnings({"CanBeFinal"})
public class StatePagerAdapterSideCar extends FragmentStatePagerAdapter {

    private final HashMap<Integer, ArrayList<String>> sidecarUrls;
    private final SparseArray<Fragment> registeredFragments = new SparseArray<>();

    public StatePagerAdapterSideCar(@NonNull FragmentManager fm, int behavior, Post post) {
        super(fm, behavior);
        this.sidecarUrls = post.getSidecarUrls();
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        // if post is image
        Fragment fragmentToShow = null;
        if (sidecarUrls != null && Objects.requireNonNull(sidecarUrls.get(position)).get(0).equals("image")) {
            // get image url
            String imageUrl = Objects.requireNonNull(sidecarUrls.get(position)).get(1);

            // start new image fragment
            fragmentToShow = PostImageFragment.newInstance(imageUrl);
        } else {
            // post is video
            if (sidecarUrls != null) {
                // get video url
                String videoUrl = Objects.requireNonNull(sidecarUrls.get(position)).get(1);

                // start new video fragment
                fragmentToShow = PostVideoFragment.newInstance(videoUrl);
            }
        }
        assert fragmentToShow != null;
        return fragmentToShow;
    }

    @SuppressLint("LongLogTag")
    @Override
    public int getCount() {
        int getCount = 0;
        try {
            getCount = sidecarUrls.size();
        } catch (NullPointerException e) {
            Log.d("StatePagerAdapterSideCar", Log.getStackTraceString(e));
        }
        return getCount;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Fragment fragment = (Fragment) super.instantiateItem(container, position);
        registeredFragments.put(position, fragment);
        return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        registeredFragments.remove(position);
        super.destroyItem(container, position, object);
    }

    /**
     * Finish all video players in registered fragments
     */
    public void finishAllRegisteredFragments() {
        for (int i = 0; i < registeredFragments.size(); i++) {
            // if fragment is videoPostFragment
            if (registeredFragments.valueAt(i) instanceof PostVideoFragment) {
                // call releasePlayer inside videoPostFragment
                ((PostVideoFragment) registeredFragments.valueAt(i)).releasePlayer();
            }
        }
    }
}
