package com.amnesica.feedsta.fragments;

import static android.view.View.GONE;

import android.graphics.text.LineBreaker;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.amnesica.feedsta.R;
import com.amnesica.feedsta.adapter.RecViewAdapterFeed;
import com.amnesica.feedsta.fragments.settings.SettingsHolderFragment;
import com.amnesica.feedsta.helper.Error;
import com.amnesica.feedsta.helper.FragmentHelper;
import com.amnesica.feedsta.helper.NetworkHandler;
import com.amnesica.feedsta.helper.StorageHelper;
import com.amnesica.feedsta.helper.comparator.CustomComparatorNewestFirst;
import com.amnesica.feedsta.helper.feed.FeedHelper;
import com.amnesica.feedsta.models.Account;
import com.amnesica.feedsta.models.Post;
import com.amnesica.feedsta.models.sidecar.Sidecar;
import com.amnesica.feedsta.models.sidecar.SidecarEntry;
import com.amnesica.feedsta.models.sidecar.SidecarEntryType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Fragment displays the feed based on the followed accounts */
public class FeedFragment extends Fragment {

  // view stuff
  private RecyclerView recyclerViewFeed;
  private SwipeRefreshLayout swipeRefreshLayout;
  private TextView textFeedHint;

  private ArrayList<Post> posts;
  private ArrayList<Post> fetchedPosts;
  private ArrayList<Account> accounts;

  private ExecutorService loadFeedExecutor;

  // copy list for storing
  private ArrayList<Post> postsToStore;

  // boolean that something went wrong when fetching
  // (e.g. not all accounts could be queried)
  private boolean bSomethingWentWrong = false;

  // counter for overall amount of accounts to fetch
  private int counterAmountAccountsToFetch = 0;

  // hashmap to store fetched accounts
  private final HashMap<String, Boolean> fetchedAccountsMap = new HashMap<>();

  private RecViewAdapterFeed recViewAdapterFeed;

  public FeedFragment() {
    //  Required empty public constructor
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    // inflate the layout for this fragment
    View view = inflater.inflate(R.layout.fragment_feed, container, false);

    setupToolbar(view);
    setupTextFeedHint(view);
    setupRecyclerViewFeed(view);
    setupSwipeRefreshLayout(view);

    checkInternetConnection();
    loadPostsFromInternalStorage();

    getAccountDataFromStorage();
    showOrHideNoAccountsHint();

    return view;
  }

  private Boolean checkInternetConnection() {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Handler handler = new Handler(Looper.getMainLooper());
    AtomicBoolean internetIsAvailable = new AtomicBoolean(false);

    try {
      executor.execute(
          () -> {
            // Background work here
            internetIsAvailable.set(NetworkHandler.isInternetAvailable());
            handler.post(
                () -> {
                  // UI Thread work here
                  if (!internetIsAvailable.get())
                    FragmentHelper.notifyUserOfProblem(this, Error.NO_INTERNET_CONNECTION);
                });
          });

      executor.shutdown();

      // timeout is 5 seconds
      executor.awaitTermination(5L, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Log.d("FeedFragment", Log.getStackTraceString(e));
    }

    return internetIsAvailable.get();
  }

  private void loadPostsFromInternalStorage() {
    if (getContext() == null) return;

    posts =
        StorageHelper.readPostsFromInternalStorage(requireContext(), StorageHelper.FILENAME_POSTS);

    setRecViewAdapterFeedWithPosts();
  }

  /** Sets up textFeedHint for showing messages related to feed */
  private void setupTextFeedHint(@NonNull final View view) {
    textFeedHint = view.findViewById(R.id.textFeedHint);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      textFeedHint.setJustificationMode(LineBreaker.JUSTIFICATION_MODE_INTER_WORD);
    }
  }

  private void setupRecyclerViewFeed(@NonNull final View view) {
    recyclerViewFeed = view.findViewById(R.id.recyclerViewFeed);
    LinearLayoutManager linearLayoutManager = new LinearLayoutManager(requireContext());
    linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
    recyclerViewFeed.setLayoutManager(linearLayoutManager);
  }

  private void setupSwipeRefreshLayout(@NonNull final View view) {
    swipeRefreshLayout = view.findViewById(R.id.swipeRefreshFeed);
    swipeRefreshLayout.setOnRefreshListener(this::loadFeedFromInstagram);
  }

