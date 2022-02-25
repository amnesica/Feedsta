package com.amnesica.feedsta.activity;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

import com.amnesica.feedsta.R;
import com.amnesica.feedsta.fragments.AboutFragment;
import com.amnesica.feedsta.fragments.CollectionsFragment;
import com.amnesica.feedsta.fragments.FeedFragment;
import com.amnesica.feedsta.fragments.FollowingFragment;
import com.amnesica.feedsta.fragments.HashtagFragment;
import com.amnesica.feedsta.fragments.PostFragment;
import com.amnesica.feedsta.fragments.ProfileFragment;
import com.amnesica.feedsta.fragments.SearchFragment;
import com.amnesica.feedsta.fragments.SettingsHolderFragment;
import com.amnesica.feedsta.helper.FragmentHelper;
import com.amnesica.feedsta.helper.StorageHelper;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

/**
 * MainActivity holds three main fragments and the BottomNavigationView
 */
@SuppressWarnings("deprecation")
public class MainActivity extends AppCompatActivity {

    // three main fragments
    private final Fragment feedFragment = new FeedFragment();
    private final Fragment searchFragment = new SearchFragment();
    private final Fragment bookmarksFragment = new CollectionsFragment();

    // menu fragments
    private final Fragment followingFragment = new FollowingFragment();
    private final Fragment infoFragment = new AboutFragment();
    private final Fragment settingsHolderFragment = new SettingsHolderFragment();

    // fragment manager and active fragment
    private final FragmentManager fm = getSupportFragmentManager();

    /**
     * Setup navigation of BottomNavigationView
     */
    private final BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @SuppressLint("NonConstantResourceId")
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_feed:
                    return FragmentHelper.loadAndShowFragment(feedFragment, fm);
                case R.id.navigation_search:
                    return FragmentHelper.loadAndShowFragment(searchFragment, fm);
                case R.id.navigation_collections:
                    return FragmentHelper.loadAndShowFragment(bookmarksFragment, fm);
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set theme (dark or light)
        setThemeBasedOnNightMode();

        setContentView(R.layout.activity_main);

        // setup BottomNavigationView
        BottomNavigationView navigation = findViewById(R.id.nav_view);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        //  Check that the activity is using the layout version with
        //  the fragment_container FrameLayout
        if (findViewById(R.id.main_container) != null) {

            //  However, if we're being restored from a previous state,
            //  then we don't need to do anything and should return or else
            //  we could end up with overlapping fragments
            if (savedInstanceState != null) {
                return;
            }

            fm.beginTransaction()
                    .add(R.id.main_container, infoFragment, infoFragment.getClass().getSimpleName())
                    .hide(infoFragment)
                    .commit();
            fm.beginTransaction()
                    .add(R.id.main_container, followingFragment, followingFragment.getClass().getSimpleName())
                    .hide(followingFragment)
                    .commit();
            fm.beginTransaction()
                    .add(R.id.main_container, settingsHolderFragment, settingsHolderFragment.getClass().getSimpleName())
                    .hide(settingsHolderFragment)
                    .commit();
            fm.beginTransaction()
                    .add(R.id.main_container, bookmarksFragment, bookmarksFragment.getClass().getSimpleName())
                    .hide(bookmarksFragment)
                    .commit();
            fm.beginTransaction()
                    .add(R.id.main_container, searchFragment, searchFragment.getClass().getSimpleName())
                    .hide(searchFragment)
                    .commit();
            fm.beginTransaction()
                    .add(R.id.main_container, feedFragment, feedFragment.getClass().getSimpleName())
                    .commit();

            // do all pending transactions do be able to get fragments with "fm.getFragments."
            fm.executePendingTransactions();
        }

