package com.amnesica.feedsta.activity;

import static com.amnesica.feedsta.helper.StaticIdentifier.permsRequestCode;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

import com.amnesica.feedsta.BuildConfig;
import com.amnesica.feedsta.R;
import com.amnesica.feedsta.fragments.AboutFragment;
import com.amnesica.feedsta.fragments.CollectionsFragment;
import com.amnesica.feedsta.fragments.FeedFragment;
import com.amnesica.feedsta.fragments.FollowingFragment;
import com.amnesica.feedsta.fragments.HashtagFragment;
import com.amnesica.feedsta.fragments.PostFragment;
import com.amnesica.feedsta.fragments.ProfileFragment;
import com.amnesica.feedsta.fragments.SearchFragment;
import com.amnesica.feedsta.fragments.settings.SettingsHolderFragment;
import com.amnesica.feedsta.helper.FragmentHelper;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.lang.ref.WeakReference;
import java.util.List;

/** MainActivity holds three main fragments and the BottomNavigationView */
public class MainActivity extends AppCompatActivity {

  // three main fragments
  private final Fragment feedFragment = new FeedFragment();
  private final Fragment searchFragment = new SearchFragment();
  private final Fragment bookmarksFragment = new CollectionsFragment();

  // menu fragments
  private final Fragment followingFragment = new FollowingFragment();
  private final Fragment infoFragment = new AboutFragment();
  private final Fragment settingsHolderFragment = new SettingsHolderFragment();

  // fragment manager
  private final FragmentManager fm = getSupportFragmentManager();

  /** Setup navigation of BottomNavigationView */
  private final BottomNavigationView.OnNavigationItemSelectedListener
      onNavigationItemSelectedListener =
          item -> {
            switch (item.getItemId()) {
              case R.id.navigation_feed:
                return FragmentHelper.loadAndShowFragment(feedFragment, fm);
              case R.id.navigation_search:
                return FragmentHelper.loadAndShowFragment(searchFragment, fm);
              case R.id.navigation_collections:
                return FragmentHelper.loadAndShowFragment(bookmarksFragment, fm);
            }
            return false;
          };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // set theme (dark or light)
    setAppTheme();

    // apply dynamic color
    DynamicColors.applyToActivitiesIfAvailable(getApplication());

    setContentView(R.layout.activity_main);