  private void loadFeedFromInstagram() {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Handler handler = new Handler(Looper.getMainLooper());

    executor.execute(
        () -> {
          // Background work here
          if (!checkInternetConnection()) {
            swipeRefreshLayout.setRefreshing(false);
            return;
          }

          getAccountDataFromStorage();
          showOrHideNoAccountsHint();

          if (accounts != null) {
            counterAmountAccountsToFetch = accounts.size();
            // randomize order of accounts to even fetch some accounts if rate limit appears
            Collections.shuffle(accounts);
            createAndExecuteThreadsToLoadFeed();
          }

          handler.post(
              () -> {
                // UI Thread work here
                if (fetchedPosts != null) {
                  // only show new posts if fetchedPosts is not empty
                  saveAndDisplayNewlyFetchedPosts();
                } else {
                  if (accounts == null) {
                    // no fetchedPosts and no accounts -> delete stored posts and clear posts list
                    deleteStoredPosts();
                    clearPostsList();
                    setTextFeedHint(getResources().getString(R.string.no_followed_accounts));
                  } else {
                    FragmentHelper.notifyUserOfProblem(this, Error.SOMETHINGS_WRONG);
                  }
                }
                swipeRefreshLayout.setRefreshing(false);
              });
        });

    executor.shutdown();
    recyclerViewFeed.invalidate();
  }

  private void clearPostsList() {
    if (posts != null) {
      posts.clear();
    }
  }

