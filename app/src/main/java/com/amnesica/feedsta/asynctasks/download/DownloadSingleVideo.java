package com.amnesica.feedsta.asynctasks.download;

import android.app.ProgressDialog;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageButton;

import androidx.fragment.app.Fragment;

import com.amnesica.feedsta.R;
import com.amnesica.feedsta.helper.FragmentHelper;
import com.amnesica.feedsta.helper.StorageHelper;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

public class DownloadSingleVideo {
  private static boolean videoSaved = false;
  private static ProgressDialog progressDialog;

  public void startDownloadVideo(
      final Fragment fragment,
      final String videoUrl,
      final String filename,
      final ImageButton downloadButton) {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Handler handler = new Handler(Looper.getMainLooper());

    executor.execute(
        () -> {
          showProgressDialog(fragment);

          // Background work here
          new DownloadVideoRunnable(fragment, videoUrl, filename).run();

          // UI Thread work here
          handler.post(() -> DownloadSingleVideo.showDownloadResult(fragment, downloadButton));
        });

    executor.shutdown();
  }

  private void showProgressDialog(final Fragment callingFragment) {
    final Fragment fragment = new WeakReference<>(callingFragment).get();
    if (fragment == null) return;

    fragment
        .requireActivity()
        .runOnUiThread(
            () -> {
              progressDialog = new ProgressDialog(fragment.requireContext());
              progressDialog.setTitle(
                  fragment
                      .requireContext()
                      .getString(R.string.progress_dialog_title_download_video));
              progressDialog.setMessage(
                  fragment
                      .requireContext()
                      .getString(R.string.progress_dialog_message_download_selected_posts));
              progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
              progressDialog.setProgress(0);
              progressDialog.show();
            });
  }

  private static void showDownloadResult(
      final Fragment callingFragment, final ImageButton imageButton) {
    final Fragment fragment = new WeakReference<>(callingFragment).get();
    if (fragment == null) return;

    if (videoSaved) {
      // change button
      imageButton.setBackgroundResource(R.drawable.ic_file_download_24dp);

      FragmentHelper.showToast(
          fragment.getResources().getString(R.string.video_saved),
          fragment.requireActivity(),
          fragment.requireContext());
    } else {
      FragmentHelper.showToast(
          fragment.getResources().getString(R.string.video_saved_failed),
          fragment.requireActivity(),
          fragment.requireContext());
    }
    progressDialog.dismiss();
  }

  public static class DownloadVideoRunnable implements Runnable {
    private final WeakReference<Fragment> fragmentReference;
    private final String videoUrl;
    private final String filename;

    // constructor
    public DownloadVideoRunnable(Fragment fragment, String videoUrl, String filename) {
      this.fragmentReference = new WeakReference<>(fragment);
      this.videoUrl = videoUrl;
      this.filename = filename;
    }

    @Override
    public void run() {
      downloadVideo();
    }

    private void downloadVideo() {
      try {
        final Fragment fragment = fragmentReference.get();
        if (fragment == null || videoUrl == null || filename == null) return;

        URL url = new URL(videoUrl);

        // open connection to url
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        int response = con.getResponseCode();
        InputStream inputStream = con.getInputStream();
        byte[] buffer = new byte[7 * 1024];

        videoSaved =
            StorageHelper.saveVideo(inputStream, buffer, filename, fragment.requireContext());

      } catch (Exception e) {
        Log.d("DownloadVideoRunnable", Log.getStackTraceString(e));
      }
    }
  }
}
