package com.amnesica.feedsta.fragments;

import static android.view.View.GONE;
import static com.amnesica.feedsta.helper.FeedHelper.counterPostBorder;
import static com.amnesica.feedsta.helper.FeedHelper.fetchBorderPerPage;
import static com.amnesica.feedsta.helper.StaticIdentifier.query_id;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.amnesica.feedsta.Account;
import com.amnesica.feedsta.Post;
import com.amnesica.feedsta.R;
import com.amnesica.feedsta.adapter.GridViewAdapterFeed;
import com.amnesica.feedsta.helper.CustomComparatorNewestFirst;
import com.amnesica.feedsta.helper.Error;
import com.amnesica.feedsta.helper.FeedHelper;
import com.amnesica.feedsta.helper.FeedObject;
import com.amnesica.feedsta.helper.FragmentHelper;
import com.amnesica.feedsta.helper.NetworkHandler;
import com.amnesica.feedsta.helper.StorageHelper;
import com.amnesica.feedsta.models.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;

/**
 * Fragment displays the feed based on the followed accounts
 */
@SuppressWarnings("deprecation")
public class FeedFragment extends Fragment {

    // view stuff
    private View v;
    private GridView gridView;
    private SwipeRefreshLayout swipe;
    private ProgressBar progressBar;
    private TextView textNoAccountsToFollow;

    // list of posts
    private ArrayList<Post> posts;

    // list of fetched posts
    private ArrayList<Post> fetchedPosts;

    // list of accounts
    private ArrayList<Account> accounts;

    // thread lists
    private ArrayList<GetPostsFromAccount> GetPostsFromAccountList;

    // finished thread list
    private ArrayList<GetPostsFromAccount> finishedThreadsList;

    // boolean when there are stored posts at startup
    private boolean bStoredPosts = false;

    // boolean firstLoad
    private boolean bFirstLoad = true;

    // copy list for storing
    private ArrayList<Post> postsToStore;

    // boolean that something went wrong when fetching
    // (e.g. not all accounts could be queried)
    private boolean bSomethingWentWrong = false;

    // list of handler to reset them properly
    private ArrayList<NetworkHandler> networkHandlersList;

    // counter for overall amount of accounts to fetch
    private int counterAmountAccountsToFetch = 0;

    // hashmap to store fetched accounts
    private final HashMap<String, Boolean> fetchedAccountsMap = new HashMap<>();

    public FeedFragment() {
        //  Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // inflate the layout for this fragment
        v = inflater.inflate(R.layout.fragment_feed, container, false);

        // set up toolbar
        Toolbar toolbar = v.findViewById(R.id.toolbar);
        setUpToolbar(toolbar);

        // set up progressbar and textNoAccountsToFollow
        progressBar = v.findViewById(R.id.progressBarFeed);
        progressBar.setProgress(0);
        textNoAccountsToFollow = v.findViewById(R.id.textNoAccountsToFollow);

        // setup gridView
        gridView = v.findViewById(R.id.gridViewFeed);
        setUpGridViewFeed(gridView);

        // set swipe refresh action
        swipe = v.findViewById(R.id.swipeRefresh);
        setUpSwipeRefreshListener(swipe);

        finishedThreadsList = new ArrayList<>();
        GetPostsFromAccountList = new ArrayList<>();

        // load posts from Instagram, but check connection first
        new CheckConnectionAndFetchPosts(FeedFragment.this).execute();

        return v;
    }