  private void createAndExecuteThreadsToLoadFeed() {
    loadFeedExecutor = Executors.newFixedThreadPool(counterAmountAccountsToFetch);
    // start new threads for all accounts
    for (Account account : accounts) {
      FeedRunnable feedRunnable = new FeedRunnable(account, this);
      Thread thread = new Thread(feedRunnable);
      loadFeedExecutor.submit(thread);
    }

    // This will make the executor accept no new threads and finish all existing threads in the
    // queue
    loadFeedExecutor.shutdown();

    try {
      loadFeedExecutor.awaitTermination(5L, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Log.d("FeedFragment", Log.getStackTraceString(e));
    }
  }

  private void deleteStoredPosts() {
    if (getContext() != null
        && StorageHelper.checkIfFileExists(StorageHelper.FILENAME_POSTS, requireContext())) {
      StorageHelper.deleteSpecificFile(requireContext(), StorageHelper.FILENAME_POSTS);
    }
  }

  /**
   * Saves newly fetched posts in internal storage and makes them visible in recyclerView in sorted
   * order. Duplicate posts are being removed
   */
  private void saveAndDisplayNewlyFetchedPosts() {
    if (posts == null) {
      // in case there are no stored posts but accounts
      posts = new ArrayList<>();
    }

    // set posts to fetchedPosts and reset fetchedPosts list
    if (!fetchedPosts.isEmpty()) {
      posts.clear();
      posts.addAll(fetchedPosts);
      fetchedPosts.clear();
      fetchedPosts = null;

      // sort posts and display
      Collections.sort(posts, new CustomComparatorNewestFirst());
      Collections.reverse(posts);

      // remove duplicates from list
      removeDuplicatePosts(posts);

      // resize list to improve performance
      if (posts.size() > FeedHelper.counterFeedBorder) {
        posts.subList(FeedHelper.counterFeedBorder, posts.size()).clear();
      }

      // list to store posts in storage for next startup
      postsToStore = new ArrayList<>(posts);

      // store posts in storage for next startup
      storePostsInStorage();

      // show snackBar alert when not all but some accounts could be queried
      if (bSomethingWentWrong
          && fetchedAccountsMap.keySet().size() < counterAmountAccountsToFetch) {
        FragmentHelper.notifyUserOfIncompleteFetchProblem(
            this, fetchedAccountsMap.keySet().size(), counterAmountAccountsToFetch);
      }
    } else {
      // show alert that nothing could be queried and something went wrong
      if (bSomethingWentWrong) {
        FragmentHelper.notifyUserOfIncompleteFetchProblem(this, 0, counterAmountAccountsToFetch);
      }
    }

    // set adapter of view with fetchedPosts
    setRecViewAdapterFeedWithPosts();
  }

  private void storePostsInStorage() {
    ExecutorService executor = Executors.newSingleThreadExecutor();

    executor.execute(
        () -> {
          // Background work here
          if (postsToStore != null && getContext() != null) {
            // check if file exits, if true, delete file
            if (StorageHelper.checkIfFileExists(StorageHelper.FILENAME_POSTS, requireContext())) {
              StorageHelper.deleteSpecificFile(requireContext(), StorageHelper.FILENAME_POSTS);
            }

            try {
              // store posts in storage in proper storage representation
              StorageHelper.storePostListInInternalStorage(
                  postsToStore, requireContext(), StorageHelper.FILENAME_POSTS);

            } catch (Exception e) {
              Log.d("FeedFragment", Log.getStackTraceString(e));
            }
          }
        });

    executor.shutdown();
  }

  private void removeDuplicatePosts(ArrayList<Post> oldListPostsWithDuplicates) {
    ArrayList<Post> newListPosts = new ArrayList<>();
    Set<String> shortCodes = new HashSet<>();

    // try to add shortCode to set, if successful add post to newListPosts
    // (-> no duplicate)
    for (Post post : oldListPostsWithDuplicates) {
      if (shortCodes.add(post.getShortcode())) {
        newListPosts.add(post);
      }
    }

    // newListPosts is new fragments.posts list
    posts.clear();
    posts.addAll(newListPosts);
  }

  /** Show or hide warning "no accounts" (show only when there are no stored posts) */
  private void showOrHideNoAccountsHint() {
    if (accounts != null) {
      // in case there were no accounts on last fetch hide warning now
      requireActivity().runOnUiThread(() -> textFeedHint.setVisibility(GONE));
    } else {
      // only show message when there are no stored posts and no followed accounts
      setTextFeedHint(getResources().getString(R.string.no_followed_accounts));

      recyclerViewFeed.invalidate();
      swipeRefreshLayout.setRefreshing(false);
    }
  }

  /** Gets followed accounts from storage */
  private void getAccountDataFromStorage() {
    if (getContext() != null) {
      // get stored accounts
      accounts = StorageHelper.readAccountsFromInternalStorage(requireContext());
    }
  }

  /**
   * Sets up the toolbar with menu and title
   *
   * @param view View
   */
  private void setupToolbar(@NonNull final View view) {
    Toolbar toolbar = view.findViewById(R.id.toolbar);
    toolbar.setTitle(getResources().getString(R.string.toolbar_title_feed));
    toolbar.inflateMenu(R.menu.menu_main);
    toolbar.setOnMenuItemClickListener(
        item -> {
          if (item.getItemId() == R.id.menu_action_followed_accounts) {
            FragmentHelper.loadAndShowFragment(
                requireActivity()
                    .getSupportFragmentManager()
                    .findFragmentByTag(FollowingFragment.class.getSimpleName()),
                requireActivity().getSupportFragmentManager());
          } else if (item.getItemId() == R.id.menu_action_statistics_dialog) {
            FragmentHelper.showStatisticsDialog(FeedFragment.this);
          } else if (item.getItemId() == R.id.menu_settings) {
            FragmentHelper.loadAndShowFragment(
                requireActivity()
                    .getSupportFragmentManager()
                    .findFragmentByTag(SettingsHolderFragment.class.getSimpleName()),
                requireActivity().getSupportFragmentManager());
          } else if (item.getItemId() == R.id.menu_info) {
            FragmentHelper.loadAndShowFragment(
                requireActivity()
                    .getSupportFragmentManager()
                    .findFragmentByTag(AboutFragment.class.getSimpleName()),
                requireActivity().getSupportFragmentManager());
          } else if (item.getItemId() == R.id.menu_exit) {
            requireActivity().finish();
          }
          return false;
        });
  }

  @Override
  public void onHiddenChanged(boolean hidden) {
    super.onHiddenChanged(hidden);
    // set highlighted item on nav bar to "feed"
    if (!hidden) {
      FragmentHelper.setBottomNavViewSelectElem(getActivity(), R.id.navigation_feed);

      // check if posts were bookmarked (data changes might have happened)
      if (posts != null && !posts.isEmpty()) {
        recViewAdapterFeed.notifyItemRangeChanged(0, posts.size() - 1);
      }
    } else {
      // stop all async tasks and their network handlers when fragment is hidden
      stopAllAsyncTasks();

      // stop videos when fragment is hidden
      recViewAdapterFeed.releaseAllExoPlayers();
    }
  }

  /**
   * Sets text in center of fragment and makes it visible by set gridView visibility to gone
   *
   * @param text CharSequence
   */
  private void setTextFeedHint(CharSequence text) {
    requireActivity()
        .runOnUiThread(
            () -> {
              textFeedHint.setText(text);
              textFeedHint.setVisibility(View.VISIBLE);
              recyclerViewFeed.setVisibility(GONE);
            });
  }

  /** Stops all running async tasks, their network handlers and resets swipe */
  private void stopAllAsyncTasks() {
    try {
      // cancel all GetPostsFromAccount threads
      if (loadFeedExecutor != null && !loadFeedExecutor.isTerminated()) {
        loadFeedExecutor.shutdownNow();
      }
    } catch (Exception e) {
      Log.d("FeedFragment", Log.getStackTraceString(e));
    }

    if (swipeRefreshLayout != null) {
      swipeRefreshLayout.setRefreshing(false);
    }
  }

  @Override
  public void onDestroy() {
    // destroy all viewholder items and stop video player if necessary
    recyclerViewFeed.setAdapter(null);
    super.onDestroy();
  }

  /** Sets the RecViewFeed adapter and make changes visible via invalidating the view */
  private void setRecViewAdapterFeedWithPosts() {
    if (getContext() != null) {
      if (posts == null) posts = new ArrayList<>();

      recViewAdapterFeed = new RecViewAdapterFeed(requireContext(), posts, this);
      recyclerViewFeed.setAdapter(recViewAdapterFeed);
      recyclerViewFeed.setVisibility(View.VISIBLE);
      recViewAdapterFeed.notifyItemRangeChanged(0, posts.size());
      recyclerViewFeed.invalidate();
    }
  }

  private void putAccountNameInFetchedAccountsMap(String accountName) {
    fetchedAccountsMap.put(accountName, true);
  }

  private void markFetchingOfPostsAsFailed(String accountName) {
    bSomethingWentWrong = true;
    fetchedAccountsMap.remove(accountName);
  }

  public static class FeedRunnable implements Runnable {

    private final Account account;
    private final WeakReference<FeedFragment> fragmentReference;
    private final NetworkHandler networkHandler = new NetworkHandler();

    public FeedRunnable(final Account account, final FeedFragment feedFragment) {
      this.fragmentReference = new WeakReference<>(feedFragment);
      this.account = account;
    }

    @Override
    public void run() {
      getAccountPosts();
    }

    private void getAccountPosts() {
      final FeedFragment feedFragment = fragmentReference.get();
      if (feedFragment == null) return;
      try {
        feedFragment.putAccountNameInFetchedAccountsMap(account.getUsername());

        final String url = "https://www.instagram.com/" + account.getUsername() + "/?__a=1&__d=dis";

        JSONObject userData = fetchUserDataFromInstagram(url, feedFragment);
        if (userData == null) return;

        enhanceAccountDataWithUserData(account, userData);

        JSONArray postsAsEdges = extractPostsAsEdges(userData);

        createPostsFromEdgeData(postsAsEdges, feedFragment);
      } catch (Exception e) {
        Log.d("FeedRunnable", Log.getStackTraceString(e));
        feedFragment.markFetchingOfPostsAsFailed(account.getUsername());
        networkHandler.closeConnectionsAndBuffers();
      }
    }

    private void enhanceAccountDataWithUserData(
        @NonNull Account account, @NonNull JSONObject userData) throws JSONException {
      final String imageProfilePicUrl = userData.getString("profile_pic_url_hd");
      account.setImageProfilePicUrl(imageProfilePicUrl);
    }

    private JSONObject fetchUserDataFromInstagram(
        @NonNull final String url, @NonNull FeedFragment feedFragment) throws Exception {
      JSONObject userData;

      // get json string from url
      String jsonStr = networkHandler.makeServiceCall(url, FeedFragment.class.getSimpleName());

      // if rate limit or unknown error notify user (e.g. if jsonStr is null)
      if (!FragmentHelper.checkIfJsonStrIsValid(jsonStr, feedFragment)) {
        feedFragment.markFetchingOfPostsAsFailed(account.getUsername());
        // stop fetching at this point
        return null;
      }

      JSONObject jsonObj = new JSONObject(jsonStr);
      userData = jsonObj.getJSONObject("graphql").getJSONObject("user");

      return userData;
    }

    /** Fetches posts from account as edges */
    private JSONArray extractPostsAsEdges(@NonNull JSONObject userData) throws Exception {
      return userData.getJSONObject("edge_owner_to_timeline_media").getJSONArray("edges");
    }

    private void createPostsFromEdgeData(
        @NonNull JSONArray edges, @NonNull FeedFragment feedFragment) throws Exception {
      final int startIndex = 0;
      final int endIndex = edges.length();

      for (int i = startIndex; i < endIndex; i++) {
        JSONObject node = edges.getJSONObject(i).getJSONObject("node");

        boolean isSidecar = node.getString("__typename").equals("GraphSidecar");
        boolean isVideo = node.getBoolean("is_video");

        Post post =
            Post.builder()
                .id(node.getString("id"))
                .imageUrl(node.getString("display_url"))
                .likes(node.getJSONObject("edge_media_preview_like").getInt("count"))
                .ownerId(node.getJSONObject("owner").getString("id"))
                .comments(node.getJSONObject("edge_media_to_comment").getInt("count"))
                .caption(getCaption(node))
                .shortcode(node.getString("shortcode"))
                .takenAtDate(new Date(Long.parseLong(node.getString("taken_at_timestamp")) * 1000))
                .is_video(isVideo)
                .username(account.getUsername())
                .imageUrlProfilePicOwner(account.getImageProfilePicUrl())
                .imageUrlThumbnail(
                    node.getJSONArray("thumbnail_resources").getJSONObject(2).getString("src"))
                .is_sideCar(isSidecar)
                .height(node.getJSONObject("dimensions").getInt("height"))
                .ownerId(node.getJSONObject("owner").getString("id"))
                .sidecar(isSidecar ? getSidecar(node) : null)
                .videoUrl(isVideo ? node.getString("video_url") : null)
                .build();

        feedFragment.addPostToFetchedPosts(post);
      }
    }

    @NonNull
    private Sidecar getSidecar(JSONObject node) throws JSONException {
      JSONObject edge_sidecar_to_children = node.getJSONObject("edge_sidecar_to_children");
      JSONArray edgesSidecar = edge_sidecar_to_children.getJSONArray("edges");
      return getSidecarFromEdge(edgesSidecar);
    }

    @Nullable
    private String getCaption(JSONObject node) throws JSONException {
      JSONArray edgesCaption = node.getJSONObject("edge_media_to_caption").getJSONArray("edges");

      String caption = null;
      if (edgesCaption.length() != 0 && !edgesCaption.isNull(0)) {
        caption = edgesCaption.getJSONObject(0).getJSONObject("node").getString("text");
      }

      return caption;
    }

    /** Fetches all urls and types of all sidecar entries */
    private Sidecar getSidecarFromEdge(JSONArray edges) throws JSONException {
      ArrayList<SidecarEntry> sidecarEntries = new ArrayList<>();

      for (int i = 0; i < edges.length(); i++) {
        // get edge from edges
        JSONObject edge = edges.getJSONObject(i);

        // get node of selected edge
        JSONObject node = edge.getJSONObject("node");

        if (node.getBoolean("is_video")) {
          // node is video -> get video_url
          String video_url = node.getString("video_url");
          int height = node.getJSONObject("dimensions").getInt("height");

          SidecarEntry sidecarEntry = new SidecarEntry(SidecarEntryType.video, i, video_url);
          sidecarEntry.setHeight(height);

          sidecarEntries.add(sidecarEntry);
        } else { // node is image -> get image_url
          String image_url = node.getString("display_url");
          int height = node.getJSONObject("dimensions").getInt("height");

          SidecarEntry sidecarEntry = new SidecarEntry(SidecarEntryType.image, i, image_url);
          sidecarEntry.setHeight(height);

          sidecarEntries.add(sidecarEntry);
        }
      }
      return new Sidecar(sidecarEntries);
    }
  }

  private void addPostToFetchedPosts(Post post) {
    if (fetchedPosts == null) {
      fetchedPosts = new ArrayList<>();
    }
    fetchedPosts.add(post);
  }
}
