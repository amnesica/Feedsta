package com.amnesica.feedsta.fragments;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.amnesica.feedsta.Account;
import com.amnesica.feedsta.R;
import com.amnesica.feedsta.adapter.ListAdapterSearch;
import com.amnesica.feedsta.helper.CustomComparatorAccount;
import com.amnesica.feedsta.helper.Error;
import com.amnesica.feedsta.helper.FragmentHelper;
import com.amnesica.feedsta.helper.NetworkHandler;
import com.amnesica.feedsta.helper.StorageHelper;
import com.amnesica.feedsta.interfaces.AdapterCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Fragment for displaying followed accounts
 */
@SuppressWarnings("deprecation")
public class FollowingFragment extends Fragment implements AdapterCallback {
    
    // view stuff
    private ListView listViewFollowedAccounts;
    private TextView textNoAccounts;
    private SwipeRefreshLayout swipeRefreshLayout;

    // list with followed accounts
    private ArrayList<Object> followedAccounts;
    private ArrayList<Account> listAccountFailedRefresh;

    // boolean something went wrong
    private boolean somethingWentWrong = false;

    // new list of followed accounts with updated thumbnailUrls
    private ArrayList<Account> followedAccountsUpdatedToAccountList;

    public FollowingFragment() {
        //  Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_following, container, false);

        // setup toolbar
        Toolbar toolbar = v.findViewById(R.id.toolbar);
        setupToolbar(toolbar);

        // setup layout stuff
        swipeRefreshLayout = v.findViewById(R.id.swipeRefreshFollowing);
        setupSwipeRefreshLayout(swipeRefreshLayout);

        listViewFollowedAccounts = v.findViewById(R.id.listFollowedAccounts);
        textNoAccounts = v.findViewById(R.id.textNoAccounts);

        // read accounts from internal storage
        readDataFromInternalStorage();

        // setup ListView with Adapter
        setupListViewWithAdapterAndOnClickListener(listViewFollowedAccounts);

        return v;
    }

