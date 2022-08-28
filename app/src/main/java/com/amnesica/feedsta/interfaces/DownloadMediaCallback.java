package com.amnesica.feedsta.interfaces;

public interface DownloadMediaCallback {
  void showImageDownloadResult(boolean downloadSuccessful);

  void showVideoDownloadResult(boolean downloadSuccessful);
}