        // if app was opened with deep link open account or post page
        Uri uri = getIntent().getData();
        if (uri != null) {
            List<String> params = uri.getPathSegments();

            if (params != null && params.size() >= 1 && params.get(0) != null &&
                    !params.get(0).isEmpty() && (params.size() == 2) &&
                    params.get(0).equals("p")) {

                // link is for post
                String postShortcode = params.get(1);
                if (postShortcode != null) {
                    goToPostFragment(postShortcode);
                }

            } else if (params != null && params.size() >= 1 && params.get(params.size() - 1) != null &&
                    !params.get(params.size() - 1).isEmpty() && !(params.size() > 1)) {

                // link is for account
                String username = params.get(params.size() - 1);
                if (username != null && !username.isEmpty()) {
                    goToProfileFragment(username);
                }

            } else if (params != null && params.size() == 3 && params.get(0).equals("explore") &&
                    params.get(1).equals("tags") && !params.get(2).isEmpty()) {

                // link is for hashtag
                String name = params.get(2);
                if (name != null && !name.isEmpty()) {
                    goToHashtagFragment(name);
                }
            }
        }
    }

    /**
     * Go to HashtagFragment with shortcode from deep link intent
     *
     * @param name name of hashtag
     */
    private void goToHashtagFragment(String name) {
        // new hashtagFragment
        HashtagFragment hashtagFragment = HashtagFragment.newInstance(name);

        // add fragment to container
        FragmentHelper.addFragmentToContainer(hashtagFragment, fm);
    }

    /**
     * Go to PostFragment with shortcode from deep link intent
     *
     * @param postShortcode shortcode of post
     */
    private void goToPostFragment(String postShortcode) {
        // new profileFragment
        PostFragment postFragment = PostFragment.newInstance(postShortcode);

        // add fragment to container
        FragmentHelper.addFragmentToContainer(postFragment, fm);
    }

    /**
     * Go to ProfileFragment with username from deep link intent
     *
     * @param username account username
     */
    private void goToProfileFragment(String username) {
        // new profileFragment
        ProfileFragment profileFragment = ProfileFragment.newInstance(username);

        // add fragment to container
        FragmentHelper.addFragmentToContainer(profileFragment, fm);
    }

    /**
     * if system is in night mode use DarkTheme otherwise use the app settings
     */
    private void setThemeBasedOnNightMode() {
        // check if system is in android night mode -> DarkTheme
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

        if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
            //  Night mode is active, we're using dark theme
            setTheme(R.style.DarkTheme);
            setThemeInAppSettingsToDark();

        } else {
            //  Night mode is not active, use the settings in app
            setThemeBasedOnAppSettings();
        }
    }

    /**
     * Sets the boolean of darkMode in sharedPreferences to true
     */
    private void setThemeInAppSettingsToDark() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (preferences != null) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("darkMode", true);
            editor.apply();
        }
    }

    /**
     * look in app settings to set theme to DarkTheme or LightTheme
     */
    private void setThemeBasedOnAppSettings() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (preferences != null) {
            boolean bDarkMode;
            bDarkMode = preferences.getBoolean(getResources().getString(R.string.dark_mode), false);
            if (bDarkMode) {
                setTheme(R.style.DarkTheme);
            } else {
                setTheme(R.style.LightTheme);
            }
        }
    }

    @Override
    protected void onDestroy() {
        // clear cache of glide on application exit
        Glide.get(getApplicationContext()).clearMemory();

        // clear disk cache of glide
        new ClearGlideDiskCache(this).execute();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        try {
            //  if there is a fragment and the back stack of this fragment is not empty,
            //  then emulate 'onBackPressed' behaviour, because in default, it is not working
            //  source: https://stackoverflow.com/questions/13418436/android-4-2-back-stack-behaviour-with-nested-fragments
            FragmentManager fm = getSupportFragmentManager();

            // check if backStack is empty, then show exit dialog
            //  (DO NOT CALL super.onBackPressed AFTER THIS!)
            if (fm.getBackStackEntryCount() == 0) {
                AlertDialog.Builder alertDialogBuilder;
                // create alertDialog
                alertDialogBuilder = new AlertDialog.Builder(this)
                        .setTitle(getResources().getString(R.string.title_closing_application_dialog))
                        .setMessage(getResources().getString(R.string.message_closing_application_dialog))
                        .setPositiveButton(R.string.positive_button_closing_application_dialog, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                // finish task (do not remove from recent apps list)
                                finish();
                            }
                        })
                        .setNegativeButton(R.string.negative_button_closing_application_dialog, null);

                final AlertDialog.Builder finalAlertDialogBuilder = alertDialogBuilder;

                AlertDialog alertDialog = finalAlertDialogBuilder.create();
                alertDialog.show();

                // change button text colors when button is shown
                TypedValue typedValue = new TypedValue();
                Resources.Theme theme = getTheme();
                theme.resolveAttribute(R.attr.colorAccent, typedValue, true);
                @ColorInt int color = typedValue.data;
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color);
                alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(color);
            } else {
                for (Fragment frag : fm.getFragments()) {
                    if (frag.isVisible()) {
                        FragmentManager childFm = frag.getChildFragmentManager();
                        if (childFm.getBackStackEntryCount() > 0) {
                            childFm.popBackStack();
                            return;
                        }
                    }
                }
                super.onBackPressed();
            }
        } catch (Exception e) {
            Log.d("MainActivity", Log.getStackTraceString(e));
        }
    }

    /**
     * Clears disk cache of glide on application exit
     */
    private static class ClearGlideDiskCache extends AsyncTask<Void, Void, Void> {

        private final WeakReference<MainActivity> activityReference;

        //  only retain a weak reference to the activity
        ClearGlideDiskCache(MainActivity context) {
            activityReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            //  This method must be called on a background thread.
            Glide.get(activityReference.get()).clearDiskCache();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }
}
