package com.amnesica.feedsta.adapter;

import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.amnesica.feedsta.models.Post;
import com.amnesica.feedsta.models.Sidecar;
import com.amnesica.feedsta.models.SidecarEntryType;

/** Adapter for displaying a sidecar with multiple images and/or videos */
public class StatePagerAdapterSideCar extends FragmentStateAdapter {

  private final Sidecar sidecar;
  private final SparseArray<Fragment> registeredFragments = new SparseArray<>();

  public StatePagerAdapterSideCar(@NonNull FragmentActivity fragmentActivity, Post post) {
    super(fragmentActivity);
    this.sidecar = post.getSidecar();
  }

  /** Finish all video players in registered fragments */
  public void finishAllRegisteredFragments() {
    for (int i = 0; i < registeredFragments.size(); i++) {
      if (registeredFragments.valueAt(i) instanceof PostVideoFragment) {
        // if fragment is videoPostFragment call releasePlayer
        ((PostVideoFragment) registeredFragments.valueAt(i)).releasePlayer();
      }
    }
  }

  @NonNull
  @Override
  public Fragment createFragment(int position) {
    // if post is image
    Fragment fragmentToShow = null;
    if (sidecar != null
        && sidecar
            .getSidecarEntries()
            .get(position)
            .getSidecarEntryType()
            .equals(SidecarEntryType.image)) {
      // get image url
      String imageUrl = sidecar.getSidecarEntries().get(position).getUrl();

      // start new image fragment
      fragmentToShow = PostImageFragment.newInstance(imageUrl);
    } else {
      // post is video
      if (sidecar != null) {
        // get video url
        String videoUrl = sidecar.getSidecarEntries().get(position).getUrl();

        // start new video fragment
        fragmentToShow = PostVideoFragment.newInstance(videoUrl);

        registeredFragments.put(position, fragmentToShow);
      }
    }
    assert fragmentToShow != null;
    return fragmentToShow;
  }

  @Override
  public int getItemCount() {
    int getCount = 0;
    try {
      getCount = sidecar.getSidecarEntries().size();
    } catch (NullPointerException e) {
      Log.d("StatePagerAdapterSideCar", Log.getStackTraceString(e));
    }
    return getCount;
  }
}
