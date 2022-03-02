package com.amnesica.feedsta.helper;

import static android.view.View.GONE;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

import com.amnesica.feedsta.Post;
import com.amnesica.feedsta.R;
import com.amnesica.feedsta.fragments.FeedFragment;
import com.amnesica.feedsta.models.Collection;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Predicate;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * Helper class for fragments with various methods
 */
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
     * Sets up the toolbar of the weakReference fragment with a backButton and a clickListener
     *
     * @param toolbar           toolbar
     * @param fragmentReference weakReference of fragment
     */
    public static void setupToolbarWithBackButton(Toolbar toolbar, WeakReference fragmentReference) {
        final Fragment weakReferenceFragment = (Fragment) fragmentReference.get();
        if (weakReferenceFragment != null) {
            if (getThemeIsDarkTheme(weakReferenceFragment.getContext())) {
                toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
            } else {
                toolbar.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
            }
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    weakReferenceFragment.requireActivity().onBackPressed();
                }
            });
        }
    }

    /**
     * Returns the top fragment from backStack. If FragmentManager is null, return currently visible fragment
     *
     * @param fm FragmentManager
     * @return top fragment from backStack, or currently visible fragment
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
        // fm is null
        return null;
    }

    /**
     * Loads and shows fragmentToShow from FragmentManager into container
     *
     * @param fragmentToShow fragmentToShow
     * @param fm             FragmentManager
     * @return true, if successful
     */
    public static Boolean loadAndShowFragment(Fragment fragmentToShow, FragmentManager fm) {
        if (fragmentToShow != null) {
            // get active fragment to hide
            Fragment active = getTopFragment(fm);
            if (active != null) {
                // show fragment
                fm.beginTransaction()
                        .hide(active)
                        .show(fragmentToShow)
                        .addToBackStack(fragmentToShow.getClass().getSimpleName())
                        .commit();
                return true;
            }
            return false;
        }
        return false;
    }

    /**
     * Adds a fragment to the container and to the backStack
     *
     * @param fragmentToAdd fragmentToAdd
     * @param fm            FragmentManager
     */
    public static void addFragmentToContainer(Fragment fragmentToAdd, FragmentManager fm) {
        if (fragmentToAdd != null && fm != null) {
            // get unique tag with class name of fragmentToAdd and incrementing counter
            String tag = fragmentToAdd.getClass().getSimpleName() + FragmentHelper.getTransactionCounter();
            // get active fragment
            Fragment active = getTopFragment(fm);

            try {
                // add fragment to container
                fm.beginTransaction()
                        .hide(active)
                        .add(R.id.main_container, fragmentToAdd, tag)
                        .addToBackStack(tag)
                        .commit();
            } catch (Exception e) {
                Log.d("FragmentHelper", Log.getStackTraceString(e));
            }
        }
    }

    /**
     * Sets highlighted element in navigation bar
     *
     * @param activity activity to get nav_view
     * @param resource resource to set highlighted
     */
    public static void setBottomNavViewSelectElem(FragmentActivity activity, int resource) {
        BottomNavigationView bottomNavigationView = Objects.requireNonNull(activity).findViewById(R.id.nav_view);
        Menu menu = bottomNavigationView.getMenu();
        menu.findItem(resource).setCheckable(true);
        menu.findItem(resource).setChecked(true);
    }

    /**
     * Notifies the user that no account or not all but some accounts could be queried and
     * shows a dialog. Method handles the NOT_ALL_ACCOUNTS_COULD_BE_QUERIED error
     *
     * @param fragment              Fragment
     * @param actualFetchedAccounts int
     * @param amountAccountsToFetch int
     */
    public static void notifyUserOfIncompleteFetchProblem(final Fragment fragment, int actualFetchedAccounts, int amountAccountsToFetch) {
        final Enum<Error> error = Error.NOT_ALL_ACCOUNTS_COULD_BE_QUERIED;

        if (!mapErrorWasShown.containsKey(error.toString())) {

            // put fragment into map to eliminate error duplicates
            mapErrorWasShown.put(error.toString(), true);

            // text for dialog
            String alertText = null;

            // text for label at snackbar's left
            String textViewLabel = null;

            // label for action of snackbar
            String textViewLabelAction = null;

            // fixing "Fragment not attached to a context"
            if (fragment != null && fragment.getContext() != null) {
                if (actualFetchedAccounts > 0) {
                    // not all accounts cound be queried
                    textViewLabel = fragment.requireContext().getResources().getString(R.string.not_all_accounts_could_be_queried) + " " +
                            "(" + actualFetchedAccounts + "/" + amountAccountsToFetch + ")";
                    // get proper response
                    alertText = fragment.requireContext().getResources().getString(R.string.not_all_accounts_could_be_queried_more_info);
                } else {
                    // nothing could be querid
                    textViewLabel = fragment.requireContext().getResources().getString(R.string.no_account_could_be_queried) + " " +
                            "(" + actualFetchedAccounts + "/" + amountAccountsToFetch + ")";
                    // get proper response
                    alertText = fragment.requireContext().getResources().getString(R.string.no_account_could_be_queried_more_info);
                }

                textViewLabelAction = fragment.requireContext().getResources().getString(R.string.more_info);

                // show alert as custom snackBar
                showCustomSnackBarForAlert(fragment, alertText, error.toString(), textViewLabel, textViewLabelAction);
            }
        }
    }

    /**
     * Notifies the user that there is a problem and shows a dialog.
     * Hint: NOT_ALL_ACCOUNTS_COULD_BE_QUERIED is supposed to be handled
     * in notifyUserOfIncompleteFetchProblem()!
     *
     * @param fragment calling fragment
     * @param error    error
     */
    public static void notifyUserOfProblem(final Fragment fragment, final Enum<Error> error) {
        if (!mapErrorWasShown.containsKey(error.toString())) {

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
                    alertText = fragment.requireContext().getResources().getString(R.string.no_internet_connection);
                    textViewLabel = fragment.requireContext().getResources().getString(R.string.no_internet_connection);
                    textViewLabelAction = fragment.requireContext().getResources().getString(R.string.okay);
                } else if (errorMessage.equals(Error.UPDATING_BOOKMARKS_CATEGORY_FAILED.toString())) {
                    alertText = fragment.requireContext().getResources().getString(R.string.updating_bookmarked_post_category_failed);
                    textViewLabel = fragment.requireContext().getResources().getString(R.string.updating_bookmarked_post_category_failed_label);
                    textViewLabelAction = fragment.requireContext().getResources().getString(R.string.okay);
                } else if (errorMessage.equals(Error.SOMETHINGS_WRONG.toString())) {
                    alertText = fragment.requireContext().getResources().getString(R.string.something_went_wrong_more_info);
                    textViewLabel = fragment.requireContext().getResources().getString(R.string.something_has_gone_wrong);
                    textViewLabelAction = fragment.requireContext().getResources().getString(R.string.more_info);
                } else if (errorMessage.equals(Error.POST_NOT_AVAILABLE_ANYMORE.toString())) {
                    alertText = fragment.requireContext().getResources().getString(R.string.post_not_available_anymore);
                    textViewLabel = fragment.requireContext().getResources().getString(R.string.post_not_available_anymore_label);
                    textViewLabelAction = fragment.requireContext().getResources().getString(R.string.more_info);
                } else if (errorMessage.equals(Error.ACCOUNT_COULD_NOT_BE_FOLLOWED.toString())) {
                    alertText = fragment.requireContext().getResources().getString(R.string.account_could_not_be_followed);
                    textViewLabel = fragment.requireContext().getResources().getString(R.string.account_could_not_be_followed);
                    textViewLabelAction = fragment.requireContext().getResources().getString(R.string.okay);
                }

                // show alert as custom snackBar
                showCustomSnackBarForAlert(fragment, alertText, error.toString(), textViewLabel, textViewLabelAction);
            }
        }
    }

    /**
     * Shows a custom snackBar to show option to save post to collection above the bottom navigation view
     *
     * @param fragment  Fragment
     * @param alertText alertText
     * @param error     Error
     */
    private static void showCustomSnackBarForAlert(final Fragment fragment, final String alertText, final String error,
                                                   final String textViewLabel, final String textViewLabelAction) {
        if (fragment != null && fragment.getContext() != null) {
            final ConstraintLayout conLayCustomSnackBarAlert = fragment.requireActivity().findViewById(R.id.conLayCustomSnackBarAlert);
            final TextView textViewLabelAlert = fragment.requireActivity().findViewById(R.id.textViewLabelAlert);
            final TextView textViewActionAlert = fragment.requireActivity().findViewById(R.id.textViewActionAlert);

            final Runnable[] runnable = new Runnable[1];

            // Fixed: Can't create handler inside thread Thread[AsyncTask #2,5,main]
            // that has not called Looper.prepare()
            if (fragment.isAdded()) {
                fragment.requireActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final Handler handler = new Handler();
                    }
                });
            }

            if (fragment.isAdded()) {
                fragment.requireActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        runnable[0] = new Runnable() {
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
            theme.resolveAttribute(R.attr.colorAccent, typedValue, true);
            @ColorInt final int textColor = typedValue.data;

            fragment.requireActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textViewLabelAlert.setTextColor(textColor);
                    textViewActionAlert.setTextColor(textColor);
                }
            });

            // set background color based on theme
            theme.resolveAttribute(R.attr.colorPrimary, typedValue, true);
            @ColorInt final int backgroundColor = typedValue.data;

            if (fragment.isAdded()) {
                fragment.requireActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        conLayCustomSnackBarAlert.setBackgroundColor(backgroundColor);
                    }
                });
            }

            // set action with action "OK" and no dialog afterwards
            if (error.equals(Error.NO_INTERNET_CONNECTION.toString()) ||
                    error.equals(Error.UPDATING_BOOKMARKS_CATEGORY_FAILED.toString()) ||
                    error.equals(Error.ACCOUNT_COULD_NOT_BE_FOLLOWED.toString())) {
                if (fragment.isAdded()) {
                    fragment.requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textViewLabelAlert.setText(textViewLabel);
                            textViewActionAlert.setText(textViewLabelAction);
                        }
                    });
                }

                textViewActionAlert.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mapErrorWasShown.remove(error);

                        if (fragment.isAdded()) {
                            fragment.requireActivity().runOnUiThread(new Runnable() {
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
                    fragment.requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textViewLabelAlert.setText(textViewLabel);
                            textViewActionAlert.setText(textViewLabelAction);
                        }
                    });
                }

                textViewActionAlert.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mapErrorWasShown.remove(error);
                        showAlertDialog(fragment, alertText, error);

                        if (fragment.isAdded()) {
                            fragment.requireActivity().runOnUiThread(new Runnable() {
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
                fragment.requireActivity().runOnUiThread(new Runnable() {
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
     * Shows an alert dialog with a alertText over a fragment
     *
     * @param fragment  fragment to show alert on
     * @param alertText text of alert
     * @param error     error to notify user about
     */
    private static void showAlertDialog(final Fragment fragment, String alertText, final String error) {
        if (fragment != null && fragment.isAdded()) {
            try {
                // make alertText final
                final String finalAlertText = alertText;

                AlertDialog.Builder alertDialogBuilder;
                // create alertDialog
                alertDialogBuilder = new AlertDialog.Builder(fragment.requireContext())
                        .setMessage(finalAlertText)
                        .setPositiveButton(fragment.requireContext().getResources().getString(R.string.okay), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mapErrorWasShown.remove(error);
                                assert finalAlertText != null;
                                if (finalAlertText.equals(fragment.requireContext().getResources().getString(R.string.post_not_available_anymore))) {
                                    fragment.requireActivity().onBackPressed();
                                }
                            }
                        })
                        // get the click outside the dialog to set the behaviour like the positive button was clicked
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                mapErrorWasShown.remove(error);
                            }
                        });

                final AlertDialog.Builder finalAlertDialogBuilder = alertDialogBuilder;

                TypedValue typedValue = new TypedValue();
                Resources.Theme theme = fragment.requireContext().getTheme();
                theme.resolveAttribute(R.attr.colorAccent, typedValue, true);
                @ColorInt final int color = typedValue.data;

                // create alertDialog
                fragment.requireActivity().runOnUiThread(new Runnable() {
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
     * Validates a jsonStr and shows error message in snackBar if error was found
     *
     * @param jsonStr  String to check
     * @param fragment calling Fragment
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean checkIfJsonStrIsValid(String jsonStr, Fragment fragment) {
        if (jsonStr != null && fragment != null) {
            if (jsonStr.startsWith("<!DOCTYPE html>")) {

                // only throw error when fragment is not FeedFragment
                if (fragment.getTag() != null && !fragment.getTag().equals("FeedFragment")) {
                    FragmentHelper.notifyUserOfProblem(fragment, Error.SOMETHINGS_WRONG);
                }

                return false;
            } else {
                // no errors
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true, if theme is DarkTheme
     *
     * @param context context
     * @return true, if theme is DarkTheme
     */
    public static boolean getThemeIsDarkTheme(Context context) {
        if (context != null) {
            // get the amount of columns from settings
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            if (preferences != null) {
                if (preferences.contains("darkMode")) {
                    return preferences.getBoolean("darkMode", false);
                }
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
        String message = "Followed Accounts: " + amountFollowedAccounts + "\nBookmarks: " + amountBookmarks;

        // create alertDialog
        AlertDialog.Builder alertDialogBuilder;
        alertDialogBuilder = new AlertDialog.Builder(fragment.requireContext())
                .setTitle("Statistics")
                .setMessage(message)
                .setPositiveButton(fragment.requireContext().getResources().getString(R.string.okay), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        final AlertDialog.Builder finalAlertDialogBuilder = alertDialogBuilder;

        // change text color based on theme
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = fragment.requireContext().getTheme();
        theme.resolveAttribute(R.attr.colorAccent, typedValue, true);
        @ColorInt final int color = typedValue.data;

        // create alertDialog
        fragment.requireActivity().runOnUiThread(new Runnable() {
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
     * Returns boolean, whether advertising string should be added to clipboard text
     *
     * @param fragment Fragment
     * @return boolean
     */
    public static boolean addAdvertisingStringToClipboard(Fragment fragment) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(fragment.requireContext());
        if (preferences != null) {
            return preferences.getBoolean(fragment.requireContext().getResources().getString(R.string.advertising_string_clipboard), false);
        }
        return false;
    }

    /**
     * Creates collections from bookmarks category and adds them to listCollectionsBookmarked
     *
     * @param context context
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
            listCollectionsBookmarked.add(new Collection("All", listAllPostsBookmarked.get(0).getImageUrlThumbnail(),
                    listAllPostsBookmarked.get(0).getImageThumbnail()));

            // then create all categories from all bookmarks category
            if (!listAllPostsBookmarked.isEmpty()) {
                for (Post post : listAllPostsBookmarked) {
                    if (post.getCategory() != null && !listCategories.contains(post.getCategory())) {

                        // create new collection
                        listCollectionsBookmarked.add(new Collection(post.getCategory(), post.getImageUrlThumbnail(),
                                post.getImageThumbnail()));

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
     * @param context context
     * @return ArrayList<Post>
     */
    private static ArrayList<Post> readAllBookmarksFromStorage(Context context) {
        return StorageHelper.readPostsFromInternalStorage(context, StorageHelper.filename_bookmarks);
    }

    /**
     * Retrieves all posts of specific collection
     *
     * @param category category in post
     * @param context  context
     * @return List<Post>
     */
    public static List<Post> getAllBookmarkedPostsOfCollection(final String category, Context context) {
        // initialize listPostsInCollectionBookmarked with bookmarks from storage
        List<Post> listPostsInCollectionBookmarked = readAllBookmarksFromStorage(context);

        if (listPostsInCollectionBookmarked != null) {
            // show all bookmarks if category is null or show specific category
            if (!category.equals("All")) {
                // get all posts with specific category
                listPostsInCollectionBookmarked = Stream.of(listPostsInCollectionBookmarked).filter(new Predicate<Post>() {
                    @Override
                    public boolean test(Post c) {
                        return (c.getCategory() != null) && c.getCategory().equals(category);
                    }
                }).collect(Collectors.<Post>toList());
            }
        }

        return listPostsInCollectionBookmarked;
    }

    /**
     * Returns true, if there is a collection with the name of the String category
     *
     * @param category String category
     * @param context  context
     * @return boolean
     */
    public static boolean collectionWithNameDoesExist(final String category, Context context) {
        List<Collection> listCollections = FragmentHelper.createCollectionsFromBookmarks(context);

        // get all collections with specific category
        listCollections = Stream.of(listCollections).filter(new Predicate<Collection>() {
            @Override
            public boolean test(Collection c) {
                return (c.getName() != null) && c.getName().equals(category);
            }
        }).collect(Collectors.<Collection>toList());

        return !listCollections.isEmpty();
    }

    /**
     * Returns true, if there are already collection
     *
     * @param context context
     * @return boolean
     */
    public static boolean collectionsAlreadyExist(Context context) {
        List<Collection> listCollections = FragmentHelper.createCollectionsFromBookmarks(context);
        return listCollections.size() > 1;
    }


    /**
     * Sets new category from newCategory to post and saves updated post in storage
     *
     * @param newCategory new category
     */
    public static boolean setNewCategoryToPost(String newCategory, Post post, Context context, Fragment fragment) {
        // set category to post
        if (post != null) {
            post.setCategory(newCategory);

            try {
                // save post in storage
                return StorageHelper.updateBookmarkCategoryInStorage(post, context);
            } catch (IOException e) {
                Log.d("FragmentHelper", Log.getStackTraceString(e));
                if (fragment != null) {
                    FragmentHelper.notifyUserOfProblem(fragment, Error.UPDATING_BOOKMARKS_CATEGORY_FAILED);
                }
                return false;
            }
        }
        return false;
    }

    /**
     * Start with rating the app. Determine if the Play Store is installed on the device first.
     *
     * @param context Context
     */
    public static void rateApp(Context context) {
        try {
            Intent rateIntent = rateIntentForUrl("market://details", context);
            context.startActivity(rateIntent);
        } catch (ActivityNotFoundException e) {
            Intent rateIntent = rateIntentForUrl("https://play.google.com/store/apps/details", context);
            context.startActivity(rateIntent);
        }
    }

    /**
     * Helper method for rateApp(context) to start intent
     *
     * @param url     String
     * @param context Context
     * @return Intent
     */
    private static Intent rateIntentForUrl(String url, Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format("%s?id=%s", url, context.getPackageName())));
        int flags = Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_MULTIPLE_TASK;
        flags |= Intent.FLAG_ACTIVITY_NEW_DOCUMENT;
        intent.addFlags(flags);
        return intent;
    }

    /**
     * Show a toast on main thread with a message
     *
     * @param message  String
     * @param activity Activity
     * @param context  Context
     */
    public static void showToast(final String message, Activity activity, final Context context) {
        if (activity != null && context != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Returns a base64 encoded string of an image from url.
     * Hint: Method exists in StorageHelper as well, but
     * cannot be used here because this would be an async task
     * call inside an async task call, hence the "duplicated"
     * method!
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
        int read = 0;
        while ((read = is.read(buffer, 0, buffer.length)) != -1) {
            baos.write(buffer, 0, read);
        }
        baos.flush();
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
    }
}