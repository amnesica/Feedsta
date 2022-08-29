package com.amnesica.feedsta.fragments;

import static android.view.View.GONE;
import static androidx.core.content.ContextCompat.getSystemService;
import static com.amnesica.feedsta.helper.StaticIdentifier.permsRequestCode;
import static com.amnesica.feedsta.helper.StaticIdentifier.permsWriteOnly;
import static com.amnesica.feedsta.helper.StaticIdentifier.query_id;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.amnesica.feedsta.R;
import com.amnesica.feedsta.adapter.GridViewAdapterPost;
import com.amnesica.feedsta.asynctasks.DownloadImage;
import com.amnesica.feedsta.asynctasks.download.DownloadPostsBatch;
import com.amnesica.feedsta.helper.EndlessScrollListener;
import com.amnesica.feedsta.helper.Error;
import com.amnesica.feedsta.helper.FeedObject;
import com.amnesica.feedsta.helper.FragmentHelper;
import com.amnesica.feedsta.helper.NetworkHandler;
import com.amnesica.feedsta.helper.StorageHelper;
import com.amnesica.feedsta.models.Account;
import com.amnesica.feedsta.models.Post;
import com.amnesica.feedsta.models.URL;
import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

import in.srain.cube.views.GridViewWithHeaderAndFooter;

/** Fragment for displaying a profile */
public class ProfileFragment extends Fragment {

  // view stuff
  private View view;
  private GridViewWithHeaderAndFooter gridViewImagesOnProfile;
  private View headerView;
  private ImageView imageProfilePic;
  private Account account;
  private Button buttonFollow;
  private EndlessScrollListener scrollListener;
  private ProgressBar progressBar;
  private ProgressDialog progressDialogBatch;
  private Toolbar toolbar;
  private ActionMode actionMode;

  // url and posts
  private URL url;
  private ArrayList<Post> posts;
  private ArrayList<Post> postsToDownload;

  // fetching stuff
  private Boolean bFirstFetch;
  private Boolean bFirstAdapterFetch;
  private Boolean addNextPageToPlaceholder = false;
  private int postCounter = 0;
  private GridViewAdapterPost adapter;

  // dialog stuff
  private boolean errorAlertAlreadyShown = false;
  private final boolean isImageFitToScreen = false;

  static ProfileFragment newInstance(Account account) {
    Bundle args = new Bundle();
    args.putSerializable("account", account);
    ProfileFragment fragment = new ProfileFragment();
    fragment.setArguments(args);
    return fragment;
  }

  // public access to call it from MainActivity for deep linking intent
  public static ProfileFragment newInstance(String username) {
    Bundle args = new Bundle();
    args.putSerializable("username", username);
    ProfileFragment fragment = new ProfileFragment();
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    // inflate the layout for this fragment
    view = inflater.inflate(R.layout.fragment_profile, container, false);

    // retrieve account
    if (this.getArguments() != null) {
      if (getArguments().getSerializable("username") != null) {
        String username = (String) getArguments().getSerializable("username");
        // initial account with only username
        account = new Account("", username, "", false, "", null);
      } else {
        account = (Account) getArguments().getSerializable("account");
      }
    }

    setupToolbar(view);

    progressBar = view.findViewById(R.id.progressBarProfile);
    progressBar.setProgress(0);

    setupGridView(view);

    headerView = getLayoutInflater().inflate(R.layout.gridview_header_account, container, false);
    setupGridViewHeader();

    // load profilePic
    try {
      Glide.with(this)
          .load(account.getImageProfilePicUrl())
          .error(ContextCompat.getDrawable(requireContext(), R.drawable.placeholder_image_error))
          .dontAnimate()
          .into(imageProfilePic);
    } catch (Exception e) {
      Log.d("ProfileFragment", Log.getStackTraceString(e));
    }

    // initialize fetch booleans
    bFirstFetch = true;
    bFirstAdapterFetch = true;

    // start fetching
    new CheckConnectionAndGetAccountImages(ProfileFragment.this).execute();

    return view;
  }

  /** Sets up gridView for posts of account */
  private void setupGridView(View view) {
    // setup gridView
    gridViewImagesOnProfile = view.findViewById(R.id.grid_view);

    // set number of columns from preferences
    setAmountOfColumnsGridView();

    // setup batch download function on gridView
    setupBatchDownloadOnGridView();
  }