    /**
     * Sets up SwipeRefreshListener to load new posts
     *
     * @param swipe swipe
     */
    private void setUpSwipeRefreshListener(SwipeRefreshLayout swipe) {
        swipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (posts != null && !posts.isEmpty()) {
                    finishedThreadsList.clear();
                    GetPostsFromAccountList.clear();
                }

                // refresh posts
                new CheckConnectionAndFetchPosts(FeedFragment.this).execute();
            }
        });
    }

    /**
     * Sets up gridView for feed with onItemClickListener for posts
     *
     * @param gridView gridView
     */
    private void setUpGridViewFeed(GridView gridView) {
        setAmountOfColumnsGridView();

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // get post to send
                Post postToSend = posts.get(position);

                // new PostFragment
                PostFragment postFragment = PostFragment.newInstance(postToSend);

                // add fragment to container
                FragmentHelper.addFragmentToContainer(postFragment, requireActivity().getSupportFragmentManager());
            }
        });
    }

    /**
     * Get the amount of columns of the gridView from SharedPreferences
     */
    private void setAmountOfColumnsGridView() {
        // get the amount of columns from settings
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        if (preferences != null) {
            String amountColumns = preferences.getString("key_feed_list_columns", "2");

            // set columns in gridView
            assert amountColumns != null;
            gridView.setNumColumns(Integer.parseInt(amountColumns));
        }
    }

    /**
     * Sets up the toolbar
     *
     * @param toolbar toolbar
     */
    private void setUpToolbar(Toolbar toolbar) {
        toolbar.setTitle(getResources().getString(R.string.toolbar_title_feed));
        toolbar.inflateMenu(R.menu.menu_main);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.menu_action_followed_accounts) {
                    FragmentHelper.loadAndShowFragment(requireActivity().getSupportFragmentManager().findFragmentByTag(FollowingFragment.class.getSimpleName()),
                            requireActivity().getSupportFragmentManager());
                } else if (item.getItemId() == R.id.menu_action_statistics_dialog) {
                    FragmentHelper.showStatisticsDialog(FeedFragment.this);
                } else if (item.getItemId() == R.id.menu_settings) {
                    FragmentHelper.loadAndShowFragment(requireActivity().getSupportFragmentManager().findFragmentByTag(SettingsHolderFragment.class.getSimpleName()),
                            requireActivity().getSupportFragmentManager());
                } else if (item.getItemId() == R.id.menu_info) {
                    FragmentHelper.loadAndShowFragment(requireActivity().getSupportFragmentManager().findFragmentByTag(AboutFragment.class.getSimpleName()),
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
            setAmountOfColumnsGridView();
        } else {
            // stop all async tasks and their network handlers
            stopAllAsyncTasks();
        }
    }

    /**
     * Sets text when there are no followed accounts
     */
    private void setTextNoFollowedAccounts() {
        TextView textNoAccountsToFollow = v.findViewById(R.id.textNoAccountsToFollow);
        textNoAccountsToFollow.setVisibility(View.VISIBLE);
        gridView.setVisibility(GONE);
    }

    /**
     * Notifies the user that something went wrong and shows toast
     */
    private void hideProgressBarAndTextLoading() {
        try {
            // hide progressBar and textView
            requireActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressBar.setVisibility(GONE);
                    if (swipe.isRefreshing()) {
                        swipe.setRefreshing(false);
                    }
                }
            });
        } catch (IllegalStateException e) {
            Log.d("FeedFragment", Log.getStackTraceString(e));
        }
    }

    /**
     * Stops all running async tasks and their network handlers and resets swipe
     */
    private void stopAllAsyncTasks() {
        try {
            if (GetPostsFromAccountList != null && !GetPostsFromAccountList.isEmpty()) {
                for (GetPostsFromAccount thread : GetPostsFromAccountList) {
                    if (thread != null) {
                        thread.cancel(true);
                    }
                }
                GetPostsFromAccountList.clear();
            }

            if (networkHandlersList != null && !networkHandlersList.isEmpty()) {
                for (NetworkHandler sh : networkHandlersList) {
                    if (sh != null) {
                        sh.closeConnectionsAndBuffers();
                    }
                }
                networkHandlersList.clear();
            }
        } catch (Exception e) {
            Log.d("FeedFragment", Log.getStackTraceString(e));
        }

        if (swipe != null) {
            swipe.setRefreshing(false);
        }
    }

    /**
     * Checks internet connection and notifies user if there is no connection.
     * Starts the fetching at the end
     */
    @SuppressWarnings({"CanBeFinal", "deprecation"})
    private static class CheckConnectionAndFetchPosts extends AsyncTask<Void, Void, Void> {

        private final WeakReference<FeedFragment> fragmentReference;
        boolean isInternetAvailable = false;

        // constructor
        CheckConnectionAndFetchPosts(FeedFragment context) {
            fragmentReference = new WeakReference<>(context);
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
                final FeedFragment fragment = fragmentReference.get();
                if (fragment != null) {

                    if (isInternetAvailable) {
                        try {
                            // start fetching posts
                            new GetPosts(fragment).execute();
                        } catch (RejectedExecutionException e) {
                            Log.d("FeedFragment", Log.getStackTraceString(e));
                        }
                    } else {
                        FragmentHelper.notifyUserOfProblem(fragment, Error.NO_INTERNET_CONNECTION);
                        fragment.hideProgressBarAndTextLoading();

                        // show posts from storage if there are any
                        if (fragment.getContext() != null) {

                            fragment.posts = StorageHelper.readPostsFromInternalStorage(fragment.requireContext(), StorageHelper.filename_posts);

                            if (fragment.posts != null && !fragment.posts.isEmpty()) {

                                // set adapter
                                GridViewAdapterFeed adapter = new GridViewAdapterFeed(fragment.getContext(), R.layout.gridview_item_image, fragment.posts);
                                fragment.gridView.setAdapter(adapter);
                                fragment.gridView.setVisibility(View.VISIBLE);

                                // invalidate view
                                adapter.notifyDataSetChanged();
                                fragment.gridView.invalidateViews();
                            }
                        }

                        // set boolean bFirstLoad
                        fragment.bFirstLoad = false;

                        // set refreshing false
                        fragment.swipe.setRefreshing(false);
                    }
                }
            }

        }
    }

    @SuppressWarnings({"CanBeFinal", "deprecation"})
    private static class GetPosts extends AsyncTask<Void, Integer, Void> {

        private final WeakReference<FeedFragment> fragmentReference;

        // constructor
        GetPosts(FeedFragment context) {
            fragmentReference = new WeakReference<>(context);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (!isCancelled()) {

                // get reference from fragment
                final FeedFragment fragment = fragmentReference.get();
                if (fragment != null) {

                    // get accounts from storage
                    getAccountDataFromStorage();

                    // get stored posts from storage only on first load
                    if (fragment.bFirstLoad) {
                        getStoredPostsFromStorage();

                        // in case there are no accounts
                        fragment.textNoAccountsToFollow.setVisibility(GONE);
                    }

                    // show warning "no accounts" or hide warning
                    // (show only when there are no stored posts)
                    if (fragment.accounts != null) {

                        // in case there were no accounts last time
                        fragment.textNoAccountsToFollow.setVisibility(GONE);
                    } else {

                        // only show message when there are no stored posts
                        if (!fragment.bStoredPosts) {

                            // set text "no followed accounts"
                            fragment.setTextNoFollowedAccounts();

                            if (fragment.posts != null && !fragment.posts.isEmpty()) {
                                fragment.posts.clear();
                            }

                            // invalidate view
                            fragment.gridView.invalidateViews();
                            fragment.swipe.setRefreshing(false);
                        }
                    }
                }
            }
        }

        @SuppressLint("WrongThread")
        @Override
        protected Void doInBackground(Void... arg0) {
            // start many new async tasks
            if (!isCancelled()) {

                // get reference from fragment
                final FeedFragment fragment = fragmentReference.get();
                if (fragment != null) {

                    // if there are stored posts, display them and end method
                    if (fragment.bStoredPosts && fragment.bFirstLoad) {
                        // end method
                        return null;
                    } else if (fragment.accounts != null) {

                        fragment.GetPostsFromAccountList = new ArrayList<>();

                        // initialize posts for fetching
                        if (fragment.fetchedPosts == null) {
                            fragment.fetchedPosts = new ArrayList<>();
                        }

                        try {
                            // only if swipe is not refreshing show textView and progressbar
                            if (!fragment.swipe.isRefreshing()) {
                                fragment.requireActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        // make progressbar and textView visible
                                        fragment.progressBar.setVisibility(View.VISIBLE);
                                    }
                                });
                            }
                        } catch (Exception e) {
                            Log.d("FeedFragment", Log.getStackTraceString(e));
                        }

                        // save size of account list to fetch
                        fragment.counterAmountAccountsToFetch = fragment.accounts.size();

                        // randomize order of accounts to even fetch some accounts if rate limit appears
                        Collections.shuffle(fragment.accounts);

                        // start new threads for all accounts parallel with THREAD_POOL_EXECUTOR
                        for (Account account : fragment.accounts) {
                            GetPostsFromAccount getPostsFromAccount = new GetPostsFromAccount(fragment, account);
                            getPostsFromAccount.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                            fragment.GetPostsFromAccountList.add(getPostsFromAccount);
                        }

                        // let all threads finish execution before finishing main thread
                        do {
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException | IllegalStateException e) {
                                Log.d("FeedFragment", Log.getStackTraceString(e));
                                fragment.bSomethingWentWrong = true;
                            }
                        } while (fragment.finishedThreadsList.size() < fragment.GetPostsFromAccountList.size());
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if (!isCancelled()) {

                // get reference from fragment
                final FeedFragment fragment = fragmentReference.get();
                if (fragment != null) {

                    try {
                        // setup gridView, if there are stored posts display them, else show new posts
                        if (fragment.bStoredPosts && fragment.bFirstLoad) {

                            // set bFirstLoad to false
                            fragment.bFirstLoad = false;

                            // set adapter of view with fragment.posts
                            setAdapter();

                            // set boolean bStoredPosts to false, because new ones where fetched
                            fragment.bStoredPosts = false;

                            fragment.swipe.setRefreshing(false);

                        } else if (fragment.finishedThreadsList.size() == fragment.GetPostsFromAccountList.size()) {

                            // hide progressbar and textView
                            fragment.progressBar.setVisibility(GONE);

                            // only show new posts if fetchedPosts is not empty
                            if (fragment.fetchedPosts != null) {

                                if (fragment.posts == null) {
                                    // in case there are no stored posts but accounts
                                    fragment.posts = new ArrayList<>();
                                }

                                // set posts to fetchedPosts and reset fetchedPosts list
                                if (!fragment.fetchedPosts.isEmpty()) {
                                    fragment.posts.clear();
                                    fragment.posts.addAll(fragment.fetchedPosts);
                                    fragment.fetchedPosts.clear();
                                    fragment.fetchedPosts = null;

                                    // sort posts and display
                                    Collections.sort(fragment.posts, new CustomComparatorNewestFirst());
                                    Collections.reverse(fragment.posts);

                                    // remove duplicates from list
                                    removeDuplicatePosts(fragment.posts);

                                    // resize list to improve performance
                                    if (fragment.posts.size() > FeedHelper.counterFeedBorder) {
                                        fragment.posts.subList(FeedHelper.counterFeedBorder, fragment.posts.size()).clear();
                                    }

                                    // list to store posts in storage for next startup
                                    fragment.postsToStore = new ArrayList<>(fragment.posts);

                                    // store posts in storage for next startup
                                    new StorePostsInStorage(fragment).execute();

                                    // show snackbar alert when not all but some accounts could be queried
                                    if (fragment.bSomethingWentWrong && fragment.fetchedAccountsMap != null && fragment.fetchedAccountsMap.keySet().size() < fragment.counterAmountAccountsToFetch) {
                                        FragmentHelper.notifyUserOfIncompleteFetchProblem(fragment, fragment.fetchedAccountsMap.keySet().size(), fragment.counterAmountAccountsToFetch);
                                    }
                                } else {
                                    // show alert that nothing could be queried and something went wrong
                                    if (fragment.bSomethingWentWrong) {
                                        FragmentHelper.notifyUserOfIncompleteFetchProblem(fragment, 0, fragment.counterAmountAccountsToFetch);
                                    }
                                }

                                // reset boolean for next iteration
                                fragment.bSomethingWentWrong = false;

                                // set adapter of view with fetchedPosts
                                setAdapter();

                                // set refreshing false
                                fragment.swipe.setRefreshing(false);

                                // set boolean bStoredPosts to false, because new ones where fetched
                                fragment.bStoredPosts = false;

                            } else {
                                if (fragment.accounts == null) {
                                    // delete stored posts
                                    if (fragment.getContext() != null && StorageHelper.checkIfFileExists(StorageHelper.filename_posts, fragment.getContext())) {
                                        StorageHelper.deleteSpecificFile(fragment.requireContext(), StorageHelper.filename_posts);
                                    }

                                    // set bFirstLoad to false
                                    fragment.bFirstLoad = false;
                                } else {
                                    // notify user
                                    FragmentHelper.notifyUserOfProblem(fragment, Error.SOMETHINGS_WRONG);
                                }
                                // set refreshing false
                                fragment.swipe.setRefreshing(false);

                                // reset boolean for next iteration
                                fragment.bSomethingWentWrong = false;
                            }
                        }
                    } catch (Exception e) {
                        if (!NetworkHandler.isInternetAvailable()) {
                            Log.d("FeedFragment", Log.getStackTraceString(e));

                            FragmentHelper.notifyUserOfProblem(fragment, Error.NO_INTERNET_CONNECTION);
                        } else {
                            // notify user, something else is problem
                            FragmentHelper.notifyUserOfProblem(fragment, Error.SOMETHINGS_WRONG);
                        }

                        // set refreshing false
                        fragment.swipe.setRefreshing(false);
                    }
                }
            }
        }

        /**
         * Sets the CustomGridViewAdapterFeed Adapter and invalidates the view
         */
        private void setAdapter() {
            if (!isCancelled()) {
                // get reference from fragment
                final FeedFragment fragment = fragmentReference.get();

                if (fragment != null) {
                    GridViewAdapterFeed adapter = new GridViewAdapterFeed(fragment.getContext(), R.layout.gridview_item_image, fragment.posts);
                    fragment.gridView.setAdapter(adapter);
                    fragment.gridView.setVisibility(View.VISIBLE);

                    // invalidate view
                    adapter.notifyDataSetChanged();
                    fragment.gridView.invalidateViews();
                }
            }
        }

        /**
         * Returns a list without duplicate posts
         *
         * @param oldListPosts list of posts with duplicates
         */
        private void removeDuplicatePosts(ArrayList<Post> oldListPosts) {
            if (!isCancelled()) {
                // get reference from fragment
                final FeedFragment fragment = fragmentReference.get();
                if (fragment != null) {
                    ArrayList<Post> newListPosts = new ArrayList<>();
                    Set<String> shortCodes = new HashSet<>();

                    // try to add shortCode to set, if successful add post to newListPosts (-> no duplicate)
                    for (Post post : oldListPosts) {
                        if (shortCodes.add(post.getShortcode())) {
                            newListPosts.add(post);
                        }
                    }

                    // newListPosts is new fragments.posts list
                    fragment.posts.clear();
                    fragment.posts.addAll(newListPosts);
                }
            }
        }

        /**
         * Gets followed accounts from storage
         */
        private void getAccountDataFromStorage() {
            if (!isCancelled()) {
                // get reference from fragment
                final FeedFragment fragment = fragmentReference.get();
                if (fragment != null && fragment.getContext() != null) {
                    // get accounts
                    fragment.accounts = StorageHelper.readAccountsFromInternalStorage(fragment.requireContext());
                }
            }
        }

        /**
         * Gets stored posts from storage
         */
        private void getStoredPostsFromStorage() {
            if (!isCancelled()) {
                // get reference from fragment
                final FeedFragment fragment = fragmentReference.get();
                if (fragment != null && fragment.getContext() != null) {
                    // get stored posts
                    fragment.posts = StorageHelper.readPostsFromInternalStorage(fragment.requireContext(), StorageHelper.filename_posts);

                    // set boolean bStoredPosts if list is not empty
                    if (fragment.posts != null && !fragment.posts.isEmpty()) {
                        fragment.bStoredPosts = true;
                    }
                }
            }
        }
    }

    /**
     * Gets posts from a single account
     */
    @SuppressWarnings({"CanBeFinal", "deprecation"})
    private static class GetPostsFromAccount extends AsyncTask<Void, Void, Void> {

        private final WeakReference<FeedFragment> fragmentReference;
        private final Account account;
        boolean bFirstFetch = true;
        NetworkHandler sh;
        private URL url;
        private int counterPost = 0;
        private boolean successfulFetchedPostsOfAccount = true;

        // constructor
        GetPostsFromAccount(FeedFragment context, Account account) {
            fragmentReference = new WeakReference<>(context);
            this.account = account;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if (!isCancelled()) {
                final FeedFragment fragment = fragmentReference.get();

                // new HttpHandler
                sh = new NetworkHandler();

                // create handlerList if it not exists already
                if (fragment != null && fragment.networkHandlersList == null) {
                    fragment.networkHandlersList = new ArrayList<>();
                }

                try {
                    // add handler to handlersList
                    if (fragment != null) {
                        fragment.networkHandlersList.add(sh);

                        // put account name in map for fetched accounts to
                        // determine if fetching was successful
                        fragment.fetchedAccountsMap.put(account.getUsername(), true);
                    }
                } catch (Exception e) {
                    Log.d("FeedFragment", Log.getStackTraceString(e));
                }

                do {
                    // make valid urls from accounts
                    makeValidURLS();

                    // fetch data of url
                    fetchDataOfUrl(url);

                    // set start- and endIndex
                    int startIndex = 0;
                    int endIndex = url.edgesTotalOfPage;

                    // fetch edge data with start- and endIndex
                    fetchEdgeData(url.jsonArrayEdges, startIndex, endIndex);

                    // prevent url.hasNextPage is null
                } while (url.hasNextPage != null && url.hasNextPage && counterPost < counterPostBorder);

                // if fetching was not successful remove account name from map
                if (!successfulFetchedPostsOfAccount && url.tag != null && fragment != null) {
                    fragment.fetchedAccountsMap.remove(account.getUsername());
                }
            } else {
                // get reference from fragment
                final FeedFragment fragment = fragmentReference.get();
                if (fragment != null) {
                    fragment.bSomethingWentWrong = true;
                    successfulFetchedPostsOfAccount = false;

                    // if fetching was not successful remove account name from map
                    fragment.fetchedAccountsMap.remove(account.getUsername());
                }
            }
            return null;
        }

        /**
         * Makes valid URLs for account posts
         */
        private void makeValidURLS() {
            if (!isCancelled()) {
                // get reference from fragment
                final FeedFragment fragment = fragmentReference.get();
                if (fragment != null) {
                    if (account != null) {
                        String urlAddress = null;

                        // make url for account posts (hint: url.endCursor is null at first page fetch)
                        if (url == null || (url.endCursor == null && bFirstFetch)) {
                            urlAddress = "https://www.instagram.com/graphql/query/?query_id=" + query_id + "&id=" + account.getId() + "&first=" + fetchBorderPerPage + "&after=";
                            bFirstFetch = false;
                        } else if (url.hasNextPage != null && url.hasNextPage) {
                            urlAddress = "https://www.instagram.com/graphql/query/?query_id=" + query_id + "&id=" + account.getId() + "&first=" + fetchBorderPerPage + "&after=" + url.endCursor;
                        }
                        url = new URL(urlAddress, account.getUsername(), FeedObject.ACCOUNT);
                    } else {
                        fragment.bSomethingWentWrong = true;
                        successfulFetchedPostsOfAccount = false;
                    }
                }
            }
        }

        /**
         * Fetches data from URL
         *
         * @param url url to fetch from
         */
        private void fetchDataOfUrl(URL url) {
            if (!isCancelled()) {
                // get reference from fragment
                final FeedFragment fragment = fragmentReference.get();
                if (fragment != null) {

                    // fetch data for accounts
                    assert url.FeedObject != null;
                    if (url.FeedObject.equals(FeedObject.ACCOUNT)) {
                        String jsonStr;

                        if (url.url != null) {
                            // get json string from url
                            jsonStr = sh.makeServiceCall(url.url, FeedFragment.class.getSimpleName());
                        } else {
                            fragment.bSomethingWentWrong = true;
                            successfulFetchedPostsOfAccount = false;
                            return;
                        }

                        if (jsonStr != null) {
                            try {
                                // if rate limit or unknown error notify user
                                if (!FragmentHelper.checkIfJsonStrIsValid(jsonStr, fragment)) {
                                    // stop fetching at this point
                                    fragment.bSomethingWentWrong = true;
                                    successfulFetchedPostsOfAccount = false;
                                    return;
                                }
                                // file overall as json object
                                JSONObject jsonObj = new JSONObject(jsonStr);

                                // get only relevant data (so save followers etc. later when need => when account site should be visited)
                                // save page_info and has_next_page
                                JSONObject page_info = jsonObj
                                        .getJSONObject("data")
                                        .getJSONObject("user")
                                        .getJSONObject("edge_owner_to_timeline_media").getJSONObject("page_info");
                                if (page_info.getBoolean("has_next_page")) {
                                    url.hasNextPage = true;
                                    url.endCursor = page_info.getString("end_cursor");
                                } else {
                                    url.hasNextPage = false;
                                }

                                JSONArray edges = jsonObj
                                        .getJSONObject("data")
                                        .getJSONObject("user")
                                        .getJSONObject("edge_owner_to_timeline_media").getJSONArray("edges");

                                url.jsonArrayEdges = edges;
                                if (edges != null && !edges.isNull(0)) {
                                    url.edgesTotalOfPage = edges.length();
                                } else {
                                    url.edgesTotalOfPage = 0;
                                }

                            } catch (JSONException | IllegalStateException e) {
                                fragment.bSomethingWentWrong = true;
                                successfulFetchedPostsOfAccount = false;
                                Log.d("FeedFragment", Log.getStackTraceString(e));
                            }
                        } else {
                            fragment.bSomethingWentWrong = true;
                            successfulFetchedPostsOfAccount = false;
                        }
                    }
                }
            }
        }

        /**
         * Fetch data of a single edge (JSONObject)
         *
         * @param edges      JSONArray
         * @param startIndex int
         * @param endIndex   int
         */
        private void fetchEdgeData(JSONArray edges, int startIndex, int endIndex) {
            if (!isCancelled()) {

                // get reference from fragment
                final FeedFragment fragment = fragmentReference.get();
                if (fragment != null) {
                    try {
                        for (int i = startIndex; i < endIndex; i++) {
                            // get edge object
                            JSONObject edge = edges.getJSONObject(i);

                            // check if post is sidecar
                            boolean is_sidecar = false;
                            if (edge.getJSONObject("node").getString("__typename").equals("GraphSidecar")) {
                                is_sidecar = true;
                            }

                            // create post with minimal information (initialize post)
                            Post post = new Post(
                                    edge.getJSONObject("node").getString("id"),
                                    edge.getJSONObject("node").getString("shortcode"),
                                    new Date(Long.parseLong(edge.getJSONObject("node").getString("taken_at_timestamp")) * 1000),
                                    edge.getJSONObject("node").getBoolean("is_video"),
                                    edge.getJSONObject("node").getString("thumbnail_src"),
                                    is_sidecar,
                                    null);

                            if (fragment.fetchedPosts == null) {
                                fragment.fetchedPosts = new ArrayList<>();
                            }

                            // add post to posts-list
                            fragment.fetchedPosts.add(post);

                            // fetch border total
                            counterPost += 1;
                        }
                    } catch (JSONException | IllegalStateException | ArrayIndexOutOfBoundsException e) {
                        fragment.bSomethingWentWrong = true;
                        successfulFetchedPostsOfAccount = false;
                    }
                }
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (!isCancelled()) {
                // get reference from fragment
                final FeedFragment fragment = fragmentReference.get();
                if (fragment != null) {
                    // add finished thread to list
                    fragment.finishedThreadsList.add(this);
                }
            }
            super.onPostExecute(aVoid);
        }
    }

    /**
     * Stores fetched posts in internal storage
     */
    @SuppressWarnings("deprecation")
    private static class StorePostsInStorage extends AsyncTask<Void, Void, Void> {

        private final WeakReference<FeedFragment> fragmentReference;

        StorePostsInStorage(FeedFragment context) {
            fragmentReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if (!isCancelled()) {
                // get reference from fragment
                final FeedFragment fragment = fragmentReference.get();
                if (fragment != null && fragment.postsToStore != null && fragment.getContext() != null) {
                    // check if file exits, if true, delete file
                    if (StorageHelper.checkIfFileExists(StorageHelper.filename_posts, fragment.requireContext())) {
                        StorageHelper.deleteSpecificFile(fragment.requireContext(), StorageHelper.filename_posts);
                    }
                    try {
                        // store posts in storage in proper storage representation
                        StorageHelper.storePostListInInternalStorage(fragment.postsToStore, fragment.requireContext(), StorageHelper.filename_posts);

                    } catch (IllegalStateException | IOException e) {
                        Log.d("FeedFragment", Log.getStackTraceString(e));
                    }
                }
            }
            return null;
        }
    }
}