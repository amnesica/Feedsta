package com.amnesica.feedsta.asynctasks.download;

import android.widget.ImageButton;

import androidx.fragment.app.Fragment;

import com.amnesica.feedsta.models.Post;
import com.amnesica.feedsta.models.SidecarEntryType;

public class DownloadMedia {

  private final Post post;
  private final ImageButton downloadButton;
  private final Fragment callingFragment;
  private final Integer viewPagerPosition;

  public DownloadMedia(
      Post post, ImageButton downloadButton, Fragment callingFragment, Integer viewPagerPosition) {
    this.post = post;
    this.downloadButton = downloadButton;
    this.callingFragment = callingFragment;
    this.viewPagerPosition = viewPagerPosition;
  }

  public void startDownloadMedia() {
    if (postIsSingleImage()) {
      new DownloadSingleImage()
          .startDownloadImage(
              callingFragment, post.getImageUrl(), post.getUsername(), downloadButton);
    } else if (postIsSingleVideo()) {
      new DownloadSingleVideo()
          .startDownloadVideo(
              callingFragment, post.getVideoUrl(), post.getUsername(), downloadButton);
    } else if (postIsSidecar()) {
      // image
      String photoUrl = post.getSidecar().getSidecarEntries().get(viewPagerPosition).getUrl();
      new DownloadSingleImage()
          .startDownloadImage(callingFragment, photoUrl, post.getUsername(), downloadButton);
    } else {
      // video
      String videoUrl = post.getSidecar().getSidecarEntries().get(viewPagerPosition).getUrl();
      new DownloadSingleVideo()
          .startDownloadVideo(callingFragment, videoUrl, post.getUsername(), downloadButton);
    }
  }

  private boolean postIsSingleImage() {
    return !post.getIs_sideCar() && !post.getIs_video();
  }

  private boolean postIsSingleVideo() {
    return !post.getIs_sideCar() && post.getIs_video();
  }

  private boolean postIsSidecar() {
    return post.getSidecar()
        .getSidecarEntries()
        .get(viewPagerPosition)
        .getSidecarEntryType()
        .equals(SidecarEntryType.image);
  }
}