    // setup BottomNavigationView
    BottomNavigationView navigation = findViewById(R.id.nav_view);
    navigation.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener);

    addFragmentTransactions(savedInstanceState);

    // if app was opened with deep link open account or post page
    handleDeepLinks();

    // show usage note on startup if it was not already shown
    if (!getBooleanFromSharedPreferences(
        getResources().getString(R.string.usage_note_was_shown), false)) {
      showUsageAndPermissionsNote();
    } else {
      // ask for permissions needed for downloading
      askForPermissionsIfNecessary();
    }
  }

  private void handleDeepLinks() {
    Uri uri = getIntent().getData();
    if (uri != null) {
      List<String> params = uri.getPathSegments();

      if (params != null
          && params.size() >= 1
          && params.get(0) != null
          && !params.get(0).isEmpty()
          && (params.size() == 2)
          && params.get(0).equals("p")) {

        // link is for post
        String postShortcode = params.get(1);
        if (postShortcode != null) {
          goToPostFragment(postShortcode);
        }

      } else if (params != null
          && params.size() >= 1
          && params.get(params.size() - 1) != null
          && !params.get(params.size() - 1).isEmpty()
          && !(params.size() > 1)) {

        // link is for account
        String username = params.get(params.size() - 1);
        if (username != null && !username.isEmpty()) {
          goToProfileFragment(username);
        }

      } else if (params != null
          && params.size() == 3
          && params.get(0).equals("explore")
          && params.get(1).equals("tags")
          && !params.get(2).isEmpty()) {

        // link is for hashtag
        String name = params.get(2);
        if (name != null && !name.isEmpty()) {
          goToHashtagFragment(name);
        }
      }
    }
  }

  private void addFragmentTransactions(Bundle savedInstanceState) {
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
          .add(
              R.id.main_container,
              settingsHolderFragment,
              settingsHolderFragment.getClass().getSimpleName())
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
  }

  /** Shows usage note as dialog and shows permissions note when usage note was accepted */
  private void showUsageAndPermissionsNote() {
    // alert dialog
    MaterialAlertDialogBuilder alertDialogBuilder;

    // create alertDialog
    alertDialogBuilder =
        new MaterialAlertDialogBuilder(MainActivity.this)
            .setTitle(R.string.dialog_usage_note_title)
            .setMessage(R.string.dialog_usage_note_message)
            .setPositiveButton(
                R.string.dialog_usage_note_yes,
                (dialog, which) -> {
                  // save information that dialog was shown in shared preferences
                  setBooleanInSharedPreferences(
                      getResources().getString(R.string.usage_note_was_shown), true);

                  // ask for permissions needed for downloading
                  askForPermissionsIfNecessary();
                })
            .setNegativeButton(
                R.string.dialog_usage_note_exit_app,
                (dialog, which) -> {
                  dialog.dismiss();

                  // save information that dialog was shown in shared preferences
                  setBooleanInSharedPreferences(
                      getResources().getString(R.string.usage_note_was_shown), true);

                  // exit app
                  finish();
                });

    final MaterialAlertDialogBuilder finalAlertDialogBuilder = alertDialogBuilder;

    // get color for button texts
    TypedValue typedValue = new TypedValue();
    Resources.Theme theme = getTheme();
    theme.resolveAttribute(R.attr.colorAccent, typedValue, true);
    @ColorInt final int color = typedValue.data;

    // create alertDialog
    AlertDialog alertDialog = finalAlertDialogBuilder.create();
    alertDialog.setCancelable(false);
    alertDialog.show();
    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color);
    alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(color);
  }

  /**
   * Gets the boolean value in SharedPreferences. If value does not exist in SharedPreferences
   * fallbackResult will be returned as result
   *
   * @param value String
   * @return boolean
   */
  private boolean getBooleanFromSharedPreferences(
      final String value, final boolean fallbackResult) {
    boolean result = false;

    // get boolean from sharedPreferences
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    if (preferences != null && value != null) {
      result = preferences.getBoolean(value, fallbackResult);
    }

    return result;
  }

  private void setBooleanInSharedPreferences(final String value, final boolean result) {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    if (preferences != null && value != null) {
      SharedPreferences.Editor editor = preferences.edit();
      editor.putBoolean(value, result);
      editor.apply();
    }
  }

  private void askForPermissionsIfNecessary() {
    boolean allPermsGranted = false;
    int permGrantedWrite =
        ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    int permGrantedRead =
        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      // READ permission and MANAGE_EXTERNAL_STORAGE needed
      if (permGrantedRead == PackageManager.PERMISSION_GRANTED
          && Environment.isExternalStorageManager()) {
        allPermsGranted = true;
      }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
      // only READ permission needed
      if (permGrantedRead == PackageManager.PERMISSION_GRANTED) {
        allPermsGranted = true;
      }
    } else {
      // WRITE and READ permission needed
      if (permGrantedWrite == PackageManager.PERMISSION_GRANTED
          && permGrantedRead == PackageManager.PERMISSION_GRANTED) {
        allPermsGranted = true;
      }
    }

    if (!allPermsGranted) {
      showPermissionsNote();
    }
  }

  private void requestRequiredPermissions() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      startIntentToGetManageAllFilesPermission();

      ActivityCompat.requestPermissions(
          this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, permsRequestCode);
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
      ActivityCompat.requestPermissions(
          this,
          new String[] {
            Manifest.permission.READ_EXTERNAL_STORAGE,
          },
          permsRequestCode);
    } else {
      ActivityCompat.requestPermissions(
          this,
          new String[] {
            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE,
          },
          permsRequestCode);
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.R)
  private void startIntentToGetManageAllFilesPermission() {
    Uri uri = Uri.parse("package:" + BuildConfig.APPLICATION_ID);
    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri);
    startActivity(intent);
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    if (requestCode == permsRequestCode) {
      // If request is cancelled, the result arrays are empty
      if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
        // permission request was denied
        FragmentHelper.showToast(
            getResources().getString(R.string.permissions_denied),
            MainActivity.this,
            MainActivity.this);

        // exit app
        finish();
      }
    }
  }

  /** Shows permissions note as dialog */
  private void showPermissionsNote() {
    // alert dialog
    MaterialAlertDialogBuilder alertDialogBuilder;

    // create alertDialog
    alertDialogBuilder =
        new MaterialAlertDialogBuilder(MainActivity.this)
            .setTitle(R.string.dialog_permissions_note_title)
            .setMessage(R.string.dialog_permissions_note_message)
            .setPositiveButton(
                R.string.dialog_permissions_note_yes,
                (dialog, which) -> {
                  // ask for permissions
                  requestRequiredPermissions();
                })
            .setNegativeButton(
                R.string.dialog_permissions_note_exit_app,
                (dialog, which) -> {
                  dialog.dismiss();

                  FragmentHelper.showToast(
                      getResources().getString(R.string.permissions_denied),
                      MainActivity.this,
                      MainActivity.this);

                  // exit app
                  finish();
                });

    final MaterialAlertDialogBuilder finalAlertDialogBuilder = alertDialogBuilder;

    // get color for button texts
    TypedValue typedValue = new TypedValue();
    Resources.Theme theme = getTheme();
    theme.resolveAttribute(R.attr.colorAccent, typedValue, true);
    @ColorInt final int color = typedValue.data;

    // create alertDialog
    AlertDialog alertDialog = finalAlertDialogBuilder.create();
    alertDialog.setCancelable(false);
    alertDialog.show();
    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color);
    alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(color);
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
   * Set theme of the app (light or dark). If on first start of the app after install the system is
   * in night/dark mode the app is also set to dark mode. On every app start afterwards the value
   * stored in SharedPreference is used
   */
  private void setAppTheme() {
    if (getBooleanFromSharedPreferences(
        getResources().getString(R.string.first_start_of_app), true)) {
      // on first app start set theme after system mode (light or dark)
      setThemeBasedOnSystemMode();

      // set value firstStartOfApp in SharedPreferences to false
      setBooleanInSharedPreferences(getResources().getString(R.string.first_start_of_app), false);
    } else {
      // on subsequent start set theme after value in SharedPreferences
      setThemeBasedOnValueInSharedPreferences();
    }
  }

  /** If system is in night mode use DarkTheme and set value "darkMode" in SharedPreferences */
  private void setThemeBasedOnSystemMode() {
    // check if system is in android night mode -> DarkTheme
    int currentNightMode =
        getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

    switch (currentNightMode) {
      case Configuration.UI_MODE_NIGHT_NO:
        // night mode is not active, we're in day time
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
      case Configuration.UI_MODE_NIGHT_YES:
        // night mode is active, we're at night!
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        // set value darkMode in SharedPreferences to true
        setBooleanInSharedPreferences(getResources().getString(R.string.dark_mode), true);
      case Configuration.UI_MODE_NIGHT_UNDEFINED:
        // assume notnight
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
  }

  /**
   * Gets the value of "darkMode" in SharedPreferences and sets the theme to DarkTheme or LightTheme
   * based on this value
   */
  private void setThemeBasedOnValueInSharedPreferences() {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    if (preferences != null) {
      boolean bDarkMode;
      bDarkMode = preferences.getBoolean(getResources().getString(R.string.dark_mode), false);
      if (bDarkMode) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
      } else {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
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
      //  source: https://stackoverflow.com/questions/13418436/android-4-2-back-stack-
      //  behaviour-with-nested-fragments
      FragmentManager fm = getSupportFragmentManager();

      // check if backStack is empty, then show exit dialog
      //  (DO NOT CALL super.onBackPressed AFTER THIS!)
      if (fm.getBackStackEntryCount() == 0) {
        MaterialAlertDialogBuilder alertDialogBuilder;
        // create alertDialog
        alertDialogBuilder =
            new MaterialAlertDialogBuilder(this)
                .setTitle(getResources().getString(R.string.title_closing_application_dialog))
                .setMessage(getResources().getString(R.string.message_closing_application_dialog))
                .setPositiveButton(
                    R.string.positive_button_closing_application_dialog,
                    (dialog, which) -> {
                      dialog.dismiss();
                      // finish task (do not remove from recent apps list)
                      finish();
                    })
                .setNegativeButton(R.string.negative_button_closing_application_dialog, null);

        final MaterialAlertDialogBuilder finalAlertDialogBuilder = alertDialogBuilder;

        AlertDialog alertDialog = finalAlertDialogBuilder.create();
        alertDialog.show();

        // change button text colors when button is shown
        @ColorInt int color = FragmentHelper.getColorId(this, R.attr.colorAccent);
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

  /** Clears disk cache of glide on application exit */
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
