package com.amnesica.feedsta.helper;

import static android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;
import static android.view.View.GONE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

import com.amnesica.feedsta.R;
import com.amnesica.feedsta.fragments.FeedFragment;
import com.amnesica.feedsta.fragments.HashtagFragment;
import com.amnesica.feedsta.fragments.ProfileFragment;
import com.amnesica.feedsta.fragments.fullscreenimages.FullscreenImagePostFragment;
import com.amnesica.feedsta.fragments.fullscreenimages.FullscreenProfileImageFragment;
import com.amnesica.feedsta.models.Collection;
import com.amnesica.feedsta.models.Post;
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Helper class for fragments with various methods */
public class FragmentHelper {

  // map for only showing one alert at a time from a fragment
  private static final HashMap<String, Boolean> mapErrorWasShown = new HashMap<>();

  // counter for getting unique tag for fragments
  private static int transactionCounter = 0;

  /**
   * Returns the transactionCounter for getting unique tags
   *
   * @return int
   */
  private static int getTransactionCounter() {
    return transactionCounter += 1;
  }

  /**
   * Sets up the toolbar of the weakReference fragment with a backButton and a clickListener to go
   * back on click
   *
   * @param toolbar Toolbar
   * @param fragmentReference WeakReference
   */
  public static void setupToolbarWithBackButton(Toolbar toolbar, WeakReference fragmentReference) {
    final Fragment weakReferenceFragment = (Fragment) fragmentReference.get();
    if (weakReferenceFragment != null) {
      toolbar.setNavigationIcon(R.drawable.ic_arrow_back_24dp);

      toolbar.setNavigationOnClickListener(
          new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              weakReferenceFragment.requireActivity().onBackPressed();
            }
          });
    }
  }

  /**
   * Returns the top fragment from backStack. If FragmentManager is null, return currently visible
   * fragment
   *
   * @param fm FragmentManager
   * @return Fragment
   */
  private static Fragment getTopFragment(FragmentManager fm) {
    if (fm != null) {
      if (fm.getBackStackEntryCount() == 0) {
        // no backStack, return the fragment which is currently visible to user
        Fragment active;
        for (Fragment frag : fm.getFragments()) {
          if (frag.isVisible()) {
            active = frag;
            return active;
          }
        }

        // only for deep linking (before: nothing here)
        return fm.findFragmentByTag(FeedFragment.class.getSimpleName());
      }
      // get top fragment from backStack
      String fragmentTag = fm.getBackStackEntryAt(fm.getBackStackEntryCount() - 1).getName();
      return fm.findFragmentByTag(fragmentTag);
    }
    return null;
  }

  /**
   * Loads and shows fragmentToShow from FragmentManager into container
   *
   * @param fragmentToShow Fragment
   * @param fm FragmentManager
   * @return Boolean
   */
  public static Boolean loadAndShowFragment(Fragment fragmentToShow, FragmentManager fm) {
    if (fragmentToShow != null) {
      // get active fragment to hide
      Fragment active = getTopFragment(fm);

      if (active != null) {
        // show fragment with animation
        fm.beginTransaction()
            .setCustomAnimations(
                R.anim.nav_enter, R.anim.nav_exit, R.anim.nav_enter, R.anim.nav_exit)
            .hide(active)
            .show(fragmentToShow)
            .addToBackStack(fragmentToShow.getClass().getSimpleName())
            .commit();
        return true;
      }
    }
    return false;
  }

  /**
   * Adds a fragment to the container and to the backStack
   *
   * @param fragmentToAdd Fragment
   * @param fm FragmentManager
   */
  public static void addFragmentToContainer(Fragment fragmentToAdd, FragmentManager fm) {
    if (fragmentToAdd != null && fm != null) {
      // get unique tag with class name of fragmentToAdd and incrementing counter
      String tag =
          fragmentToAdd.getClass().getSimpleName() + FragmentHelper.getTransactionCounter();

      // get active fragment
      Fragment active = getTopFragment(fm);

      // add fragment to container with animation
      try {
        // use animation for nav elements also for fullscreen fragments
        if (fragmentToAdd
                .getClass()
                .getSimpleName()
                .equals(FullscreenImagePostFragment.class.getSimpleName())
            || fragmentToAdd
                .getClass()
                .getSimpleName()
                .equals(FullscreenProfileImageFragment.class.getSimpleName())) {
          fm.beginTransaction()
              .setCustomAnimations(
                  R.anim.nav_enter, R.anim.nav_exit,
                  R.anim.nav_enter, R.anim.nav_exit)
              .hide(active)
              .add(R.id.main_container, fragmentToAdd, tag)
              .addToBackStack(tag)
              .commit();
        } else {
          fm.beginTransaction()
              .setCustomAnimations(
                  R.anim.frag_enter, R.anim.frag_exit,
                  R.anim.frag_pop_enter, R.anim.frag_pop_exit)
              .hide(active)
              .add(R.id.main_container, fragmentToAdd, tag)
              .addToBackStack(tag)
              .commit();
        }

      } catch (Exception e) {
        Log.d("FragmentHelper", Log.getStackTraceString(e));
      }
    }
  }

  /**
   * Sets highlighted element in navigation bar and makes navigation bar visible
   *
   * @param activity FragmentActivity
   * @param resource int
   */
  public static void setBottomNavViewSelectElem(FragmentActivity activity, int resource) {
    if (activity == null) return;

    BottomNavigationView bottomNavigationView =
        Objects.requireNonNull(activity).findViewById(R.id.nav_view);
    if (bottomNavigationView == null) return;

    // select item in bottom navigation view
    Menu menu = bottomNavigationView.getMenu();
    menu.findItem(resource).setCheckable(true);
    menu.findItem(resource).setChecked(true);

    // Slide up bottom navigation view if necessary
    slideUpBottomNavigationBar(activity);
  }

  /**
   * Slides up the bottom navigation view and thus makes it visible
   *
   * @param activity FragmentActivity
   */
  public static void slideUpBottomNavigationBar(FragmentActivity activity) {
    if (activity == null) return;

    BottomNavigationView bottomNavigationView =
        Objects.requireNonNull(activity).findViewById(R.id.nav_view);
    if (bottomNavigationView == null) return;
    bottomNavigationView.setVisibility(View.VISIBLE);

    ViewGroup.LayoutParams layoutParams = bottomNavigationView.getLayoutParams();
    if (layoutParams instanceof CoordinatorLayout.LayoutParams) {
      CoordinatorLayout.Behavior behavior =
          ((CoordinatorLayout.LayoutParams) layoutParams).getBehavior();
      if (behavior instanceof HideBottomViewOnScrollBehavior) {
        HideBottomViewOnScrollBehavior<BottomNavigationView> hideShowBehavior =
            (HideBottomViewOnScrollBehavior<BottomNavigationView>) behavior;
        hideShowBehavior.slideUp(bottomNavigationView);
      }
    }
  }

  /**
   * Slides down the bottom navigation view and thus makes it invisible
   *
   * @param activity FragmentActivity
   */
  public static void slideDownBottomNavigationBar(FragmentActivity activity) {
    if (activity == null) return;

    BottomNavigationView bottomNavigationView =
        Objects.requireNonNull(activity).findViewById(R.id.nav_view);
    if (bottomNavigationView == null) return;
    bottomNavigationView.setVisibility(View.VISIBLE);

    ViewGroup.LayoutParams layoutParams = bottomNavigationView.getLayoutParams();
    if (layoutParams instanceof CoordinatorLayout.LayoutParams) {
      CoordinatorLayout.Behavior behavior =
          ((CoordinatorLayout.LayoutParams) layoutParams).getBehavior();
      if (behavior instanceof HideBottomViewOnScrollBehavior) {
        HideBottomViewOnScrollBehavior<BottomNavigationView> hideShowBehavior =
            (HideBottomViewOnScrollBehavior<BottomNavigationView>) behavior;
        hideShowBehavior.slideDown(bottomNavigationView);
      }
    }
  }

  /**
   * Makes the bottom navigation view disappear by setting it to invisible
   *
   * @param activity FragmentActivity
   */
  public static void makeBottomNavigationBarInvisible(FragmentActivity activity) {
    if (activity == null) return;

    BottomNavigationView bottomNavigationView =
        Objects.requireNonNull(activity).findViewById(R.id.nav_view);
    if (bottomNavigationView == null) return;
    bottomNavigationView.setVisibility(View.INVISIBLE);
  }

  /**
   * Notifies the user that no account or not all but some accounts could be queried and shows a
   * dialog. Method handles the NOT_ALL_ACCOUNTS_COULD_BE_QUERIED error
   *
   * @param fragment Fragment
   * @param actualFetchedAccounts int
   * @param amountAccountsToFetch int
   */
  public static void notifyUserOfIncompleteFetchProblem(
      final Fragment fragment, int actualFetchedAccounts, int amountAccountsToFetch) {
    final Enum<Error> error = Error.NOT_ALL_ACCOUNTS_COULD_BE_QUERIED;

    if (!mapErrorWasShown.containsKey(error.toString())) {

      // put fragment into map to eliminate error duplicates
      mapErrorWasShown.put(error.toString(), true);

      // text for dialog
      String alertText;

      // text for label at snackbar's left
      String textViewLabel;

      // label for action of snackbar
      String textViewLabelAction;

      // fixing "Fragment not attached to a context"
      if (fragment != null && fragment.getContext() != null) {
        if (actualFetchedAccounts > 0) {
          // not all accounts cound be queried
          textViewLabel =
              fragment
                      .requireContext()
                      .getResources()
                      .getString(R.string.not_all_accounts_could_be_queried)
                  + " "
                  + "("
                  + actualFetchedAccounts
                  + "/"
                  + amountAccountsToFetch
                  + ")";
          // get proper response
          alertText =
              fragment
                  .requireContext()
                  .getResources()
                  .getString(R.string.not_all_accounts_could_be_queried_more_info);
        } else {
          // nothing could be querid
          textViewLabel =
              fragment
                      .requireContext()
                      .getResources()
                      .getString(R.string.no_account_could_be_queried)
                  + " "
                  + "("
                  + actualFetchedAccounts
                  + "/"
                  + amountAccountsToFetch
                  + ")";
          // get proper response
          alertText =
              fragment
                  .requireContext()
                  .getResources()
                  .getString(R.string.no_account_could_be_queried_more_info);
        }

        textViewLabelAction =
            fragment.requireContext().getResources().getString(R.string.more_info);

        // show alert as custom snackBar
        showCustomSnackBarForAlert(
            fragment, alertText, error.toString(), textViewLabel, textViewLabelAction);
      }
    }
  }

  /**
   * Shows network error or something wrong error to user. Calls notifyUserOfProblem internally
   *
   * @param fragment Fragment
   */
  public static void showNetworkOrSomethingWrongErrorToUser(Fragment fragment) {
    if (!NetworkHandler.isInternetAvailable()) {
      notifyUserOfProblem(fragment, Error.NO_INTERNET_CONNECTION);
    } else {
      notifyUserOfProblem(fragment, Error.SOMETHINGS_WRONG);
    }
  }

  /**
   * Notifies the user that there is a problem and shows a dialog. Hint:
   * NOT_ALL_ACCOUNTS_COULD_BE_QUERIED is supposed to be handled in
   * notifyUserOfIncompleteFetchProblem()!
   *
   * @param fragment Fragment
   * @param error Enum<Error>
   */
  public static void notifyUserOfProblem(final Fragment fragment, final Enum<Error> error) {
    if (!mapErrorWasShown.containsKey(error.toString())) {

      // TODO Replace custom snackbar with actual snackbar and SnackbarHelper

      // put fragment into map to eliminate error duplicates
      mapErrorWasShown.put(error.toString(), true);
      String errorMessage = error.toString();

      // text for dialog
      String alertText = null;

      // text for label at snackbar's left
      String textViewLabel = null;

      // label for action of snackbar
      String textViewLabelAction = null;

      // fixing "Fragment not attached to a context"
      if (fragment != null && fragment.getContext() != null) {
        // get proper response
        if (errorMessage.equals(Error.NO_INTERNET_CONNECTION.toString())) {
          alertText =
              fragment.requireContext().getResources().getString(R.string.no_internet_connection);
          textViewLabel =
              fragment.requireContext().getResources().getString(R.string.no_internet_connection);
          textViewLabelAction = fragment.requireContext().getResources().getString(R.string.okay);
        } else if (errorMessage.equals(Error.UPDATING_BOOKMARKS_CATEGORY_FAILED.toString())) {
          alertText =
              fragment
                  .requireContext()
                  .getResources()
                  .getString(R.string.updating_bookmarked_post_category_failed);
          textViewLabel =
              fragment
                  .requireContext()
                  .getResources()
                  .getString(R.string.updating_bookmarked_post_category_failed_label);
          textViewLabelAction = fragment.requireContext().getResources().getString(R.string.okay);
        } else if (errorMessage.equals(Error.SOMETHINGS_WRONG.toString())) {
          alertText =
              fragment
                  .requireContext()
                  .getResources()
                  .getString(R.string.something_went_wrong_more_info);
          textViewLabel =
              fragment.requireContext().getResources().getString(R.string.something_has_gone_wrong);
          textViewLabelAction =
              fragment.requireContext().getResources().getString(R.string.more_info);
        } else if (errorMessage.equals(Error.POST_NOT_AVAILABLE_ANYMORE.toString())) {
          alertText =
              fragment
                  .requireContext()
                  .getResources()
                  .getString(R.string.post_not_available_anymore);
          textViewLabel =
              fragment
                  .requireContext()
                  .getResources()
                  .getString(R.string.post_not_available_anymore_label);
          textViewLabelAction =
              fragment.requireContext().getResources().getString(R.string.more_info);
        } else if (errorMessage.equals(Error.ACCOUNT_COULD_NOT_BE_FOLLOWED.toString())) {
          alertText =
              fragment
                  .requireContext()
                  .getResources()
                  .getString(R.string.account_could_not_be_followed);
          textViewLabel =
              fragment
                  .requireContext()
                  .getResources()
                  .getString(R.string.account_could_not_be_followed);
          textViewLabelAction = fragment.requireContext().getResources().getString(R.string.okay);
        }

        // show alert as custom snackBar
        showCustomSnackBarForAlert(
            fragment, alertText, error.toString(), textViewLabel, textViewLabelAction);
      }
    }
  }

  /**
   * Shows a custom snackBar to show option to save post to collection above the bottom navigation
   * view
   *
   * @param fragment Fragment
   * @param alertText String
   * @param error String
   * @param textViewLabel String
   * @param textViewLabelAction String
   */
  private static void showCustomSnackBarForAlert(
      final Fragment fragment,
      final String alertText,
      final String error,
      final String textViewLabel,
      final String textViewLabelAction) {
    if (fragment != null && fragment.getContext() != null) {
      final ConstraintLayout conLayCustomSnackBarAlert =
          fragment.requireActivity().findViewById(R.id.conLayCustomSnackBarAlert);
      final TextView textViewLabelAlert =
          fragment.requireActivity().findViewById(R.id.textViewLabelAlert);
      final TextView textViewActionAlert =
          fragment.requireActivity().findViewById(R.id.textViewActionAlert);

      final Runnable[] runnable = new Runnable[1];

      // Fixed: Can't create handler inside thread Thread[AsyncTask #2,5,main]
      // that has not called Looper.prepare()
      if (fragment.isAdded()) {
        fragment
            .requireActivity()
            .runOnUiThread(
                new Runnable() {
                  @Override
                  public void run() {
                    final Handler handler = new Handler();
                  }
                });
      }

      if (fragment.isAdded()) {
        fragment
            .requireActivity()
            .runOnUiThread(
                new Runnable() {
                  @Override
                  public void run() {
                    runnable[0] =
                        new Runnable() {
                          @Override
                          public void run() {
                            mapErrorWasShown.remove(error);
                            conLayCustomSnackBarAlert.setVisibility(GONE);
                          }
                        };
                  }
                });
      }

      // set text color based on theme
      TypedValue typedValue = new TypedValue();
      Resources.Theme theme = fragment.requireContext().getTheme();
      theme.resolveAttribute(R.attr.colorError, typedValue, true);
      @ColorInt final int textColor = typedValue.data;

      fragment
          .requireActivity()
          .runOnUiThread(
              new Runnable() {
                @Override
                public void run() {
                  textViewLabelAlert.setTextColor(textColor);
                  textViewActionAlert.setTextColor(textColor);
                }
              });

      // make error container visible
      if (fragment.isAdded()) {
        fragment
            .requireActivity()
            .runOnUiThread(
                new Runnable() {
                  @Override
                  public void run() {
                    adjustMarginsOfSnackBarDependingOnFragment(conLayCustomSnackBarAlert, fragment);
                    conLayCustomSnackBarAlert.setVisibility(View.VISIBLE);
                  }
                });
      }

      // set action with action "OK" and no dialog afterwards
      if (error.equals(Error.NO_INTERNET_CONNECTION.toString())
          || error.equals(Error.UPDATING_BOOKMARKS_CATEGORY_FAILED.toString())
          || error.equals(Error.ACCOUNT_COULD_NOT_BE_FOLLOWED.toString())) {
        if (fragment.isAdded()) {
          fragment
              .requireActivity()
              .runOnUiThread(
                  new Runnable() {
                    @Override
                    public void run() {
                      textViewLabelAlert.setText(textViewLabel);
                      textViewActionAlert.setText(textViewLabelAction);
                    }
                  });
        }

        textViewActionAlert.setOnClickListener(
            new View.OnClickListener() {
              @Override
              public void onClick(View view) {
                mapErrorWasShown.remove(error);

                if (fragment.isAdded()) {
                  fragment
                      .requireActivity()
                      .runOnUiThread(
                          new Runnable() {
                            @Override
                            public void run() {
                              conLayCustomSnackBarAlert.removeCallbacks(runnable[0]);
                              conLayCustomSnackBarAlert.setVisibility(GONE);
                            }
                          });
                }
              }
            });
      } else {
        // set labels with action "MORE INFO" and dialog afterwards
        if (fragment.isAdded()) {
          fragment
              .requireActivity()
              .runOnUiThread(
                  new Runnable() {
                    @Override
                    public void run() {
                      textViewLabelAlert.setText(textViewLabel);
                      textViewActionAlert.setText(textViewLabelAction);
                    }
                  });
        }

        textViewActionAlert.setOnClickListener(
            new View.OnClickListener() {
              @Override
              public void onClick(View view) {
                mapErrorWasShown.remove(error);
                showAlertDialog(fragment, alertText, error);

                if (fragment.isAdded()) {
                  fragment
                      .requireActivity()
                      .runOnUiThread(
                          new Runnable() {
                            @Override
                            public void run() {
                              conLayCustomSnackBarAlert.removeCallbacks(runnable[0]);
                              conLayCustomSnackBarAlert.setVisibility(GONE);
                            }
                          });
                }
              }
            });
      }

      // make custom snackbar visible and trigger postDelay
      if (fragment.isAdded()) {
        fragment
            .requireActivity()
            .runOnUiThread(
                new Runnable() {
                  @Override
                  public void run() {
                    conLayCustomSnackBarAlert.setVisibility(View.VISIBLE);
                    conLayCustomSnackBarAlert.postDelayed(runnable[0], 3000);
                  }
                });
      }
    }
  }

  /**
   * Adjusts the margins of the snackbar for showing an error depending on the fragment
   *
   * @param conLayCustomSnackBarAlert ConstraintLayout
   * @param fragment Fragment
   */
  private static void adjustMarginsOfSnackBarDependingOnFragment(
      ConstraintLayout conLayCustomSnackBarAlert, Fragment fragment) {
    if (conLayCustomSnackBarAlert == null || fragment == null) return;

    String fragmentName = fragment.getClass().getSimpleName();

    int marginBottom, marginHorizontal;
    CoordinatorLayout.LayoutParams layoutParams =
        (CoordinatorLayout.LayoutParams) conLayCustomSnackBarAlert.getLayoutParams();

    if (fragmentName.equals("FeedFragment")
        || fragmentName.equals("SearchFragment")
        || fragmentName.equals("CollectionsFragment")) {
      // calc margins depending on fragment with nav bar visible
      marginBottom = convertDpToPixel(112f, fragment.requireContext());
      marginHorizontal = convertDpToPixel(8f, fragment.requireContext());
    } else {
      // calc margins depending on fragment with nav bar not visible
      marginBottom = convertDpToPixel(8f, fragment.requireContext());
      marginHorizontal = convertDpToPixel(8f, fragment.requireContext());
    }

    // adjust margins
    layoutParams.setMargins(marginHorizontal, 0, marginHorizontal, marginBottom);
    conLayCustomSnackBarAlert.setLayoutParams(layoutParams);
  }

  /**
   * This method converts dp unit to equivalent pixels, depending on device density. Source:
   * https://stackoverflow.com/questions/4605527/converting-pixels-to-dp
   *
   * @param dp int
   * @param context Context
   * @return int
   */
  public static int convertDpToPixel(float dp, Context context) {
    // return dp * (context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics
    // .DENSITY_DEFAULT);
    // getResources().getDisplayMetrics().density; // Convert the dps to pixels, based on density
    // scale
    // return (int) (input * scale + 0.5f);
    Resources r = context.getResources();
    return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
  }

  /**
   * Shows an alert dialog with a alertText over a fragment
   *
   * @param fragment Fragment
   * @param alertText String
   * @param error String
   */
  private static void showAlertDialog(
      final Fragment fragment, String alertText, final String error) {
    if (fragment != null && fragment.isAdded()) {
      try {
        // make alertText final
        final String finalAlertText = alertText;

        MaterialAlertDialogBuilder alertDialogBuilder;
        // create alertDialog
        alertDialogBuilder =
            new MaterialAlertDialogBuilder(fragment.requireContext())
                .setMessage(finalAlertText)
                .setPositiveButton(
                    fragment.requireContext().getResources().getString(R.string.okay),
                    new DialogInterface.OnClickListener() {
                      @Override
                      public void onClick(DialogInterface dialog, int which) {
                        mapErrorWasShown.remove(error);
                        assert finalAlertText != null;
                        if (finalAlertText.equals(
                            fragment
                                .requireContext()
                                .getResources()
                                .getString(R.string.post_not_available_anymore))) {
                          fragment.requireActivity().onBackPressed();
                        }
                      }
                    })
                // get the click outside the dialog to set the behaviour like the positive button
                // was clicked
                .setOnCancelListener(
                    new DialogInterface.OnCancelListener() {
                      @Override
                      public void onCancel(DialogInterface dialog) {
                        mapErrorWasShown.remove(error);
                      }
                    });

        final MaterialAlertDialogBuilder finalAlertDialogBuilder = alertDialogBuilder;

        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = fragment.requireContext().getTheme();
        theme.resolveAttribute(R.attr.colorAccent, typedValue, true);
        @ColorInt final int color = typedValue.data;

        // create alertDialog
        fragment
            .requireActivity()
            .runOnUiThread(
                new Runnable() {
                  @Override
                  public void run() {
                    AlertDialog alertDialog = finalAlertDialogBuilder.create();
                    alertDialog.setCanceledOnTouchOutside(true);
                    alertDialog.show();
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color);
                  }
                });
      } catch (Exception e) {
        Log.d("FragmentHelper", Log.getStackTraceString(e));
      }
    }
  }

  /**
   * Gets the colorId of the color with the resIdColor
   *
   * @param context Context
   * @param resIdColor int
   * @return int (ColorInt)
   */
  public static int getColorId(Context context, int resIdColor) {
    if (context == null) return 0;
    TypedValue typedValue = new TypedValue();
    Resources.Theme theme = context.getTheme();
    theme.resolveAttribute(resIdColor, typedValue, true);
    @ColorInt final int color = typedValue.data;
    return color;
  }

  /**
   * Validates a jsonStr and shows error message in snackBar if error was found
   *
   * @param jsonStr String
   * @param fragment Fragment
   */
  public static boolean checkIfJsonStrIsValid(String jsonStr, Fragment fragment) {
    if (fragment != null) {
      if (jsonStr == null || (jsonStr.startsWith("<!DOCTYPE html>") || jsonStr.isEmpty())) {
        throwErrorWhenFragmentIsNotFeedFragment(fragment);
        return false;
      } else {
        // no errors
        return true;
      }
    }
    return false;
  }

  private static void throwErrorWhenFragmentIsNotFeedFragment(Fragment fragment) {
    if (fragment.getTag() != null && !fragment.getTag().equals("FeedFragment")) {
      FragmentHelper.notifyUserOfProblem(fragment, Error.SOMETHINGS_WRONG);
    }
  }

  /**
   * Returns true, if theme is darkTheme
   *
   * @param context Context
   * @return boolean
   */
  public static boolean getThemeIsDarkTheme(Context context) {
    if (context == null) return false;

    // get the amount of columns from settings
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    if (preferences != null) {
      if (preferences.contains("darkMode")) {
        return preferences.getBoolean("darkMode", false);
      }
    }

    return false;
  }

  /**
   * Shows a dialog with information about followed accounts and amount of bookmarks
   *
   * @param fragment Fragment
   */
  public static void showStatisticsDialog(Fragment fragment) {
    // get statistics
    int amountFollowedAccounts = StorageHelper.amountFollowedAccounts(fragment.getContext());
    int amountBookmarks = StorageHelper.amountBookmarks(fragment.getContext());

    // create message
    String message =
        "Followed Accounts: " + amountFollowedAccounts + "\nBookmarks: " + amountBookmarks;

    // create alertDialog
    MaterialAlertDialogBuilder alertDialogBuilder;
    alertDialogBuilder =
        new MaterialAlertDialogBuilder(fragment.requireContext())
            .setTitle("Statistics")
            .setMessage(message)
            .setPositiveButton(
                fragment.requireContext().getResources().getString(R.string.okay),
                new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                  }
                });

    final MaterialAlertDialogBuilder finalAlertDialogBuilder = alertDialogBuilder;

    // change text color based on theme
    TypedValue typedValue = new TypedValue();
    Resources.Theme theme = fragment.requireContext().getTheme();
    theme.resolveAttribute(R.attr.colorAccent, typedValue, true);
    @ColorInt final int color = typedValue.data;

    // create alertDialog
    fragment
        .requireActivity()
        .runOnUiThread(
            new Runnable() {
              @Override
              public void run() {
                AlertDialog alertDialog = finalAlertDialogBuilder.create();
                alertDialog.setCanceledOnTouchOutside(true);
                alertDialog.show();
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color);
              }
            });
  }

  /**
   * Returns boolean whether advertising string should be added to clipboard text
   *
   * @param fragment Fragment
   * @return boolean
   */
  public static boolean addAdvertisingStringToClipboard(Fragment fragment) {
    SharedPreferences preferences =
        PreferenceManager.getDefaultSharedPreferences(fragment.requireContext());
    if (preferences != null) {
      return preferences.getBoolean(
          fragment.requireContext().getResources().getString(R.string.advertising_string_clipboard),
          false);
    }
    return false;
  }

  /**
   * Creates collections from bookmarks category and adds them to listCollectionsBookmarked
   *
   * @param context Context
   * @return List<Collection>
   */
  public static List<Collection> createCollectionsFromBookmarks(Context context) {
    // initialize lists
    List<Collection> listCollectionsBookmarked = new ArrayList<>();
    List<String> listCategories = new ArrayList<>();

    // read Bookmarks from Storage
    List<Post> listAllPostsBookmarked = readAllBookmarksFromStorage(context);

    // first create collection "All"
    if (listAllPostsBookmarked != null && !listAllPostsBookmarked.isEmpty()) {

      // add first bookmark as thumbnail
      listCollectionsBookmarked.add(
          new Collection(
              "All",
              listAllPostsBookmarked.get(0).getImageUrlThumbnail(),
              listAllPostsBookmarked.get(0).getImageThumbnail()));

      // then create all categories from all bookmarks category
      if (!listAllPostsBookmarked.isEmpty()) {
        for (Post post : listAllPostsBookmarked) {
          if (post.getCategory() != null && !listCategories.contains(post.getCategory())) {

            // create new collection
            listCollectionsBookmarked.add(
                new Collection(
                    post.getCategory(), post.getImageUrlThumbnail(), post.getImageThumbnail()));

            // add category to
            listCategories.add(post.getCategory());
          }
        }
      }
    }

    return listCollectionsBookmarked;
  }

  /**
   * Retrieves all bookmarks from storage
   *
   * @param context Context
   * @return ArrayList<Post>
   */
  private static ArrayList<Post> readAllBookmarksFromStorage(Context context) {
    return StorageHelper.readPostsFromInternalStorage(context, StorageHelper.FILENAME_BOOKMARKS);
  }

  /**
   * Retrieves all posts of specific collection (category)
   *
   * @param category String
   * @param context Context
   * @return List<Post>
   */
  public static List<Post> getAllBookmarkedPostsOfCollection(
      final String category, Context context) {
    // initialize listPostsInCollectionBookmarked with bookmarks from storage
    List<Post> listPostsInCollectionBookmarked = readAllBookmarksFromStorage(context);

    if (listPostsInCollectionBookmarked != null) {
      // show all bookmarks if category is null or show specific category
      if (!category.equals("All")) {
        // get all posts with specific category
        listPostsInCollectionBookmarked =
            listPostsInCollectionBookmarked.stream()
                .filter(post -> (post.getCategory() != null) && post.getCategory().equals(category))
                .collect(Collectors.toList());
      }
    }

    return listPostsInCollectionBookmarked;
  }

  /**
   * Returns true, if there is a collection with the name of the String category
   *
   * @param category String
   * @param context Context
   * @return boolean
   */
  public static boolean collectionWithNameDoesExist(final String category, Context context) {
    List<Collection> listCollections = FragmentHelper.createCollectionsFromBookmarks(context);

    // get all collections with specific category
    listCollections =
        listCollections.stream()
            .filter(
                collection -> collection.getName() != null && collection.getName().equals(category))
            .collect(Collectors.toList());

    return !listCollections.isEmpty();
  }

  /**
   * Returns true, if there are already collections
   *
   * @param context Context
   * @return boolean
   */
  public static boolean collectionsAlreadyExist(Context context) {
    List<Collection> listCollections = FragmentHelper.createCollectionsFromBookmarks(context);
    return listCollections.size() > 1;
  }

  /**
   * Start intent to visit the project page on Github
   *
   * @param context Context
   */
  public static void openProjectOnGithub(Context context) {
    final String githubUrl = "https://github.com/amnesica/feedsta";
    Intent rateIntent = getIntentForUrl(githubUrl, context);
    context.startActivity(rateIntent);
  }

  /**
   * Helper method to start intent
   *
   * @param url String
   * @param context Context
   * @return Intent
   */
  private static Intent getIntentForUrl(final String url, Context context) {
    Intent intent =
        new Intent(
            Intent.ACTION_VIEW,
            Uri.parse(String.format("%s?id=%s", url, context.getPackageName())));
    int flags = Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_MULTIPLE_TASK;
    flags |= Intent.FLAG_ACTIVITY_NEW_DOCUMENT;
    intent.addFlags(flags);
    return intent;
  }

  /**
   * Shows a toast on main thread with a message
   *
   * @param message String
   * @param activity Activity
   * @param context Context
   */
  public static void showToast(final String message, Activity activity, final Context context) {
    if (activity != null && context != null) {
      activity.runOnUiThread(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }
  }

  /**
   * Returns a base64 encoded string of an image from url. Hint: Method exists in StorageHelper as
   * well, but cannot be used here because this would be an async task call inside an async task
   * call, hence the "duplicated" method!
   *
   * @param url String
   * @return String
   * @throws Exception Exception
   */
  public static String getBase64EncodedImage(String url) throws Exception {
    if (url == null) return null;

    URL imageUrl = new URL(url);
    URLConnection ucon = imageUrl.openConnection();
    InputStream is = ucon.getInputStream();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    int read;
    while ((read = is.read(buffer, 0, buffer.length)) != -1) {
      baos.write(buffer, 0, read);
    }
    baos.flush();
    return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
  }

  /**
   * Creates an SpannableStringBuilder with clickable links to accounts and hashtags. A link is
   * generated for every word which contains an "@" or "#" character. If something went wrong the
   * input text is returned
   *
   * @param text String
   * @param fragment Fragment
   * @return SpannableStringBuilder
   */
  public static SpannableStringBuilder createSpannableStringWithClickableLinks(
      String text, final Fragment fragment) {
    if (text == null) return null;

    SpannableStringBuilder ssb = new SpannableStringBuilder();
    final Pattern patternAccount = Pattern.compile("(@([A-Za-zÀ-ÿ0-9._]*[A-Za-zÀ-ÿ0-9_|$]))");
    final Pattern patternHashtag = Pattern.compile("(#([A-Za-zÀ-ÿ0-9_]*))");

    try {
      // replace every word containing "@" or "#" with clickable link
      for (String word : text.split(" ")) {
        if (word.contains("@")) {
          createClickableLinkWithAtChar(fragment, ssb, patternAccount, word);
        } else if (word.contains("#")) {
          createClickableLinkWithHashtagChar(fragment, ssb, patternHashtag, word);
        } else {
          // insert normal word without link
          ssb.append(word);
        }
        ssb.append(" ");
      }
      return ssb;
    } catch (Exception e) {
      Log.d("FragmentHelper", Log.getStackTraceString(e));
      // return normal text if something went wrong
      return new SpannableStringBuilder(text);
    }
  }

  private static void createClickableLinkWithHashtagChar(
      Fragment fragment, SpannableStringBuilder ssb, Pattern patternHashtag, String word) {
    // add link to hashtag
    int indexStartHashtagName = 0;
    int indexEndHashtagName = 0;

    Matcher matcher = patternHashtag.matcher(word);
    while (matcher.find()) {
      indexStartHashtagName = matcher.start();
      indexEndHashtagName = matcher.end();
    }

    // +1 because "#" should be omitted
    final String hashtagName = word.substring(indexStartHashtagName + 1, indexEndHashtagName);
    SpannableString spannableString = new SpannableString(word);

    ClickableSpan clickableSpan = createClickableSpanHashtag(fragment, hashtagName);

    spannableString.setSpan(
        clickableSpan, indexStartHashtagName, indexEndHashtagName, SPAN_EXCLUSIVE_EXCLUSIVE);
    ssb.append(spannableString);
  }

  private static void createClickableLinkWithAtChar(
      Fragment fragment, SpannableStringBuilder ssb, Pattern patternAccount, String word) {
    // add link to account
    int indexStartAccountName = 0;
    int indexEndAccountName = 0;

    Matcher matcher = patternAccount.matcher(word);
    while (matcher.find()) {
      indexStartAccountName = matcher.start();
      indexEndAccountName = matcher.end();
    }

    // +1 because "@" should be omitted
    final String accountName = word.substring(indexStartAccountName + 1, indexEndAccountName);
    SpannableString spannableString = new SpannableString(word);

    ClickableSpan clickableSpan = createClickableSpanAccount(fragment, accountName);

    spannableString.setSpan(
        clickableSpan, indexStartAccountName, indexEndAccountName, SPAN_EXCLUSIVE_EXCLUSIVE);
    ssb.append(spannableString);
  }

  @NonNull
  private static ClickableSpan createClickableSpanHashtag(Fragment fragment, String hashtagName) {
    return new ClickableSpan() {
      @SuppressLint("ResourceType")
      @Override
      public void updateDrawState(@NonNull TextPaint ds) {
        super.updateDrawState(ds);
        // underline text
        ds.setUnderlineText(true);

        // set color to textColorSecondary
        int secondaryColor =
            MaterialColors.getColor(
                fragment.requireContext(), android.R.attr.textColorSecondary, Color.BLUE);
        ds.setColor(secondaryColor);
      }

      @Override
      public void onClick(@NonNull View view) {
        view.invalidate();
        goToHashtagFragment(hashtagName, fragment);
      }
    };
  }

  private static void goToHashtagFragment(String hashtagName, Fragment fragment) {
    // new hashtagFragment
    HashtagFragment hashtagFragment = HashtagFragment.newInstance(hashtagName);

    // add fragment to container
    FragmentHelper.addFragmentToContainer(
        hashtagFragment, fragment.requireActivity().getSupportFragmentManager());
  }

  @NonNull
  private static ClickableSpan createClickableSpanAccount(Fragment fragment, String accountName) {
    return new ClickableSpan() {
      @SuppressLint("ResourceType")
      @Override
      public void updateDrawState(@NonNull TextPaint ds) {
        super.updateDrawState(ds);
        // underline text
        ds.setUnderlineText(true);

        // set color to textColorSecondary
        int secondaryColor =
            MaterialColors.getColor(
                fragment.requireContext(), android.R.attr.textColorSecondary, Color.BLUE);
        ds.setColor(secondaryColor);
      }

      @Override
      public void onClick(@NonNull View view) {
        view.invalidate();
        goToProfileFragment(accountName, fragment);
      }
    };
  }

  public static void goToProfileFragment(@NonNull String accountName, Fragment fragment) {
    // new profileFragment
    ProfileFragment profileFragment = ProfileFragment.newInstance(accountName);

    // add fragment to container
    FragmentHelper.addFragmentToContainer(
        profileFragment, fragment.requireActivity().getSupportFragmentManager());
  }
}
