package com.amnesica.feedsta.asynctasks;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import androidx.fragment.app.Fragment;

import com.amnesica.feedsta.R;
import com.amnesica.feedsta.fragments.PostFragment;
import com.amnesica.feedsta.helper.FragmentHelper;
import com.amnesica.feedsta.helper.StorageHelper;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;

/**
 * Async task to download an image from an url (used in PostFragment and ProfileFragment)
 */
public class DownloadImage extends AsyncTask<Void, Void, Void> {

    private final WeakReference<Fragment> fragmentReference;
    private final String photoUrl;
    private final String filename;
    private boolean saved = false;

    // constructor
    public DownloadImage(Fragment fragment, String photoUrl, String filename) {
        this.fragmentReference = new WeakReference<>(fragment);
        this.photoUrl = photoUrl;
        this.filename = filename;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        if (isCancelled()) return null;

        // get reference from fragment
        final Fragment fragment = fragmentReference.get();
        if (fragment == null || photoUrl == null || filename == null) return null;

        try {
            java.net.URL url = new java.net.URL(photoUrl);

            // open connection to url
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);

            // save to storage
            saved = StorageHelper.saveImage(myBitmap, filename, fragment.getContext());

            input.close();
        } catch (Exception e) {
            Log.d("DownloadImage", Log.getStackTraceString(e));
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
            // change icon if fragment is PostFragment
            if (fragment instanceof PostFragment) {
                ((PostFragment) fragment).setButtonDownloadToSaved();
            }

            FragmentHelper.showToast(fragment.getResources().getString(R.string.image_saved),
                                     fragment.requireActivity(), fragment.requireContext());
        } else {
            FragmentHelper.showToast(fragment.getResources().getString(R.string.image_saved_failed),
                                     fragment.requireActivity(), fragment.requireContext());
        }
    }
}
