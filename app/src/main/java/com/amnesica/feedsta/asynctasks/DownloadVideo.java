package com.amnesica.feedsta.asynctasks;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;

import androidx.fragment.app.Fragment;

import com.amnesica.feedsta.R;
import com.amnesica.feedsta.fragments.PostFragment;
import com.amnesica.feedsta.helper.FragmentHelper;
import com.amnesica.feedsta.helper.StorageHelper;

import java.io.InputStream;
import java.lang.ref.WeakReference;

import javax.net.ssl.HttpsURLConnection;

/** Async task to download a video from an url (used in PostFragment) */
public class DownloadVideo extends AsyncTask<Void, Void, Void> {

  private final WeakReference<Fragment> fragmentReference;
  private final String videoUrl;
  private final String filename;
  private boolean saved = false;

  // constructor
  public DownloadVideo(Fragment context, String videoUrl, String filename) {
    fragmentReference = new WeakReference<>(context);
    this.videoUrl = videoUrl;
    this.filename = filename;
  }

  @Override
  protected void onPreExecute() {
    showProgressDialog();
  }

  @Override
  protected Void doInBackground(Void... voids) {
    if (isCancelled()) return null;

    // get reference from fragment
    final Fragment fragment = fragmentReference.get();
    if (fragment == null || videoUrl == null || filename == null) return null;

    try {
      java.net.URL url = new java.net.URL(videoUrl);

      // open connection to url
      HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
      int response = con.getResponseCode();
      InputStream inputStream = con.getInputStream();
      byte[] buffer = new byte[7 * 1024];

      saved = StorageHelper.saveVideo(inputStream, buffer, filename, fragment.requireContext());
    } catch (Exception e) {
      Log.d("DownloadVideo", Log.getStackTraceString(e));
    }
    return null;
  }

  @Override
  protected void onPostExecute(Void aVoid) {
    super.onPostExecute(aVoid);
    if (isCancelled()) return;

    // get reference from fragment
    final Fragment fragment = fragmentReference.get();
    if (fragment == null) return;

    if (saved) {
      // change icon and display toast
      if (fragment instanceof PostFragment) {
        ((PostFragment) fragment).setButtonDownloadToSaved();

        // dismiss progressDialog
        ((PostFragment) fragment).progressDialogBatch.dismiss();
      }

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
  }

  private void showProgressDialog() {
    if (isCancelled()) return;

    // get reference from fragment
    final Fragment fragment = fragmentReference.get();
    if (fragment == null) return;

    if (fragment instanceof PostFragment) {
      ((PostFragment) fragment).progressDialogBatch = new ProgressDialog(fragment.requireContext());
      ((PostFragment) fragment)
          .progressDialogBatch.setTitle(
              fragment.requireContext().getString(R.string.progress_dialog_title_download_video));
      ((PostFragment) fragment)
          .progressDialogBatch.setMessage(
              fragment
                  .requireContext()
                  .getString(R.string.progress_dialog_message_download_selected_posts));
      ((PostFragment) fragment).progressDialogBatch.setProgressStyle(ProgressDialog.STYLE_SPINNER);
      ((PostFragment) fragment).progressDialogBatch.setProgress(0);
      ((PostFragment) fragment).progressDialogBatch.show();
    }
  }
}