    /**
     * Sets up the listView of followed accounts and sets the OnClickListener to go to a specific profile
     * @param lvFollowedAccounts ListView
     */
    private void setupListViewWithAdapterAndOnClickListener(ListView lvFollowedAccounts) {
        if (followedAccounts != null && !followedAccounts.isEmpty()) {
            checkInternetConnectionAndUpdateThumbnailUrl(false);
        }

        // get list item in list view
        lvFollowedAccounts.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // current clicked on account
                Account accountToView = (Account) followedAccounts.get(position);

                // new profileFragment
                ProfileFragment profileFragment = ProfileFragment.newInstance(accountToView);

                // add fragment to container
                FragmentHelper.addFragmentToContainer(profileFragment, requireActivity().getSupportFragmentManager());
            }
        });
    }

    /**
     * Sets up the swipeRefreshLayout
     *
     * @param swipeRefreshLayout swipeRefreshLayout
     */
    private void setupSwipeRefreshLayout(final SwipeRefreshLayout swipeRefreshLayout) {
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                readDataFromInternalStorage();
                if (followedAccounts != null && !followedAccounts.isEmpty()) {
                    checkInternetConnectionAndUpdateThumbnailUrl(true);
                } else {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        });
    }

    /**
     * Sets up the toolbar with NavigationOnClickListener and MenuItemClickListener
     *
     * @param toolbar toolbar
     */
    private void setupToolbar(Toolbar toolbar) {
        toolbar.setTitle(getResources().getString(R.string.toolbar_title_following));
        toolbar.inflateMenu(R.menu.menu_main);
        FragmentHelper.setupToolbarWithBackButton(toolbar, new WeakReference<>(FollowingFragment.this));
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.menu_info) {
                    FragmentHelper.loadAndShowFragment(requireActivity().getSupportFragmentManager().findFragmentByTag(AboutFragment.class.getSimpleName()),
                            requireActivity().getSupportFragmentManager());
                } else if (item.getItemId() == R.id.menu_action_statistics_dialog) {
                    FragmentHelper.showStatisticsDialog(FollowingFragment.this);
                } else if (item.getItemId() == R.id.menu_settings) {
                    FragmentHelper.loadAndShowFragment(requireActivity().getSupportFragmentManager().findFragmentByTag(SettingsHolderFragment.class.getSimpleName()),
                            requireActivity().getSupportFragmentManager());
                } else if (item.getItemId() == R.id.menu_exit) {
                    requireActivity().finish();
                }
                return false;
            }
        });
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        // set highlighted item on nav bar to "feed" and refresh followed accounts
        if (!hidden) {
            FragmentHelper.setBottomNavViewSelectElem(getActivity(), R.id.navigation_feed);
            readDataFromInternalStorage();

            // setup ListView with Adapter
            checkInternetConnectionAndUpdateThumbnailUrl(false);
        }
    }

    /**
     * Reads data from internal storage and adds all read accounts to list followedAccounts
     */
    private void readDataFromInternalStorage() {
        // get from storage
        try {
            ArrayList<Account> readAccountList;
            if (getContext() != null) {
                readAccountList = StorageHelper.readAccountsFromInternalStorage(requireContext());

                // initialize list
                if (followedAccounts == null) {
                    followedAccounts = new ArrayList<>();
                }

                // display list or show warning
                if (readAccountList != null) {

                    // clear list before adding all read accounts (no duplicates)
                    followedAccounts.clear();
                    followedAccounts.addAll(readAccountList);
                    listViewFollowedAccounts.setVisibility(View.VISIBLE);
                    textNoAccounts.setVisibility(View.GONE);

                    // invalidate view
                    listViewFollowedAccounts.invalidateViews();
                } else { // no accounts in list -> warning
                    listViewFollowedAccounts.setVisibility(View.GONE);
                    textNoAccounts.setVisibility(View.VISIBLE);
                    textNoAccounts.setText(requireContext().getResources().getString(R.string.no_followed_accounts));

                    // reset followedAccounts if readAccountsList is null
                    if (followedAccounts != null) {
                        followedAccounts.clear();
                    }
                }
            }
        } catch (Exception e) {
            Log.d("FollowingFragment", Log.getStackTraceString(e));
        }
    }

    /**
     * Removes an account from storage
     * @param account Account
     */
    @Override
    public void removeAccountFromStorage(final Account account) {
        AlertDialog.Builder alertDialogBuilder;
        // create alertDialog
        alertDialogBuilder = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.dialog_remove_accounts_title)
                .setMessage(R.string.dialog_remove_accounts_message)
                .setPositiveButton(getResources().getString(R.string.button_continue), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        boolean removingSuccessful;
                        // continue with remove operation
                        removingSuccessful = StorageHelper.removeAccountFromInternalStorage(account, requireContext());

                        if (!removingSuccessful) {
                            FragmentHelper.showToast(requireContext().getString(R.string.dialog_remove_accounts_failed), requireActivity(), requireContext());
                            return;
                        }

                        if (removingSuccessful) {
                            FragmentHelper.showToast(requireContext().getString(R.string.dialog_remove_accounts_successful), requireActivity(), requireContext());

                            // refresh followed accounts posts
                            readDataFromInternalStorage();
                        }
                    }
                })
                .setNegativeButton(getResources().getString(R.string.CANCEL), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (listAccountFailedRefresh != null) {
                            listAccountFailedRefresh.clear();
                            listAccountFailedRefresh = null;
                        }
                    }
                })

                // get the click outside the dialog to set the behaviour like the negative button was clicked
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        if (listAccountFailedRefresh != null) {
                            listAccountFailedRefresh.clear();
                            listAccountFailedRefresh = null;
                        }
                    }
                });

        final AlertDialog.Builder finalAlertDialogBuilder = alertDialogBuilder;

        // get color for button texts
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = requireContext().getTheme();
        theme.resolveAttribute(R.attr.colorAccent, typedValue, true);
        @ColorInt final int color = typedValue.data;

        // create alertDialog
        requireActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog alertDialog = finalAlertDialogBuilder.create();
                alertDialog.setCanceledOnTouchOutside(true);
                alertDialog.show();
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color);
                alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(color);
            }
        });
    }

    /**
     * Checks internet connection first and updates the thumbnail urls of the followed accounts
     * if bLoadUpdatedUrls is true. Shows dialog that this is a long running operation
     * @param bLoadUpdatedUrls boolean, if urls of thumbnails of accounts should be refreshed
     */
    private void checkInternetConnectionAndUpdateThumbnailUrl(boolean bLoadUpdatedUrls) {
        if (bLoadUpdatedUrls && followedAccounts != null && !followedAccounts.isEmpty()) {
            AlertDialog.Builder alertDialogBuilder;

            // create alertDialog
            alertDialogBuilder = new AlertDialog.Builder(requireContext())
                    .setTitle(getResources().getString(R.string.title_dialog_refresh_accounts))
                    .setMessage(getResources().getString(R.string.message_dialog_refresh_accounts))
                    .setPositiveButton(getResources().getString(R.string.button_continue), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // continue with refresh operation
                            new CheckConnectionAndSetUpAdapter(FollowingFragment.this, true).execute();
                        }
                    })
                    .setNegativeButton(getResources().getString(R.string.CANCEL), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // stop refreshing
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    })

                    // get the click outside the dialog to set the behaviour like the negative button was clicked
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            // stop refreshing
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    });

            final AlertDialog.Builder finalAlertDialogBuilder = alertDialogBuilder;

            // get color for button texts
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = requireContext().getTheme();
            theme.resolveAttribute(R.attr.colorAccent, typedValue, true);
            @ColorInt final int color = typedValue.data;

            // create alertDialog
            requireActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog alertDialog = finalAlertDialogBuilder.create();
                    alertDialog.setCanceledOnTouchOutside(true);
                    alertDialog.show();
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color);
                    alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(color);
                }
            });
        } else {
            new CheckConnectionAndSetUpAdapter(FollowingFragment.this, false).execute();
        }
    }

    /**
     * Shows a dialog to confirm override of accounts in storage after refresh could not refresh
     * some accounts
     * @param fragment FollowingFragment
     */
    private void showConfirmationDialogAndOverrideAccountsRefresh(final FollowingFragment fragment) {
        try {
            AlertDialog.Builder alertDialogBuilder;

            // create alertDialog
            alertDialogBuilder = new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.dialog_refreshing_accounts_failed_title)
                    .setMessage(listAccountFailedRefresh.size() + "/" + followedAccounts.size() + " " + getString(R.string.dialog_refreshing_accounts_failed_message))
                    .setPositiveButton(R.string.dialog_refreshing_accounts_failed_positive_button, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // store updated accountList in memory and update listView
                            new StoreUpdatedFollowedAccountsInStorage(fragment).execute();
                        }
                    })
                    .setNegativeButton(R.string.dialog_refreshing_accounts_failed_negative_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (listAccountFailedRefresh != null) {
                                listAccountFailedRefresh.clear();
                                listAccountFailedRefresh = null;
                            }
                            fragment.resetSomethingsWrong();
                        }
                    })

                    // get the click outside the dialog to set the behaviour like the negative button was clicked
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            if (listAccountFailedRefresh != null) {
                                listAccountFailedRefresh.clear();
                                listAccountFailedRefresh = null;
                            }
                            fragment.resetSomethingsWrong();
                        }
                    });

            final AlertDialog.Builder finalAlertDialogBuilder = alertDialogBuilder;

            // get color for button texts
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = requireContext().getTheme();
            theme.resolveAttribute(R.attr.colorAccent, typedValue, true);
            @ColorInt final int color = typedValue.data;

            // create alertDialog
            requireActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog alertDialog = finalAlertDialogBuilder.create();
                    alertDialog.setCanceledOnTouchOutside(true);
                    alertDialog.show();
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color);
                    alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(color);
                }
            });
        } catch (Exception e) {
            Log.d("FollowingFragment", Log.getStackTraceString(e));
        }
    }

    private void refreshAccountList() {
        // refresh followed accounts posts
        readDataFromInternalStorage();
        listViewFollowedAccounts.invalidateViews();
    }

    private void resetSomethingsWrong() {
        somethingWentWrong = false;
    }

    /**
     * Checks internet connection and notifies user if there is no connection.
     * Sets CustomListAdapterSearch at the end
     */
    @SuppressWarnings("CanBeFinal")
    private static class CheckConnectionAndSetUpAdapter extends AsyncTask<Void, Void, Void> {

        private final WeakReference<FollowingFragment> fragmentReference;
        boolean isInternetAvailable = false;
        boolean bLoadUpdatedUrls;

        // constructor
        CheckConnectionAndSetUpAdapter(FollowingFragment context, Boolean bLoadUpdatedUrls) {
            fragmentReference = new WeakReference<>(context);
            this.bLoadUpdatedUrls = bLoadUpdatedUrls;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if (!isCancelled()) {
                isInternetAvailable = NetworkHandler.isInternetAvailable();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (!isCancelled()) {
                // get reference from fragment
                final FollowingFragment fragment = fragmentReference.get();
                if (fragment != null) {
                    if (fragment.followedAccounts != null && !fragment.followedAccounts.isEmpty()) {
                        // sort accounts and display
                        Collections.sort(fragment.followedAccounts, new CustomComparatorAccount());

                        try {
                            ListAdapterSearch adapter = new ListAdapterSearch(
                                    fragment.getContext(), R.layout.list_item_search,
                                    fragment.followedAccounts,
                                    true,
                                    fragment);
                            fragment.listViewFollowedAccounts.setAdapter(adapter);
                        } catch (NullPointerException e) {
                            Log.d("FollowingFragment", Log.getStackTraceString(e));
                        }
                    } else {
                        fragment.swipeRefreshLayout.setRefreshing(false);
                    }

                    if (!isInternetAvailable) {
                        FragmentHelper.notifyUserOfProblem(fragment, Error.NO_INTERNET_CONNECTION);

                        // set refreshing false
                        fragment.swipeRefreshLayout.setRefreshing(false);
                    }

                    // load updated urls if boolean is true
                    if (isInternetAvailable &&
                            bLoadUpdatedUrls &&
                            fragment.followedAccounts != null
                            && !fragment.followedAccounts.isEmpty()) {
                        new UpdateThumbnailURL(fragment).execute();
                    } else {
                        fragment.swipeRefreshLayout.setRefreshing(false);
                    }

                    // only stop refreshing when bLoadUpdatedUrls is false
                    if (!bLoadUpdatedUrls) {

                        // stop refreshing
                        fragment.swipeRefreshLayout.setRefreshing(false);
                    }
                }
            }
        }
    }

    /**
     * Updates the thumbnail url of followed accounts because they change over time
     */
    private static class UpdateThumbnailURL extends AsyncTask<Void, Integer, Void> {
        private final WeakReference<FollowingFragment> fragmentReference;
        NetworkHandler sh;
        private ProgressDialog progressDialogBatch;
        private int editedItems = 0;

        // constructor
        UpdateThumbnailURL(FollowingFragment context) {
            fragmentReference = new WeakReference<>(context);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            if (!isCancelled()) {
                showProgressDialog();
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if (!isCancelled()) {
                // get reference from fragment
                final FollowingFragment fragment = fragmentReference.get();
                if (fragment != null) {
                    sh = new NetworkHandler();

                    // set initial max size to zero
                    progressDialogBatch.setMax(0);

                    // set max size of progressDialog
                    int progressMaxSize = fragment.followedAccounts.size();

                    // set length of progressDialog
                    progressDialogBatch.setMax(progressMaxSize);

                    // initialize list for failed accounts
                    fragment.listAccountFailedRefresh = new ArrayList<>();

                    // convert followedAccounts (object) to list of accounts in followedAccountsUpdated
                    fragment.followedAccountsUpdatedToAccountList = new ArrayList<>();
                    for (Object obj : fragment.followedAccounts) {
                        Account account = (Account) obj;
                        fragment.followedAccountsUpdatedToAccountList.add(account);
                    }

                    // check if size of the two lists are the same
                    if (fragment.followedAccountsUpdatedToAccountList.size() == fragment.followedAccounts.size()) {

                        // get new thumbnail urls
                        for (Account followedAccountUpdated : fragment.followedAccountsUpdatedToAccountList) {
                            if (followedAccountUpdated != null && followedAccountUpdated.getUsername() != null) {
                                String url = "https://www.instagram.com/" + followedAccountUpdated.getUsername() + "/?__a=1";

                                // get new thumbnail url
                                try {
                                    String newThumbnailUrl = getNewThumbnailUrl(url);
                                    if (newThumbnailUrl != null) {
                                        // set new url
                                        followedAccountUpdated.setImageProfilePicUrl(newThumbnailUrl);

                                        // publish progress -> not real progress here -> Savings missing here
                                        publishProgress(editedItems += 1);
                                    } else {
                                        fragment.somethingWentWrong = true;

                                        // add failed account to list
                                        fragment.listAccountFailedRefresh.add(followedAccountUpdated);
                                    }
                                } catch (JSONException e) {
                                    fragment.somethingWentWrong = true;
                                    Log.d("FollowingFragment", Log.getStackTraceString(e));
                                }
                            }
                        }
                    } else {
                        fragment.somethingWentWrong = true;
                    }
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            progressDialogBatch.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (!isCancelled()) {
                // get reference from fragment
                final FollowingFragment fragment = fragmentReference.get();
                if (fragment != null) {

                    // dismiss progress dialog
                    progressDialogBatch.dismiss();

                    // only if there was no error store updated accountList in memory
                    if (!fragment.somethingWentWrong) {

                        // store updated accountList in memory
                        new FollowingFragment.StoreUpdatedFollowedAccountsInStorage(fragment).execute();
                    } else {

                        // notify user
                        if (fragment.listAccountFailedRefresh != null && !fragment.listAccountFailedRefresh.isEmpty()) {

                            // show dialog with amount of failed accounts and ask what to do
                            fragment.showConfirmationDialogAndOverrideAccountsRefresh(fragment);
                        } else if (!NetworkHandler.isInternetAvailable()) {

                            // no internet
                            FragmentHelper.notifyUserOfProblem(fragment, Error.NO_INTERNET_CONNECTION);
                        } else {

                            // something else
                            FragmentHelper.notifyUserOfProblem(fragment, Error.SOMETHINGS_WRONG);
                        }

                        fragment.swipeRefreshLayout.setRefreshing(false);
                        fragment.resetSomethingsWrong();
                    }
                }
            }
        }

        /**
         * Gets new url for thumbnail of followed account
         * @param url url to fetch from
         * @return new url of thumbnail as String
         * @throws JSONException JSONException
         */
        private String getNewThumbnailUrl(String url) throws JSONException {
            String newThumbnailUrl = null;
            if (!isCancelled()) {

                // get reference from fragment
                final FollowingFragment fragment = fragmentReference.get();
                if (fragment != null) {

                    // get json string from url
                    String jsonStr = sh.makeServiceCall(url, this.getClass().getSimpleName());

                    if (jsonStr != null) {

                        // something went wrong -> possible rate limit reached
                        if (!FragmentHelper.checkIfJsonStrIsValid(jsonStr, fragment)) {
                            return null;
                        }

                        // file overall as json object
                        JSONObject jsonObj = new JSONObject(jsonStr);
                        JSONObject graphql = jsonObj.getJSONObject("graphql");
                        JSONObject user = graphql.getJSONObject("user");

                        // get new new thumbnail url
                        newThumbnailUrl = user.getString("profile_pic_url");
                        jsonObj = null;
                    }
                }
            }
            return newThumbnailUrl;
        }

        private void showProgressDialog() {
            if (!isCancelled()) {
                if (!isCancelled()) {
                    // get reference from fragment
                    final FollowingFragment fragment = fragmentReference.get();

                    if (fragment != null) {
                        progressDialogBatch = new ProgressDialog(fragment.requireContext());
                        progressDialogBatch.setTitle(fragment.requireContext().getString(R.string.title_dialog_refresh_accounts));
                        progressDialogBatch.setMessage(fragment.requireContext().getString(R.string.message_dialog_refresh_accounts));
                        progressDialogBatch.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                        progressDialogBatch.setProgress(0);
                        progressDialogBatch.show();
                    }
                }
            }
        }
    }

    /**
     * Stores updated followedAccountsList in internal storage
     */
    private static class StoreUpdatedFollowedAccountsInStorage extends AsyncTask<Void, Void, Void> {

        private final WeakReference<FollowingFragment> fragmentReference;
        private final boolean removingSuccessful = false;
        private boolean storingSuccessful = false;
        private boolean renamingFilesSuccessful = false;

        // constructor
        StoreUpdatedFollowedAccountsInStorage(FollowingFragment context) {
            fragmentReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if (!isCancelled()) {
                // get reference from fragment
                final FollowingFragment fragment = fragmentReference.get();

                // if there were failed accounts -> delete those in followedAccountsUpdatedToAccountList
                if (fragment.listAccountFailedRefresh != null && !fragment.listAccountFailedRefresh.isEmpty() &&
                        fragment.followedAccountsUpdatedToAccountList != null && !fragment.followedAccountsUpdatedToAccountList.isEmpty()) {
                    for (Account account : fragment.listAccountFailedRefresh) {
                        fragment.followedAccountsUpdatedToAccountList.remove(account);
                    }
                }

                if (fragment != null && fragment.followedAccountsUpdatedToAccountList != null &&
                        fragment.getContext() != null) {
                    try {
                        // store accounts in storage in proper storage representation in filename_accounts_updated
                        storingSuccessful = StorageHelper.storeAccountListInInternalStorage(fragment.followedAccountsUpdatedToAccountList, fragment.requireContext(), StorageHelper.filename_accounts_updated);
                    } catch (Exception e) {
                        storingSuccessful = false;
                        Log.d("FollowingFragment", Log.getStackTraceString(e));
                        fragment.somethingWentWrong = true;
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (!isCancelled()) {
                // get reference from fragment
                final FollowingFragment fragment = fragmentReference.get();
                if (fragment != null) {
                    // check if storing filename_accounts_updated was successful and if the file exists
                    // and rename filename_accounts_updated to old filename_accounts
                    if (storingSuccessful && fragment.getContext() != null &&
                            StorageHelper.checkIfFileExists(StorageHelper.filename_accounts_updated, fragment.requireContext())) {
                        try {
                            StorageHelper.renameSpecificFileTo(fragment.requireContext(), StorageHelper.filename_accounts, StorageHelper.filename_accounts_updated);
                            renamingFilesSuccessful = true;
                        } catch (NullPointerException e) {
                            renamingFilesSuccessful = false;
                            Log.d("FollowingFragment", Log.getStackTraceString(e));
                        }
                    } else {
                        // note: private accounts are no problem! -> do not need to be removed!
                        fragment.somethingWentWrong = true;
                    }

                    // reset followedAccountsUpdated and listAccountFailedRefresh for next iteration
                    if (fragment.followedAccountsUpdatedToAccountList != null) {
                        fragment.followedAccountsUpdatedToAccountList.clear();
                        fragment.followedAccountsUpdatedToAccountList = null;
                    }
                    if (fragment.listAccountFailedRefresh != null) {
                        fragment.listAccountFailedRefresh.clear();
                        fragment.listAccountFailedRefresh = null;
                    }

                    // show toast to display accounts updated successful or failed
                    if (fragment.getActivity() != null && storingSuccessful && renamingFilesSuccessful) {
                        FragmentHelper.showToast(fragment.getResources().getString(R.string.accounts_updated), fragment.requireActivity(), fragment.requireContext());
                    } else if (removingSuccessful) {
                        FragmentHelper.showToast(fragment.getResources().getString(R.string.accounts_removed_successful), fragment.requireActivity(), fragment.requireContext());
                    } else {
                        FragmentHelper.showToast(fragment.getResources().getString(R.string.accounts_updated_failed), fragment.requireActivity(), fragment.requireContext());
                    }

                    // set refreshing false
                    fragment.swipeRefreshLayout.setRefreshing(false);

                    // refresh listView
                    fragment.refreshAccountList();

                    fragment.resetSomethingsWrong();
                }
            }
        }
    }
}