  /**
   * Sets up the toolbar with specific navigation icon and the menu to copy the link to the profile
   * or to download the profile photo of the profile
   *
   * @param view View
   */
  private void setupToolbar(View view) {
    if (view == null) return;

    toolbar = view.findViewById(R.id.toolbar);
    toolbar.setNavigationIcon(R.drawable.ic_arrow_back_24dp);

    toolbar.setNavigationOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            requireActivity().onBackPressed();
          }
        });

    // set menu to copy account
    toolbar.inflateMenu(R.menu.menu_account);
    toolbar.setOnMenuItemClickListener(
        new Toolbar.OnMenuItemClickListener() {
          @Override
          public boolean onMenuItemClick(MenuItem item) {
            if (item.getItemId() == R.id.menu_copy_account_link) {
              copyAccountToClipboard();
            } else if (item.getItemId() == R.id.menu_download_account_profile_photo) {
              downloadProfilePhoto();
            }
            return false;
          }
        });
  }

  /** Starts async task to download a profile photo */
  private void downloadProfilePhoto() {
    if (account == null) return;

    // start save image from url
    DownloadImage downloadImageAsyncTask =
        new DownloadImage(
            ProfileFragment.this, account.getImageProfilePicUrl(), account.getUsername());
    downloadImageAsyncTask.execute();
  }

  /** Copies link to account to clipboard and displays toast to user */
  private void copyAccountToClipboard() {
    ClipboardManager clipboard = getSystemService(requireContext(), ClipboardManager.class);
    ClipData clip = ClipData.newPlainText("urlToCopy", createUrlForCopyAccount());
    if (clipboard != null) {
      clipboard.setPrimaryClip(clip);
      // make toast that link has been copied
      FragmentHelper.showToast(
          getResources().getString(R.string.link_copied), requireActivity(), requireContext());
    }
  }

  /**
   * Creates a specific string to copy to clipboard. Adds advertising string or returns just the url
   *
   * @return String
   */
  private CharSequence createUrlForCopyAccount() {
    if (account.getUsername() == null) return "";

    if (FragmentHelper.addAdvertisingStringToClipboard(ProfileFragment.this)) {
      return "https://www.instagram.com/"
          + account.getUsername()
          + getResources().getString(R.string.copy_account_second_part);
    } else {
      return "https://www.instagram.com/" + account.getUsername();
    }
  }

  /** Sets up the download of multiple posts after long pressing on one post */
  private void setupBatchDownloadOnGridView() {
    if (gridViewImagesOnProfile != null) {
      gridViewImagesOnProfile.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);

      // mark posts as checked for download. Set static reference to call method finish on mode when
      // trying to close contextual menu
      AbsListView.MultiChoiceModeListener multiChoiceModeListener =
          new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(
                ActionMode mode, int position, long id, boolean checked) {
              mode.setTitle(
                  ""
                      + gridViewImagesOnProfile.getCheckedItemCount()
                      + " "
                      + getString(R.string.contextual_menu_profile_download_text));

              int offset = getOffsetFromSharedPreferences();
              if (posts != null && !posts.isEmpty()) {
                // mark posts as checked
                posts.get(position - offset).toggleChecked();
                adapter.notifyDataSetChanged();
              }
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
              MenuInflater menuInflater = mode.getMenuInflater();
              menuInflater.inflate(R.menu.menu_contextual_actionbar_download_posts, menu);

              // set static reference to call method finish on mode when trying to close
              // contextual menu
              actionMode = mode;

              return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
              return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
              // get all checked posts in arrayList
              if (item.getItemId() == R.id.menu_download_multiple) {
                postsToDownload = new ArrayList<>();
                if (posts != null) {
                  for (Post post : posts) {
                    if (post != null && post.isChecked()) {
                      postsToDownload.add(post);
                    }
                  }

                  // check permissions and start batch download
                  requestPermissions(permsWriteOnly, permsRequestCode);
                }

                // exit contextual action menu
                mode.finish();
              }
              return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
              if (posts != null) {
                for (Post post : posts) {
                  if (post != null && post.isChecked()) {
                    post.toggleChecked();
                  }
                }
              }

              actionMode = null;
            }
          };

      // set multiChoiceModeListener on gridView
      gridViewImagesOnProfile.setMultiChoiceModeListener(multiChoiceModeListener);
    }
  }

  /**
   * Get offset for marking items in gridView depending on shared preferences amount columns in
   * account
   */
  private int getOffsetFromSharedPreferences() {
    int offset = 0;
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
    if (preferences != null) {
      offset =
          Integer.parseInt(
              Objects.requireNonNull(preferences.getString("key_accounts_list_columns", "3")));
    }
    return offset;
  }

  @Override
  public void onHiddenChanged(boolean hidden) {
    if (hidden) {
      // close contextual menu
      if (ProfileFragment.this.actionMode != null) {
        ProfileFragment.this.actionMode.finish();
      }
    }
    super.onHiddenChanged(hidden);
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (permsRequestCode == 200) {
      if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        //  Permission is granted. Continue the action or workflow
        //  in your app.
        // start BatchDownloadPosts async task
        new DownloadPostsBatch(ProfileFragment.this, postsToDownload, progressDialogBatch)
            .execute();
      } else {
        //  Explain to the user that the feature is unavailable because
        //  the features requires a permission that the user has denied.
        //  At the same time, respect the user's decision. Don't link to
        //  system settings in an effort to convince the user to change
        //  their decision.
        FragmentHelper.showToast(
            getResources().getString(R.string.permission_denied),
            requireActivity(),
            requireContext());
      }
    }
  }

  /** Sets the amount of columns of the gridView from sharedPreferences */
  private void setAmountOfColumnsGridView() {
    // get the amount of columns from settings
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
    if (preferences != null) {
      String amountColumns = preferences.getString("key_accounts_list_columns", "3");

      // set columns in gridView
      if (amountColumns != null) {
        gridViewImagesOnProfile.setNumColumns(Integer.parseInt(amountColumns));
      }
    }
  }

  /** Sets the EndlessScrollListener on the gridViewImgsOnProfile */
  private void createOnEndlessScrollListener() {
    scrollListener =
        new EndlessScrollListener(
            gridViewImagesOnProfile,
            new EndlessScrollListener.RefreshList() {
              @Override
              public void onRefresh(int pageNumber) {
                // get more account images when end of gridView
                // is reached
                getAccountImages();
              }
            });
  }

  /** Starts the async task to get posts from account */
  private void getAccountImages() {
    if (account == null) return;
    GetMoreAccountInfoAndImages taskGetMoreAccountInfoAndImages =
        new GetMoreAccountInfoAndImages(ProfileFragment.this);
    taskGetMoreAccountInfoAndImages.execute();
  }

  /** Hides the progressBar */
  private void hideProgressBar() {
    if (this.getActivity() != null) {
      requireActivity()
          .runOnUiThread(
              new Runnable() {
                @Override
                public void run() {
                  progressBar.setVisibility(GONE);
                }
              });
    }
  }

  /** Sets up headerView of gridView posts and set onClickListener to go to PostFragment on click */
  private void setupGridViewHeader() {
    // add header view to gridView
    gridViewImagesOnProfile.addHeaderView(headerView);

    // set onItemClickListener
    gridViewImagesOnProfile.setOnItemClickListener(
        new AdapterView.OnItemClickListener() {
          @Override
          public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Post postToSend = posts.get(position);
            goToPostFragment(postToSend);
          }
        });

    // setup view imageProfile as fullscreen image
    setupFullScreenImage();

    // setup buttonFollow onClickListener
    buttonFollow = headerView.findViewById(R.id.buttonFollow);
    buttonFollow.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            followAccount();
          }
        });
  }

  /** Saves an account in internal storage in its proper representation (StorageRep) */
  private void followAccount() {
    if (getContext() == null) return;

    if (!StorageHelper.checkIfAccountOrPostIsInFile(
        account, StorageHelper.FILENAME_ACCOUNTS, requireContext())) {
      // insert account in internal storage on device
      boolean saved = false;

      try {
        saved = StorageHelper.storeAccountInInternalStorage(account, requireContext());
      } catch (Exception e) {
        Log.d("ProfileFragment", Log.getStackTraceString(e));

        // handle failed storing of account with note to user
        FragmentHelper.notifyUserOfProblem(this, Error.ACCOUNT_COULD_NOT_BE_FOLLOWED);
      }

      // update button
      if (saved) {
        followButtonToUnfollow();
      } else {
        followButtonToFollow();
      }
    } else {
      // remove account from list
      Boolean removed = StorageHelper.removeAccountFromInternalStorage(account, requireContext());

      // update button
      if (removed) {
        followButtonToFollow();
      } else {
        followButtonToUnfollow();
      }
    }
  }

  /**
   * Go to profileFragment with click on postToSend
   *
   * @param postToSend postToSend
   */
  private void goToPostFragment(Post postToSend) {
    // new postFragment
    PostFragment postFragment = PostFragment.newInstance(postToSend);

    // add fragment to container
    FragmentHelper.addFragmentToContainer(
        postFragment, requireActivity().getSupportFragmentManager());
  }

  /** Sets up fullscreen image to see image profile picture fullscreen */
  private void setupFullScreenImage() {
    imageProfilePic = headerView.findViewById(R.id.accountOrHashtagProfilePic);
    imageProfilePic.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            if (!isImageFitToScreen) {
              // new fullscreenImageFragment
              FullscreenProfileImageFragment fullscreenProfileImageFragment;

              if (account.getImageThumbnail() != null) {
                // new fullscreenImageFragment with image profile as string
                fullscreenProfileImageFragment =
                    FullscreenProfileImageFragment.newInstance(account.getImageThumbnail());
              } else {
                // new fullscreenImageFragment with image profile pic as url
                fullscreenProfileImageFragment =
                    FullscreenProfileImageFragment.newInstance(account.getImageProfilePicUrl());
              }

              // add fragment to container
              FragmentHelper.addFragmentToContainer(
                  fullscreenProfileImageFragment, requireActivity().getSupportFragmentManager());
            }
          }
        });
  }

  /** Set buttonFollow to unfollow state */
  private void followButtonToUnfollow() {
    if (getContext() != null) {
      buttonFollow.setText(getResources().getString(R.string.button_unfollow));

      TypedValue typedValue = new TypedValue();
      Resources.Theme theme = getContext().getTheme();
      theme.resolveAttribute(R.attr.colorPrimary, typedValue, true);
      @ColorInt int color = typedValue.data;
      buttonFollow.setTextColor(color);

      theme.resolveAttribute(R.attr.colorAccent, typedValue, true);
      @ColorInt int colorBackground = typedValue.data;
      buttonFollow.setBackgroundColor(colorBackground);
    }
  }

  /** Set buttonFollow to Follow-state */
  private void followButtonToFollow() {
    if (getContext() != null) {
      buttonFollow.setText(getResources().getString(R.string.button_follow));

      TypedValue typedValue = new TypedValue();
      Resources.Theme theme = getContext().getTheme();
      theme.resolveAttribute(R.attr.colorPrimary, typedValue, true);
      @ColorInt int color = typedValue.data;
      buttonFollow.setTextColor(color);

      theme.resolveAttribute(R.attr.colorAccent, typedValue, true);
      @ColorInt int colorBackground = typedValue.data;
      buttonFollow.setBackgroundColor(colorBackground);
    }
  }

  @Override
  public void onResume() {
    super.onResume();

    // Slide down bottom navigation view if necessary
    FragmentHelper.slideDownBottomNavigationBar(getActivity());

    // button follow - unfollow
    if (!account.getIs_private()) {
      // check if account is in follow list
      if (StorageHelper.checkIfAccountOrPostIsInFile(
          account, StorageHelper.FILENAME_ACCOUNTS, getContext())) {
        // set text to "unfollow"
        followButtonToUnfollow();
      } else { // account is not followed yet
        // set text to "follow"
        followButtonToFollow();
      }
    }
  }

  /**
   * Set TextView specified with resource. Boldtext is shown bold and normalText is shown normal.
   *
   * @param resource resource of textView
   * @param boldText bold text
   * @param normalText normal text
   */
  private void updateResourceTextString(int resource, String boldText, String normalText) {
    SpannableString str = new SpannableString(boldText + normalText);
    str.setSpan(
        new StyleSpan(Typeface.BOLD), 0, boldText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    ((TextView) headerView.findViewById(resource)).setText(str);
  }

  /**
   * Updates the toolbar title with specified string myString
   *
   * @param myString string to be titled
   */
  private void updateToolbarTitle(String myString) {
    ((Toolbar) view.findViewById(R.id.toolbar)).setTitle(myString);
  }

  /** Makes the verified badge visible when account is verified */
  private void setVerifiedBadgeToVisible() {
    view.findViewById(R.id.verifiedBadge).setVisibility(View.VISIBLE);
  }

  /**
   * Async task to check internet connection and notify user if there is no connection. Starts
   * fetching at the end
   */
  private static class CheckConnectionAndGetAccountImages extends AsyncTask<Void, Void, Void> {

    private final WeakReference<ProfileFragment> fragmentReference;
    boolean isInternetAvailable = false;

    // constructor
    CheckConnectionAndGetAccountImages(ProfileFragment context) {
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
      if (isCancelled()) return;

      // get reference from fragment
      final ProfileFragment fragment = fragmentReference.get();
      if (fragment == null) return;

      if (isInternetAvailable) {
        // fetch account images
        fragment.getAccountImages();
      } else {
        FragmentHelper.notifyUserOfProblem(fragment, Error.NO_INTERNET_CONNECTION);
      }
    }
  }

  /** Async task to start fetching account images and to set common profile information */
  private static class GetMoreAccountInfoAndImages extends AsyncTask<Void, Void, Void> {

    private final WeakReference<ProfileFragment> fragmentReference;
    NetworkHandler sh;

    // constructor
    GetMoreAccountInfoAndImages(ProfileFragment context) {
      fragmentReference = new WeakReference<>(context);
    }

    @Override
    protected void onPreExecute() {
      super.onPreExecute();
      if (isCancelled()) return;

      // get reference from fragment
      final ProfileFragment fragment = fragmentReference.get();
      if (fragment == null) return;

      if (fragment.bFirstAdapterFetch) {
        // make progressBar visible
        fragment.progressBar.setVisibility(View.VISIBLE);
      }
    }

    @Override
    protected Void doInBackground(Void... arg0) {
      if (isCancelled()) return null;

      // get reference from fragment
      final ProfileFragment fragment = fragmentReference.get();
      if (fragment == null) return null;

      sh = new NetworkHandler();
      makeValidUrls();

      // only fetch if there are more pages or if it is the first fetch
      if (fragment.bFirstAdapterFetch
          || (fragment.scrollListener != null && fragment.scrollListener.hasMorePages)) {
        fetchPageDataOfUrl(fragment.url);
        int startIndex = 0;
        int endIndex = fragment.url.edgesTotalOfPage;
        fetchEdgeData(fragment.url, fragment.url.jsonArrayEdges, startIndex, endIndex);
      }
      return null;
    }

    /** Makes valid urls for account posts */
    private void makeValidUrls() {
      if (isCancelled()) return;

      // get reference from fragment
      final ProfileFragment fragment = fragmentReference.get();
      if (fragment == null) return;
      if (fragment.account == null) return;

      String urlAddress = null;

      // make url for account posts (hint: url.endCursor is null at first page fetch)
      if (fragment.url == null || (fragment.url.endCursor == null && fragment.bFirstFetch)) {
        urlAddress =
            "https://www.instagram.com/" + fragment.account.getUsername() + "/?__a=1&__d=dis";
        fragment.bFirstFetch = false;
      } else if (fragment.url.hasNextPage != null
          && fragment.url.hasNextPage
          && fragment.account.getId() != null) {
        urlAddress =
            "https://www.instagram.com/graphql/query/?query_id="
                + query_id
                + "&id="
                + fragment.account.getId()
                + "&first="
                + fragment.url.edgesTotalOfPage
                + "&after="
                + fragment.url.endCursor;
        fragment.addNextPageToPlaceholder = true;
      } else if (fragment.url.hasNextPage != null && !fragment.url.hasNextPage) {
        // no more pages or posts
        fragment.scrollListener.noMorePages();
        return;
      }
      fragment.url = new URL(urlAddress, fragment.account.getUsername(), FeedObject.ACCOUNT);
    }

    /**
     * Fetches data from a page of an url and sets information to account
     *
     * @param url URL
     */
    private void fetchPageDataOfUrl(URL url) {
      if (isCancelled()) return;

      // get reference from fragment
      final ProfileFragment fragment = fragmentReference.get();
      if (fragment == null) return;

      // fetch data for account
      String jsonStr = null;
      if (url.url != null) {
        // get json string from url
        jsonStr = sh.makeServiceCall(url.url, fragment.getClass().getSimpleName());
      } else if (!fragment.scrollListener.hasMorePages) {
        // no more pages/posts
        return;
      }

      if (jsonStr == null) {
        // only show error if there is no previous error
        if (!fragment.errorAlertAlreadyShown) {
          FragmentHelper.showNetworkOrSomethingWrongErrorToUser(fragment);
        }
        return;
      }

      try {
        if (!FragmentHelper.checkIfJsonStrIsValid(jsonStr, fragment)) {
          // set boolean errorAlertAlreadyShown = true -> to disable other following dialogs
          fragment.errorAlertAlreadyShown = true;
          return;
        }

        // file overall as json object
        JSONObject jsonObj = new JSONObject(jsonStr);
        JSONObject edge_owner_to_timeline_media;

        // get general information on first fetch
        if (!url.url.startsWith("https://www.instagram.com/graphql/query/?query_id=")) {
          // get fullName (case: its an commenter account)
          fragment.account.setFullName(
              jsonObj.getJSONObject("graphql").getJSONObject("user").getString("full_name"));

          // get private status (case: its an commenter account)
          fragment.account.setIs_private(
              jsonObj.getJSONObject("graphql").getJSONObject("user").getBoolean("is_private"));

          // get biography
          fragment.account.setBiography(
              jsonObj.getJSONObject("graphql").getJSONObject("user").getString("biography"));

          // set hd image profile pic (update profile pic)
          fragment.account.setImageProfilePicUrl(
              jsonObj
                  .getJSONObject("graphql")
                  .getJSONObject("user")
                  .getString("profile_pic_url_hd"));

          // get external url
          fragment.account.setExternal_url(
              jsonObj.getJSONObject("graphql").getJSONObject("user").getString("external_url"));

          // get followers
          fragment.account.setEdge_followed_by(
              jsonObj
                  .getJSONObject("graphql")
                  .getJSONObject("user")
                  .getJSONObject("edge_followed_by")
                  .getInt("count"));

          // get follows
          fragment.account.setEdge_follow(
              jsonObj
                  .getJSONObject("graphql")
                  .getJSONObject("user")
                  .getJSONObject("edge_follow")
                  .getInt("count"));

          // set verified status
          fragment.account.setIs_verified(
              jsonObj.getJSONObject("graphql").getJSONObject("user").getBoolean("is_verified"));

          // get edges
          edge_owner_to_timeline_media =
              jsonObj
                  .getJSONObject("graphql")
                  .getJSONObject("user")
                  .getJSONObject("edge_owner_to_timeline_media");
        } else {
          // second fetch -> get edges
          edge_owner_to_timeline_media =
              jsonObj
                  .getJSONObject("data")
                  .getJSONObject("user")
                  .getJSONObject("edge_owner_to_timeline_media");
        }

        if (edge_owner_to_timeline_media != null) {
          int itemCount = edge_owner_to_timeline_media.getInt("count");
          fragment.account.setItemCount(itemCount);
        }

        // save page_info and has_next_page
        JSONObject page_info = edge_owner_to_timeline_media.getJSONObject("page_info");
        if (page_info.getBoolean("has_next_page")) {
          url.hasNextPage = true;
          url.endCursor = page_info.getString("end_cursor");
        } else {
          url.hasNextPage = false;
        }

        JSONArray edges = edge_owner_to_timeline_media.getJSONArray("edges");
        url.jsonArrayEdges = edges;
        if (edges != null && !edges.isNull(0)) {
          url.edgesTotalOfPage = edges.length();
        } else {
          url.edgesTotalOfPage = 0;
        }
      } catch (JSONException e) {
        Log.d("ProfileFragment", Log.getStackTraceString(e));
        // only show error if there is no previous error
        if (!fragment.errorAlertAlreadyShown) {
          FragmentHelper.showNetworkOrSomethingWrongErrorToUser(fragment);
        }
      }
    }

    /**
     * Fetches data from an edge of an url and creates new posts
     *
     * @param url URL
     * @param edges JSONArray
     * @param startIndex int
     * @param endIndex int
     */
    private void fetchEdgeData(URL url, JSONArray edges, int startIndex, int endIndex) {
      if (isCancelled()) return;

      // get reference from fragment
      final ProfileFragment fragment = fragmentReference.get();
      if (fragment == null) return;

      try {
        for (int i = startIndex; i < endIndex; i++) {
          if (edges.getJSONObject(i) != null) {

            // increase post counter
            fragment.postCounter += 1;

            // get node of selected edge
            JSONObject node = edges.getJSONObject(i).getJSONObject("node");

            // check if post is sidecar
            String __typename = node.getString("__typename");
            boolean is_sidecar = __typename.equals("GraphSidecar");

            // get ownerId
            String ownerId = node.getJSONObject("owner").getString("id");
            fragment.account.setId(ownerId);

            // get caption of post
            JSONArray edgesCaption =
                node.getJSONObject("edge_media_to_caption").getJSONArray("edges");

            String caption = null;
            if (edgesCaption.length() != 0 && !edgesCaption.isNull(0)) {
              caption = edgesCaption.getJSONObject(0).getJSONObject("node").getString("text");
            }

            int likes;
            if (!url.url.startsWith("https://www.instagram.com/graphql/query/?query_id=")) {
              // graphql
              likes = node.getJSONObject("edge_liked_by").getInt("count");
            } else {
              // data
              likes = node.getJSONObject("edge_media_preview_like").getInt("count");
            }

            // create post with username from account
            Post post =
                Post.builder()
                    .id(node.getString("id"))
                    .imageUrl(node.getString("display_url"))
                    .likes(likes)
                    .ownerId(ownerId)
                    .comments(node.getJSONObject("edge_media_to_comment").getInt("count"))
                    .caption(caption)
                    .shortcode(node.getString("shortcode"))
                    .takenAtDate(new Date(node.getLong("taken_at_timestamp") * 1000))
                    .is_video(node.getBoolean("is_video"))
                    .username(fragment.account.getUsername())
                    .imageUrlProfilePicOwner(fragment.account.getImageProfilePicUrl())
                    .imageUrlThumbnail(node.getString("thumbnail_src"))
                    .is_sideCar(is_sidecar)
                    .build();

            if (fragment.posts == null) {
              fragment.posts = new ArrayList<>();

              for (int j = 0; j < url.edgesTotalOfPage; j++) {
                Post placeholder = Post.builder().build();
                fragment.posts.add(placeholder);
              }
            }
            if (fragment.posts != null && fragment.addNextPageToPlaceholder) {
              // add placeholder when fetching next page
              fragment.addNextPageToPlaceholder = false;

              // only once per next page
              for (int k = 0; k < url.edgesTotalOfPage; k++) {
                Post placeholder = Post.builder().build();
                fragment.posts.add(placeholder);
              }
            }
            assert fragment.posts != null;
            fragment.posts.set(fragment.postCounter - 1, post);
          }
        }
      } catch (JSONException e) {
        Log.d("ProfileFragment", Log.getStackTraceString(e));

        // only show error if there is no previous error
        if (!fragment.errorAlertAlreadyShown) {
          FragmentHelper.showNetworkOrSomethingWrongErrorToUser(fragment);
        }
      }
    }

    @Override
    protected void onPostExecute(Void result) {
      super.onPostExecute(result);
      if (isCancelled()) return;

      // get reference from fragment
      final ProfileFragment fragment = fragmentReference.get();
      if (fragment == null) return;

      try {
        if (fragment.bFirstAdapterFetch) {
          fragment.bFirstAdapterFetch = false;

          // hide progressBar
          fragment.hideProgressBar();

          // set adapter
          setAdapter();

          // setting text fields
          setTextFields();

          // update "posts", "following", "followers"
          updateCommonAccountInfo();

          // setting toolbar title
          fragment.updateToolbarTitle(fragment.account.getUsername());

          // set verified badge
          if (fragment.account.getIs_verified()) {
            fragment.setVerifiedBadgeToVisible();
          }

          // set info that account is private (case: commenter account)
          if (fragment.account.getIs_private()) {
            TextView textAccountIsPrivate = fragment.view.findViewById(R.id.textAccountIsPrivate);
            textAccountIsPrivate.setVisibility(View.VISIBLE);

            // disabled follow button
            fragment.buttonFollow.setAlpha(0.5f);
            fragment.buttonFollow.setEnabled(false);
          } else {
            // account is public and fetching is possible
            fragment.gridViewImagesOnProfile.setVisibility(View.VISIBLE);

            // create and set scrollListener
            fragment.createOnEndlessScrollListener();
            fragment.gridViewImagesOnProfile.setOnScrollListener(fragment.scrollListener);
          }

          // reload thumbnail image url after opening deep link
          if (fragment.account.getImageProfilePicUrl() != null
              && !fragment.account.getImageProfilePicUrl().isEmpty()) {
            ImageView imageView = fragment.headerView.findViewById(R.id.accountOrHashtagProfilePic);
            Glide.with(fragment.headerView)
                .load(fragment.account.getImageProfilePicUrl())
                .error(
                    ContextCompat.getDrawable(
                        fragment.requireContext(), R.drawable.placeholder_image_error))
                .dontAnimate()
                .into(imageView);
          }
        } else {
          // after first fetch
          if (fragment.adapter != null) {
            fragment.adapter.notifyDataSetChanged();
          }
          if (fragment.gridViewImagesOnProfile != null) {
            fragment.gridViewImagesOnProfile.invalidateViews();
          }
          // only notify hasMorePages if there are more pages
          if (fragment.scrollListener != null && fragment.scrollListener.hasMorePages) {
            fragment.scrollListener.notifyMorePages();
          }
        }
      } catch (Exception e) {
        FragmentHelper.showNetworkOrSomethingWrongErrorToUser(fragment);
      }
    }

    /** Updates common account information such as "posts", "followers" and "follows" */
    private void updateCommonAccountInfo() {
      if (isCancelled()) return;

      // get reference from fragment
      final ProfileFragment fragment = fragmentReference.get();
      if (fragment == null) return;

      fragment.updateResourceTextString(
          fragment.headerView.findViewById(R.id.textCountItems).getId(),
          Integer.toString(fragment.account.getItemCount()),
          "\nPosts");
      fragment.updateResourceTextString(
          fragment.headerView.findViewById(R.id.textFollowers).getId(),
          Integer.toString(fragment.account.getEdge_followed_by()),
          "\nFollowers");
      fragment.updateResourceTextString(
          fragment.headerView.findViewById(R.id.textFollows).getId(),
          Integer.toString(fragment.account.getEdge_follow()),
          "\nFollowing");
    }

    /** Updates the textFields under profile picture */
    private void setTextFields() {
      if (isCancelled()) return;

      // get reference from fragment
      final ProfileFragment fragment = fragmentReference.get();
      if (fragment == null) return;

      if (fragment.account.getBiography() != null) {
        fragment.updateResourceTextString(
            fragment.headerView.findViewById(R.id.textBiography).getId(),
            "",
            fragment.account.getBiography());
      } else {
        fragment.headerView.findViewById(R.id.textBiography).setVisibility(GONE);
      }

      if ((fragment.account.getExternal_url() != null)
          && !fragment.account.getExternal_url().equals("null")) {
        fragment.updateResourceTextString(
            fragment.headerView.findViewById(R.id.textExternalUrl).getId(),
            "",
            fragment.account.getExternal_url());
      } else {
        fragment.headerView.findViewById(R.id.textExternalUrl).setVisibility(GONE);
      }

      if (fragment.account.getFullName() != null) {
        fragment.updateResourceTextString(
            fragment.headerView.findViewById(R.id.textFullName).getId(),
            "",
            fragment.account.getFullName());
      } else {
        fragment.headerView.findViewById(R.id.textFullName).setVisibility(GONE);
      }
    }

    /** Sets the adapter customGridViewAdapterPost for the gridView */
    private void setAdapter() {
      if (isCancelled()) return;

      // get reference from fragment
      final ProfileFragment fragment = fragmentReference.get();
      if (fragment == null) return;

      fragment.adapter =
          new GridViewAdapterPost(
              fragment.getContext(), R.layout.gridview_item_image, fragment.posts);
      fragment.gridViewImagesOnProfile.setAdapter(fragment.adapter);
    }
  }

  //    /**
  //     * Async task to download the profile photo of the account
  //     */
  //    private static class DownloadProfilePhoto extends AsyncTask<Void, Void, Void> {
  //
  //        private final WeakReference<ProfileFragment> fragmentReference;
  //        private final String photoUrl;
  //        private final String nameAccountOrHashtag;
  //        private boolean saved = false;
  //
  //        // constructor
  //        public DownloadProfilePhoto(ProfileFragment fragment, String photoUrl, String
  //        nameAccountOrHashtag) {
  //            this.fragmentReference = new WeakReference<>(fragment);
  //            this.photoUrl = photoUrl;
  //            this.nameAccountOrHashtag = nameAccountOrHashtag;
  //        }
  //
  //        @Override
  //        protected Void doInBackground(Void... voids) {
  //            if (isCancelled()) return null;
  //
  //            // get reference from fragment
  //            final ProfileFragment fragment = fragmentReference.get();
  //            if (fragment == null) return null;
  //
  //            try {
  //                java.net.URL url = new java.net.URL(photoUrl);
  //
  //                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
  //                connection.setDoInput(true);
  //                connection.connect();
  //                InputStream input = connection.getInputStream();
  //                Bitmap myBitmap = BitmapFactory.decodeStream(input);
  //
  //                // save to storage
  //                saved = StorageHelper.saveImage(myBitmap, nameAccountOrHashtag,
  // fragment.getContext());
  //
  //                input.close();
  //            } catch (Exception e) {
  //                Log.d("ProfileFragment", Log.getStackTraceString(e));
  //            }
  //            return null;
  //        }
  //
  //        @Override
  //        protected void onPostExecute(Void aVoid) {
  //            super.onPostExecute(aVoid);
  //            if (isCancelled()) return;
  //
  //            // get reference from fragment
  //            final ProfileFragment fragment = fragmentReference.get();
  //            if (fragment == null) return;
  //
  //            if (saved) {
  //
  // FragmentHelper.showToast(fragment.getResources().getString(R.string.image_saved),
  //                                         fragment.requireActivity(), fragment.requireContext());
  //            } else {
  //
  // FragmentHelper.showToast(fragment.getResources().getString(R.string.image_saved_failed),
  //                                         fragment.requireActivity(), fragment.requireContext());
  //            }
  //        }
  //    }
}
