package com.amnesica.feedsta.asynctasks.download;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Task to download an image from an url (used for FeedFragment, PostFragment and ProfileFragment)
 */
public class DownloadSingleImage {

  private static boolean imageSaved = false;

  public void startDownloadImage(
      final Fragment fragment,
      final String photoUrl,
      final String filename,
      final ImageButton downloadButton) {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Handler handler = new Handler(Looper.getMainLooper());

    executor.execute(
        () -> {
          // Background work here
          new DownloadImageRunnable(fragment, photoUrl, filename).run();

          // UI Thread work here
          handler.post(() -> showDownloadResult(fragment, downloadButton));
        });

    executor.shutdown();
  }

  private void showDownloadResult(final Fragment callingFragment, final ImageButton imageButton) {
    final Fragment fragment = new WeakReference<>(callingFragment).get();
    if (fragment == null) return;

    if (imageSaved) {
      // change button
      imageButton.setBackgroundResource(R.drawable.ic_file_download_24dp);

      FragmentHelper.showToast(
          fragment.getResources().getString(R.string.image_saved),
          fragment.requireActivity(),
          fragment.requireContext());
    } else {
      FragmentHelper.showToast(
          fragment.getResources().getString(R.string.image_saved_failed),
          fragment.requireActivity(),
          fragment.requireContext());
    }
  }

  public static class DownloadImageRunnable implements Runnable {
    private final WeakReference<Fragment> fragmentReference;
    private final String photoUrl;
    private final String filename;

    // constructor
    public DownloadImageRunnable(Fragment fragment, String photoUrl, String filename) {
      this.fragmentReference = new WeakReference<>(fragment);
      this.photoUrl = photoUrl;
      this.filename = filename;
    }

    @Override
    public void run() {
      downloadImage();
    }

    private void downloadImage() {
      try {
        final Fragment fragment = fragmentReference.get();
        if (fragment == null || photoUrl == null || filename == null) return;

        URL url = new URL(photoUrl);

        // open connection to url
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoInput(true);
        connection.connect();
        InputStream input = connection.getInputStream();
        Bitmap myBitmap = BitmapFactory.decodeStream(input);

        // save to storage
        imageSaved = StorageHelper.saveImage(myBitmap, filename, fragment.getContext());

        input.close();

      } catch (Exception e) {
        Log.d("DownloadImageRunnable", Log.getStackTraceString(e));
      }
    }
  }
}
