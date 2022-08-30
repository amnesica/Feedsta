package com.amnesica.feedsta.fragments;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.widget.ViewPager2;

import com.amnesica.feedsta.R;
import com.amnesica.feedsta.adapter.ListAdapterComment;
import com.amnesica.feedsta.adapter.sidecar.StatePagerAdapterSideCar;
import com.amnesica.feedsta.asynctasks.CopyLink;
import com.amnesica.feedsta.asynctasks.bookmarks.BookmarkPost;
import com.amnesica.feedsta.asynctasks.download.DownloadMedia;
import com.amnesica.feedsta.fragments.fullscreenimages.FullscreenImagePostFragment;
import com.amnesica.feedsta.helper.Error;
import com.amnesica.feedsta.helper.FragmentHelper;
import com.amnesica.feedsta.helper.NetworkHandler;
import com.amnesica.feedsta.helper.StorageHelper;
import com.amnesica.feedsta.helper.feed.FeedObject;
import com.amnesica.feedsta.models.Account;
import com.amnesica.feedsta.models.Comment;
import com.amnesica.feedsta.models.Post;
import com.amnesica.feedsta.models.URL;
import com.amnesica.feedsta.models.sidecar.Sidecar;
import com.amnesica.feedsta.models.sidecar.SidecarEntry;
import com.amnesica.feedsta.models.sidecar.SidecarEntryType;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoListener;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

/** Fragment for displaying a post */
public class PostFragment extends Fragment {

  // view stuff
  private TextView textLikes;
  private TextView textOwnerIdOrUsername;
  private TextView textCaption;
  private TextView textUsernameAppBar;
  private TextView textDate;
  private ImageView imageViewPost;
  private ImageView imageProfilePicPostOwner;
  private ListView listComments;
  private ImageButton buttonLoadMoreComments;
  private ProgressBar progressBarComments;
  private View header;
  private ViewPager2 viewPager2;
  private ImageButton buttonBookmark;
  private StatePagerAdapterSideCar statePagerAdapterSideCar;
  private ProgressBar progressBar;
  private ImageButton imageButtonDownload;
  private ImageButton buttonCopyLink;
  public ProgressDialog progressDialogBatch;
  private TabLayout tabLayoutViewpager;

  // booleans
  private Boolean scrolling;
  private Boolean bFirstFetch;
  private Boolean bFirstAdapterFetch;

  // exoplayer for videoView
  private SimpleExoPlayer player;
  private PlayerView playerView;
  private FrameLayout mainMediaFrameLayout;
  private ImageView fullScreenIcon;
  private Dialog fullScreenDialog;
  private DataSource.Factory dataSourceFactory;
  private int resumeWindow;
  private long resumePosition;
  private boolean exoPlayerIsInFullscreenMode = false;
  private boolean videoMuted = true;
  private boolean fullscreenIsPortrait = false;

  // domain classes
  private URL url;
  private Post post;
  private ArrayList<Comment> comments;

  // if post is from deep link -> get more info about post
  private Boolean postIsFromDeepLink = false;

  // states
  private final String STATE_RESUME_WINDOW = "resumeWindow";
  private final String STATE_RESUME_POSITION = "resumePosition";
  private final String STATE_PLAYER_FULLSCREEN = "playerFullscreen";
  private final String STATE_VIDEO_MUTED = "videoMuted";
  private final String STATE_FULLSCREEN_IS_PORTRAIT = "fullscreenIsPortrait";

  public static PostFragment newInstance(Post post) {
    PostFragment fragment = new PostFragment();
    Bundle b = new Bundle();
    b.putSerializable("post", post);
    fragment.setArguments(b);
    return fragment;
  }

  // public access to call it from MainActivity for deep linking intent
  public static PostFragment newInstance(String shortcode) {
    PostFragment fragment = new PostFragment();
    Bundle b = new Bundle();
    b.putSerializable("shortcode", shortcode);
    fragment.setArguments(b);
    return fragment;
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    // inflate the layout for this fragment
    View view = inflater.inflate(R.layout.fragment_post, container, false);

    getPostFromArguments();

    setupToolbar(view);
    setupProgressBar(view);
    setupHeaderView();
    setupFooterView(view);

    initializeBookmarkedButton();

    // check connection and set post info (e.g. textFields and sidecar)
    checkConnectionAndSetPostInfo();

    restorePositionOfVideo(savedInstanceState);

    return view;
  }

  private void getPostFromArguments() {
    post = null;
    if (getArguments() != null) {
      if (getArguments().getSerializable("shortcode") != null) {
        String shortcode = (String) getArguments().getSerializable("shortcode");

        // initial post with only shortcode
        post = Post.builder().shortcode(shortcode).build();
        postIsFromDeepLink = true;
      } else {
        post = (Post) getArguments().getSerializable("post");
      }
    }
  }

  private void setupProgressBar(View view) {
    progressBar = view.findViewById(R.id.progressBarPost);
    progressBar.setVisibility(VISIBLE);
  }

  private void restorePositionOfVideo(Bundle savedInstanceState) {
    if (savedInstanceState != null) {
      resumeWindow = savedInstanceState.getInt(STATE_RESUME_WINDOW);
      resumePosition = savedInstanceState.getLong(STATE_RESUME_POSITION);
      exoPlayerIsInFullscreenMode = savedInstanceState.getBoolean(STATE_PLAYER_FULLSCREEN);
      videoMuted = savedInstanceState.getBoolean(STATE_VIDEO_MUTED);
      fullscreenIsPortrait = savedInstanceState.getBoolean(STATE_FULLSCREEN_IS_PORTRAIT);
    }
  }

  private void setupToolbar(View view) {
    if (view == null) return;

    Toolbar toolbar = view.findViewById(R.id.toolbar);
    toolbar.setTitle(getResources().getString(R.string.toolbar_title_post));
    FragmentHelper.setupToolbarWithBackButton(toolbar, new WeakReference<>(PostFragment.this));
  }

  /**
   * Calls async task CheckConnectionAndGetPostInfo to check internet connection and start fetching
   */
  private void checkConnectionAndSetPostInfo() {
    new CheckConnectionAndGetPostInfo(PostFragment.this).execute();
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    outState.putInt(STATE_RESUME_WINDOW, resumeWindow);
    outState.putLong(STATE_RESUME_POSITION, resumePosition);
    outState.putBoolean(STATE_PLAYER_FULLSCREEN, exoPlayerIsInFullscreenMode);
    outState.putBoolean(STATE_VIDEO_MUTED, videoMuted);
    outState.putBoolean(STATE_FULLSCREEN_IS_PORTRAIT, fullscreenIsPortrait);
    super.onSaveInstanceState(outState);
  }

  private void setupImageButtonDownload() {
    imageButtonDownload = header.findViewById(R.id.buttonSaveImageOrVideo);
    imageButtonDownload.setBackgroundResource(R.drawable.ic_file_download_outline_24dp);
    imageButtonDownload.setOnClickListener(
        view -> {
          DownloadMedia downloadMedia =
              new DownloadMedia(
                  post, imageButtonDownload, PostFragment.this, viewPager2.getCurrentItem());
          downloadMedia.startDownloadMedia();
        });
  }

  private void disableButtonsUnderPost() {
    if (buttonBookmark != null && buttonCopyLink != null && imageButtonDownload != null) {
      buttonBookmark.setEnabled(false);
      buttonCopyLink.setEnabled(false);
      imageButtonDownload.setEnabled(false);
    }
  }

  private void hideProgressBar() {
    if (this.getActivity() != null) {
      requireActivity().runOnUiThread(() -> progressBar.setVisibility(GONE));
    }
  }

  /** Starts async task GetMoreInfoPost */
  private void startGetMorePostInfoTask() {
    try {
      GetMorePostInfo getMorePostInfo = new GetMorePostInfo(PostFragment.this);
      getMorePostInfo.execute();
    } catch (Exception e) {
      Log.d("PostFragment", Log.getStackTraceString(e));
    }
  }

  /** Initializes bookmarked button. Button is shown completely black if bookmark already exits */
  private void initializeBookmarkedButton() {
    // setting bookmarkButton
    Boolean bBookmarked =
        StorageHelper.checkIfAccountOrPostIsInFile(
            post, StorageHelper.FILENAME_BOOKMARKS, getContext());

    // set button background resource for bookmarkButton
    if (bBookmarked != null && bBookmarked) {
      setButtonBookmarkedToSaved();
    } else {
      setButtonBookmarkedToNotSaved();
    }
  }

  /**
   * Sets up footer with list of comments
   *
   * @param view View
   * @param footer View
   */
  private void setupListComments(View view, View footer) {
    if (view == null || footer == null) return;

    listComments = view.findViewById(R.id.listViewComments);

    // add footer view to comments list
    listComments.addFooterView(footer);

    // click on comment to go to commenter account
    listComments.setOnItemClickListener(
        (parent, view1, position, id) -> {
          if (comments != null && comments.get(position - 1) != null) {
            // get comment from list
            Comment commentToGoTo = comments.get(position - 1);

            if (commentToGoTo != null) {
              // create account
              Account account =
                  new Account(
                      commentToGoTo.getOwnerProfilePicUrl(),
                      commentToGoTo.getUsername(),
                      "",
                      false,
                      commentToGoTo.getId(),
                      null);
              // go to profile
              goToProfileFragment(account);
            }
          }
        });
  }

  /**
   * Sets up the footer View with button "load more comments"
   *
   * @param view View
   */
  private void setupFooterView(View view) {
    if (view == null) return;

    View footer = getLayoutInflater().inflate(R.layout.listview_footer_post, null);
    // find button and set load more comments background resource
    buttonLoadMoreComments = footer.findViewById(R.id.buttonLoadMoreComments);
    buttonLoadMoreComments.setBackgroundResource(R.drawable.ic_add_circle_outline_48dp);

    progressBarComments = footer.findViewById(R.id.progressbarComments);

    setupListComments(view, footer);
  }

  /** Find views for header elements and sets button bookmark and button copyLink */
  private void setupHeaderView() {
    header = getLayoutInflater().inflate(R.layout.listview_header_post, null);
    if (header == null) return;

    // find header views
    textLikes = header.findViewById(R.id.likes);
    textOwnerIdOrUsername = header.findViewById(R.id.ownerIdOrUsername);
    textCaption = header.findViewById(R.id.caption);
    textUsernameAppBar = header.findViewById(R.id.textUsernameInBarUnderAppBar);
    textDate = header.findViewById(R.id.date);
    imageProfilePicPostOwner = header.findViewById(R.id.imageProfilePicPostOwner);
    imageViewPost = header.findViewById(R.id.singleImagePost);
    viewPager2 = header.findViewById(R.id.viewpagerPost);
    tabLayoutViewpager = header.findViewById(R.id.tabLayoutViewpager);

    // make links highlighted and clickable in textView
    textCaption.setMovementMethod(LinkMovementMethod.getInstance());

    // on click listener to show post image fullscreen to zoom into image
    // (only enable listener for posts with actual url, because
    // BigImageViewer (for viewing image fullscreen) cannot handle base64 encoded images that!)
    imageViewPost.setOnClickListener(
        view -> {
          if (post.getImageUrl() != null) {
            goToFullscreenImagePostFragment();
          }
        });

    setupBookmarkButton(header);

    // set viewpager for sidecar and normal images and video
    viewPager2.setOffscreenPageLimit(1);

    // clickListener go to users profile on tap on appBar
    RelativeLayout relBarUnderAppBar = header.findViewById(R.id.BarUnderAppBar);
    relBarUnderAppBar.setOnClickListener(
        v -> {
          Account accountToGoTo =
              new Account(
                  post.getImageUrlProfilePicOwner(),
                  post.getUsername(),
                  "",
                  false,
                  post.getOwnerId(),
                  null);

          goToProfileFragment(accountToGoTo);
        });

    setupCopyLinkButton(header);
  }

  private void goToFullscreenImagePostFragment() {
    // start fullscreen image post fragment
    FullscreenImagePostFragment fullscreenImagePostFragment =
        FullscreenImagePostFragment.newInstance(post.getImageUrl());

    // add fullscreenImagePostFragment to FragmentManager
    FragmentHelper.addFragmentToContainer(
        fullscreenImagePostFragment, requireActivity().getSupportFragmentManager());
  }

  /**
   * Sets up the button to bookmark a post
   *
   * @param header View
   */
  private void setupBookmarkButton(View header) {
    if (header == null) return;

    // set button bookmark
    buttonBookmark = header.findViewById(R.id.buttonBookmark);
    buttonBookmark.setOnClickListener(
        v -> {
          BookmarkPost bookmarkPost = new BookmarkPost(post, PostFragment.this, buttonBookmark);
          bookmarkPost.bookmarkPost();
        });
  }

  /**
   * Sets up the button to copy a link
   *
   * @param header View
   */
  private void setupCopyLinkButton(View header) {
    if (header == null) return;

    // set button copyLink
    buttonCopyLink = header.findViewById(R.id.buttonCopyLink);
    buttonCopyLink.setBackgroundResource(R.drawable.ic_content_copy_24dp);

    buttonCopyLink.setOnClickListener(
        listener -> new CopyLink(post, PostFragment.this, buttonCopyLink).copyLink());
  }

  /**
   * Go to profileFragment with click on postToSend
   *
   * @param accountToGoTo Account
   */
  private void goToProfileFragment(Account accountToGoTo) {
    // release and stop video player if exits
    releasePlayer();

    // new profileFragment
    ProfileFragment profileFragment = ProfileFragment.newInstance(accountToGoTo);

    // add fragment to container
    FragmentHelper.addFragmentToContainer(
        profileFragment, requireActivity().getSupportFragmentManager());
  }

  /** Sets the button for bookmarked post to state "saved" */
  private void setButtonBookmarkedToSaved() {
    buttonBookmark.setBackgroundResource(R.drawable.ic_bookmark_24dp);
  }

  /** Sets the button for bookmarked post to state "not saved" */
  private void setButtonBookmarkedToNotSaved() {
    buttonBookmark.setBackgroundResource(R.drawable.ic_bookmark_border_24dp);
  }

  @Override
  public void onResume() {
    // reset screen orientation to portrait if video was fullscreen before and app was exited
    if (exoPlayerIsInFullscreenMode) closeFullscreenDialog();

    // resume with video
    if (header != null
        && header.findViewById(R.id.main_media_frame_post).getVisibility() == VISIBLE
        && post != null
        && post.getVideoUrl() != null) {
      loadVideoFromUrl(post.getVideoUrl());
    }

    // Slide down bottom navigation view if necessary
    FragmentHelper.slideDownBottomNavigationBar(getActivity());

    super.onResume();
  }

  @Override
  public void onPause() {
    super.onPause();

    // release video player
    if (playerView != null && player != null) {
      resumeWindow = player.getCurrentWindowIndex();
      resumePosition = Math.max(0, player.getContentPosition());

      player.release();
    }

    // dismiss fullScreen dialog
    if (fullScreenDialog != null) {
      fullScreenDialog.dismiss();
    }

    stopAllVideoTasksInRegisteredFragments();
  }

  @Override
  public void onStop() {
    super.onStop();
    stopAllVideoTasksInRegisteredFragments();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    stopAllVideoTasksInRegisteredFragments();
  }

  @Override
  public void onHiddenChanged(boolean hidden) {
    if (hidden) {
      stopAllVideoTasksInRegisteredFragments();
    } else {
      // load video from url for normal video post
      if (post != null && post.getIs_video() && !post.getIs_sideCar()) {
        if (post.getVideoUrl() != null) {
          loadVideoFromUrl(post.getVideoUrl());
        }
      }

      // if post is sidecar, reload whole adapter (workaround)
      if (post != null && post.getIs_sideCar()) {
        if (viewPager2 != null && statePagerAdapterSideCar != null) {
          // reload adapter
          viewPager2.setAdapter(statePagerAdapterSideCar);
        }
      }

      // reload bookmark info
      Boolean postAlreadyBookmarked =
          StorageHelper.checkIfAccountOrPostIsInFile(
              post, StorageHelper.FILENAME_BOOKMARKS, getContext());
      if (postAlreadyBookmarked) {
        // set button background resource
        setButtonBookmarkedToSaved();
      } else {
        // not saved yet
        setButtonBookmarkedToNotSaved();

        // reset category on post
        post.setCategory(null);
      }
    }
    super.onHiddenChanged(hidden);
  }

  /** Releases and stops the video player */
  private void releasePlayer() {
    if (player != null) {
      try {
        player.stop();
        player.release();
      } catch (Exception e) {
        Log.d("PostFragment", Log.getStackTraceString(e));
      }
    }
  }

  /** Releases the video player and all players in registered fragments */
  private void stopAllVideoTasksInRegisteredFragments() {
    // release video player
    releasePlayer();

    // finish all video sidecar views with release of player
    if (statePagerAdapterSideCar != null) {
      statePagerAdapterSideCar.finishAllRegisteredFragments();
    }
  }

  /**
   * Loads and plays a video with the videoUrl
   *
   * @param videoUrl String
   */
  private void loadVideoFromUrl(String videoUrl) {
    if (videoUrl == null || getContext() == null) return;

    dataSourceFactory =
        new DefaultDataSourceFactory(
            getContext(), Util.getUserAgent(getContext(), requireActivity().getPackageName()));

    if (playerView == null) {
      // initialize playerView
      playerView = header.findViewById(R.id.exoplayer_post);
      playerView.setVisibility(VISIBLE);

      // make frame visible
      mainMediaFrameLayout = header.findViewById(R.id.main_media_frame_post);
      mainMediaFrameLayout.setVisibility(VISIBLE);

      initDialogToHoldFullscreenVideo();
      initializeFullscreenVideoButton();
    }

    initializeExoPlayer(videoUrl);

    if (exoPlayerIsInFullscreenMode) {
      ((ViewGroup) playerView.getParent()).removeView(playerView);
      fullScreenDialog.addContentView(
          playerView,
          new ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

      // set image to close fullscreen
      fullScreenIcon.setImageDrawable(
          ContextCompat.getDrawable(
              requireActivity(), R.drawable.ic_baseline_fullscreen_exit_24dp));
      fullScreenDialog.show();
    }
  }

  /**
   * Initialize exoplayer video
   *
   * @param videoUrl String
   */
  private void initializeExoPlayer(String videoUrl) {
    // TODO Refactoring Exoplayer/Video
    boolean showControls = shouldControlsBeDisplayed(requireContext());

    if (!showControls) {
      setupMuteUnmuteOnVideoClick();
    } else {
      setupVideoVolumeButton();
    }

    // new player instance
    player = ExoPlayerFactory.newSimpleInstance(requireActivity());
    playerView.setPlayer(player);

    // hide controller initially
    playerView.setControllerAutoShow(false);
    playerView.hideController();

    if (!showControls) {
      // hide controller when visibility of playerView changes
      playerView.setControllerVisibilityListener(
          i -> {
            if (i == 0) {
              playerView.hideController();
            }
          });
    }

    // provide media to be played by exoplayer with videoUrl
    MediaSource videoSource =
        new ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(Uri.parse(videoUrl));
    player.prepare(videoSource);

    // loop video
    player.setRepeatMode(Player.REPEAT_MODE_ALL);

    // mute initially
    player.setVolume(0f);

    // play video when ready
    player.setPlayWhenReady(true);

    // rescale and resize video to screen
    setupAdjustmentOfVideoSizeWhenSizeChanges();

    // seek to previous position if there is one
    boolean haveResumePosition = resumeWindow != C.INDEX_UNSET;
    if (haveResumePosition) {
      player.seekTo(resumeWindow, resumePosition);
      if (videoMuted) {
        player.setVolume(0f);
      } else {
        player.setVolume(1f);
      }
    }
  }

  /**
   * Set up VideoListener to adjust size and aspect ratio of video when size changes, e.g. when
   * fullscreen mode is entered. Boolean fullscreenIsPortrait is set to true here
   */
  private void setupAdjustmentOfVideoSizeWhenSizeChanges() {
    player.addVideoListener(
        new VideoListener() {
          //  this is where we will resize view to fit aspect ratio of video
          @Override
          public void onVideoSizeChanged(
              int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {

            if (width <= height) {
              // set boolean to stay in portrait mode fullscreen if video was made in portrait
              // mode
              fullscreenIsPortrait = true;
              try {
                // lock orientation in portrait mode then
                requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
              } catch (Exception e) {
                Log.d("PostFragment", Log.getStackTraceString(e));
              }
            }

            //  get layout params of view
            ViewGroup.LayoutParams p = mainMediaFrameLayout.getLayoutParams();
            int currWidth = mainMediaFrameLayout.getWidth();

            //  set new width/height of view. Height or width must be cast to float as int/int
            //  will give 0 and distort view, e.g. 9/16 = 0 but 9.0/16 = 0.5625. p.height is
            //  int hence the final cast to int
            p.width = currWidth;
            p.height = (int) ((float) height / width * currWidth);

            //  redraw layout
            mainMediaFrameLayout.requestLayout();
          }
        });
  }

  /** Set up video volume button to mute video and show different image */
  private void setupVideoVolumeButton() {
    final ImageView volumeButton = playerView.findViewById(R.id.exo_volume_icon);

    // mute video clickListener
    volumeButton.setOnClickListener(
        view -> {
          if (videoMuted) {
            volumeButton.setImageDrawable(
                ContextCompat.getDrawable(
                    requireActivity(), R.drawable.ic_baseline_volume_up_24dp));
            if (player != null) {
              player.setVolume(1f);
            }
            videoMuted = false;
          } else {
            volumeButton.setImageDrawable(
                ContextCompat.getDrawable(
                    requireActivity(), R.drawable.ic_baseline_volume_off_24dp));
            if (player != null) {
              player.setVolume(0f);
            }
            videoMuted = true;
          }
        });
  }

  /** Set up listener to mute or unmute video on click */
  private void setupMuteUnmuteOnVideoClick() {
    playerView.setOnClickListener(
        v -> {
          // single click -> mute/unmute video
          if (player != null && player.isPlaying() && player.getVolume() > 0f) {

            // mute video
            player.setVolume(0f);
            videoMuted = true;
          } else {

            // unmute video
            if (player != null) {
              player.setVolume(1f);
              videoMuted = false;
            }
          }
        });
  }

  /** Initialize dialog with fullscreen video */
  private void initDialogToHoldFullscreenVideo() {
    fullScreenDialog =
        new Dialog(requireActivity(), android.R.style.Theme_Black_NoTitleBar_Fullscreen) {
          public void onBackPressed() {
            if (exoPlayerIsInFullscreenMode) closeFullscreenDialog();
            super.onBackPressed();
          }
        };
  }

  /** Opens the dialog with the fullscreen video */
  private void openFullscreenDialog() {
    try {
      if (fullscreenIsPortrait) {
        // stay in portrait mode if video was made in portrait mode
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
      } else {
        // change to landscape mode otherwise
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
      }
    } catch (Exception e) {
      Log.d("PostFragment", Log.getStackTraceString(e));
    }

    // remove old view and show video fullscreen in dialog
    ((ViewGroup) playerView.getParent()).removeView(playerView);
    fullScreenDialog.addContentView(
        playerView,
        new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    fullScreenIcon.setImageDrawable(
        ContextCompat.getDrawable(requireActivity(), R.drawable.ic_baseline_fullscreen_exit_24dp));
    exoPlayerIsInFullscreenMode = true;
    fullScreenDialog.show();
  }

  /** Closes the dialog the fullscreen video */
  private void closeFullscreenDialog() {
    try {
      // force orientation to portrait mode
      requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    } catch (Exception e) {
      Log.d("PostFragment", Log.getStackTraceString(e));
    }

    ((ViewGroup) playerView.getParent()).removeView(playerView);
    mainMediaFrameLayout.addView(playerView);

    exoPlayerIsInFullscreenMode = false;

    fullScreenDialog.dismiss();

    // set image to open fullscreen again
    fullScreenIcon.setImageDrawable(
        ContextCompat.getDrawable(requireActivity(), R.drawable.ic_baseline_fullscreen_24dp));
  }

  /** Initialize custom button in exoplayer controls to open or close fullscreen video */
  private void initializeFullscreenVideoButton() {
    final PlayerControlView controlView = playerView.findViewById(R.id.exo_controller);
    fullScreenIcon = controlView.findViewById(R.id.exo_fullscreen_icon);

    final FrameLayout fullScreenButton = controlView.findViewById(R.id.exo_fullscreen_button);
    fullScreenButton.setOnClickListener(
        view -> {
          if (!exoPlayerIsInFullscreenMode) openFullscreenDialog();
          else closeFullscreenDialog();
        });
  }

  /**
   * Checks if video controller should be displayed
   *
   * @param context Context
   * @return boolean
   */
  private boolean shouldControlsBeDisplayed(Context context) {
    if (context == null) return false;

    // get the amount of columns from settings
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    if (preferences != null) {
      if (preferences.contains(getResources().getString(R.string.videoControls))) {
        return preferences.getBoolean(getResources().getString(R.string.videoControls), false);
      }
    }

    return false;
  }

  /** Sets the button download to state "saved" */
  public void setButtonDownloadToSaved() {
    requireActivity()
        .runOnUiThread(
            () -> {
              try {
                imageButtonDownload.setBackgroundResource(R.drawable.ic_file_download_24dp);
              } catch (Exception e) {
                Log.d("PostFragment", Log.getStackTraceString(e));
              }
            });
  }

  /**
   * Async task to check internet connection and notify user if there is no connection. Starts
   * fetching at the end
   */
  private static class CheckConnectionAndGetPostInfo extends AsyncTask<Void, Void, Void> {

    private final WeakReference<PostFragment> fragmentReference;
    boolean isInternetAvailable = false;

    CheckConnectionAndGetPostInfo(PostFragment context) {
      fragmentReference = new WeakReference<>(context);
    }

    @Override
    protected Void doInBackground(Void... voids) {
      if (isCancelled()) return null;
      isInternetAvailable = NetworkHandler.isInternetAvailable();
      return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
      super.onPostExecute(aVoid);
      if (isCancelled()) return;

      // get reference from fragment
      final PostFragment fragment = fragmentReference.get();
      if (fragment == null) return;

      if (isInternetAvailable) {
        // get more info about post (sidecar, video, image)
        fragment.startGetMorePostInfoTask();
      } else {
        // check if post is bookmark and string imageThumbnail is available
        if (fragment.postHasThumbnailImageAsString()) {
          fragment.showImageThumbnailOfBookmark();
        } else {
          // disable all buttons if image as string could not get displayed
          fragment.disableButtonsUnderPost();
        }

        FragmentHelper.notifyUserOfProblem(fragment, Error.NO_INTERNET_CONNECTION);

        fragment.hideProgressBar();
      }
    }
  }

  /**
   * Checks if imageThumbnailUrl of post starts with "https://instagram". If so the post was not
   * bookmarked regarding the url of the thumbnail, otherwise it would be saved as string
   *
   * @return boolean
   */
  private boolean postHasThumbnailImageAsString() {
    // check if image thumbnail of post is available and is base64 encoded string
    return post != null
        && post.getImageThumbnail() != null
        && !post.getImageThumbnail().startsWith("https://instagram");
  }

  /**
   * Shows thumbnail of bookmark in imageViewPost. This might be useful when post/bookmark is not
   * available anymore. Instead of image url the saved image string is displayed
   */
  private void showImageThumbnailOfBookmark() {
    // check if image thumbnail of post is available and is base64 encoded string
    if (post == null || imageViewPost == null || post.getImageThumbnail() == null) return;

    // make progressBar gone
    hideProgressBar();

    // hide other layout elements
    changeVisibilityOfUiElementsDependingOnPostType(true);

    // set height of imageView
    setHeightOfImageViewPost();

    // set basic post info like likes, caption, username etc.
    setBasicPostInfo();

    // set up download Button
    setupImageButtonDownload();

    // load image into view
    Glide.with(requireContext())
        .asBitmap()
        .load(Base64.decode(post.getImageThumbnail(), Base64.DEFAULT))
        .error(ContextCompat.getDrawable(requireContext(), R.drawable.placeholder_image_error))
        .diskCacheStrategy(DiskCacheStrategy.NONE)
        .skipMemoryCache(true)
        .dontAnimate()
        .into(imageViewPost);

    // add header and update view
    listComments.addHeaderView(header);
    ListAdapterComment adapter =
        new ListAdapterComment(
            getContext(), R.layout.list_item_comment, new ArrayList<Comment>(), this);
    listComments.setAdapter(adapter);
    buttonLoadMoreComments.setVisibility(GONE);
  }

  /**
   * Sets the height of the imageViewPost. If no height is given the height is then set to 1000px
   */
  private void setHeightOfImageViewPost() {
    if (post == null || imageViewPost == null) return;

    if (post.getHeight() == 0) {
      // set height value for bookmarks without this value (old bookmarks)
      post.setHeight(1000);
    }

    // set height of imageView
    imageViewPost.getLayoutParams().height = post.getHeight();
    imageViewPost.requestLayout();
    imageViewPost.invalidate();
  }

  /** Set basic post info like likes, caption, username etc. */
  private void setBasicPostInfo() {
    if (getActivity() == null) return;

    // load profile pic app bar
    Glide.with(this)
        .load(post.getImageUrlProfilePicOwner())
        .error(ContextCompat.getDrawable(requireContext(), R.drawable.placeholder_image_error))
        .dontAnimate()
        .into(imageProfilePicPostOwner);

    // set Likes
    textLikes.setText(getResources().getString(R.string.likes, String.valueOf(post.getLikes())));

    // set caption (with clickable links)
    if (post.getCaption() != null) {
      textCaption.setText(
          FragmentHelper.createSpannableStringWithClickableLinks(post.getCaption(), this));
    }

    // set username or ownerId
    if (post.getUsername() == null && post.getOwnerId() != null) {
      textOwnerIdOrUsername.setText(post.getOwnerId());
      textUsernameAppBar.setText(post.getOwnerId());
    } else {
      textOwnerIdOrUsername.setText(post.getUsername());
      // set title in bar under app bar
      textUsernameAppBar.setText(post.getUsername());
    }

    // set date
    if (post.getTakenAtDate() != null) {
      textDate.setText(DateFormat.getDateTimeInstance().format(post.getTakenAtDate()));
    }
  }

  /**
   * Makes UI elements visible depending on the type of the post. Parameter postIsBookmark is needed
   * to make imageViewPost visible regardless of type
   *
   * @param postIsBookmark boolean
   */
  private void changeVisibilityOfUiElementsDependingOnPostType(boolean postIsBookmark) {
    if (post == null) return;

    if (postIsBookmark) {
      // set viewpager to gone and make imageViewPost visible regardless of type
      viewPager2.setVisibility(GONE);
      imageViewPost.setVisibility(VISIBLE);
      return;
    }

    if (!post.getIs_sideCar() && !post.getIs_video()) {
      // post is simple image
      viewPager2.setVisibility(GONE);
      imageViewPost.setVisibility(VISIBLE);
    }
    if (!post.getIs_sideCar() && post.getIs_video()) {
      // post is simple video
      viewPager2.setVisibility(GONE);
      imageViewPost.setVisibility(GONE);
    }
  }

  /** Async task to get more info about post */
  private static class GetMorePostInfo extends AsyncTask<Void, Void, Void> {

    private final WeakReference<PostFragment> fragmentReference;
    private NetworkHandler sh;

    GetMorePostInfo(PostFragment context) {
      fragmentReference = new WeakReference<>(context);
    }

    @Override
    protected void onPreExecute() {
      super.onPreExecute();
      if (isCancelled()) return;

      // get reference from fragment
      final PostFragment fragment = fragmentReference.get();
      if (fragment == null) return;

      // make ui elements visible and gone depending on type of post
      fragment.changeVisibilityOfUiElementsDependingOnPostType(false);

      // initialize booleans for first fetching
      fragment.bFirstFetch = true;
      fragment.bFirstAdapterFetch = true;
      fragment.scrolling = false;

      // make progressBar visible
      try {
        fragment.progressBar.setVisibility(View.VISIBLE);
      } catch (Exception e) {
        Log.d("PostFragment", Log.getStackTraceString(e));
      }
    }

    @Override
    protected Void doInBackground(Void... voids) {
      if (isCancelled()) return null;

      sh = new NetworkHandler();
      URL url;
      url = makeValidUrlForVideo();
      getMoreInfoFromPost(url);

      return null;
    }

    /**
     * Makes valid URLs for posts
     *
     * @return String
     */
    private URL makeValidUrlForVideo() {
      if (isCancelled()) return null;

      // get reference from fragment
      final PostFragment fragment = fragmentReference.get();
      if (fragment == null) return null;

      String urlAddress =
          "https://www.instagram.com/p/" + fragment.post.getShortcode() + "/?__a=1&__d=dis";
      return fragment.url = new URL(urlAddress, fragment.post.getShortcode(), FeedObject.ACCOUNT);
    }

    /**
     * Gets more info from post
     *
     * @param url URL
     */
    private void getMoreInfoFromPost(URL url) {
      if (isCancelled()) return;

      // get reference from fragment
      final PostFragment fragment = fragmentReference.get();
      if (fragment == null) return;

      String newUrl = url.url;

      // get json string from url
      String jsonStr = sh.makeServiceCall(newUrl, this.getClass().getSimpleName());

      if (jsonStr == null) {
        FragmentHelper.showNetworkOrSomethingWrongErrorToUser(fragment);
        fragment.disableButtonsUnderPost();
        return;
      }

      // something went wrong -> possible rate limit reached
      if (!FragmentHelper.checkIfJsonStrIsValid(jsonStr, fragment)) {
        return;
      }

      // file overall as json object
      JSONObject jsonObj;
      try {
        jsonObj = new JSONObject(jsonStr);

        // getting through overall structure
        JSONObject graphql = jsonObj.getJSONObject("graphql");
        JSONObject shortcode_media = graphql.getJSONObject("shortcode_media");

        // if post is from deep link set basic info first
        if (fragment.postIsFromDeepLink) {
          fragment.post.setId(shortcode_media.getString("id"));
          fragment.post.setTakenAtDate(
              new Date(Long.parseLong(shortcode_media.getString("taken_at_timestamp")) * 1000));
          fragment.post.setIs_video(shortcode_media.getBoolean("is_video"));
          fragment.post.setImageUrlThumbnail(
              shortcode_media.getJSONArray("display_resources").getJSONObject(0).getString("src"));
          fragment.post.setIs_sideCar(
              shortcode_media.getString("__typename").equals("GraphSidecar"));
        }

        // get likes, username, ownerId, comments (int), caption and imageUrl (full size)
        fragment.post.setLikes(
            shortcode_media.getJSONObject("edge_media_preview_like").getInt("count"));
        fragment.post.setUsername(shortcode_media.getJSONObject("owner").getString("username"));
        fragment.post.setOwnerId(shortcode_media.getJSONObject("owner").getString("id"));
        fragment.post.setComments(
            shortcode_media.getJSONObject("edge_media_preview_comment").getInt("count"));

        // get height of image for single image post
        fragment.post.setHeight(
            shortcode_media
                .getJSONArray("display_resources")
                .getJSONObject(2)
                .getInt("config_height"));

        // get caption of post
        JSONArray edgesCaption =
            shortcode_media.getJSONObject("edge_media_to_caption").getJSONArray("edges");
        String caption = null;
        if (edgesCaption.length() != 0) {
          JSONObject captionNode = edgesCaption.getJSONObject(0);
          JSONObject captionNodeString = captionNode.getJSONObject("node");
          caption = captionNodeString.getString("text");
        }
        fragment.post.setCaption(caption);
        fragment.post.setImageUrl(shortcode_media.getString("display_url"));
        fragment.post.setImageUrlProfilePicOwner(
            shortcode_media.getJSONObject("owner").getString("profile_pic_url"));

        // hint: active workaround: only comments under shortcode-url can be fetched ->
        // no more than 24 comments can be fetched at the moment
        JSONObject edge_media_to_parent_comment =
            shortcode_media.getJSONObject("edge_media_to_parent_comment");

        // save page_info and has_next_page
        JSONObject page_info = edge_media_to_parent_comment.getJSONObject("page_info");

        JSONArray edgesComments = edge_media_to_parent_comment.getJSONArray("edges");

        url.jsonArrayEdges = edgesComments;
        if (edgesComments != null) {
          url.edgesTotalOfPage = edgesComments.length();
        } else {
          url.edgesTotalOfPage = 0;
        }
        int startIndex = 0;
        int endIndex;
        if (url.edgesTotalOfPage == 1) {
          endIndex = 1; // if there is only one comment
        } else {
          endIndex = url.edgesTotalOfPage - 1;
        }

        // initialize comments to get a view
        if (fragment.comments == null) {
          fragment.comments = new ArrayList<>();
        }

        if (edgesComments.length() > 0) {
          // set edge data for comments
          fragment.comments = getEdgeDataComments(edgesComments, startIndex, endIndex);
        }

        if (fragment.post.getIs_sideCar()) {
          // get sidecar urls if post is sidecar
          JSONObject edge_sidecar_to_children =
              shortcode_media.getJSONObject("edge_sidecar_to_children");
          JSONArray edgesSidecar = edge_sidecar_to_children.getJSONArray("edges");

          // get sidecar urls
          Sidecar sidecar = getSidecarFromEdge(edgesSidecar);

          // set sidecar of post
          fragment.post.setSidecar(sidecar);
        } else if (fragment.post.getIs_video()) {
          // get video url of just a normal video - no sidecar
          String video_url = shortcode_media.getString("video_url");
          fragment.post.setVideoUrl(video_url);
        }
      } catch (JSONException | IllegalStateException e) {
        Log.d("PostFragment", Log.getStackTraceString(e));

        FragmentHelper.showNetworkOrSomethingWrongErrorToUser(fragment);
        fragment.disableButtonsUnderPost();
      }
    }

    /**
     * Fetches all urls and types of all sidecar entries
     *
     * @param edges JSONArray
     * @return Sidecar
     * @throws JSONException JSONException
     */
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

    /**
     * Gets comments from JSONArray edges
     *
     * @param edges JSONArray
     * @param startIndex int
     * @param endIndex int
     * @return ArrayList<Comment>
     */
    private ArrayList<Comment> getEdgeDataComments(JSONArray edges, int startIndex, int endIndex)
        throws JSONException {
      if (isCancelled()) return null;

      // get reference from fragment
      final PostFragment fragment = fragmentReference.get();
      if (fragment == null) return null;

      ArrayList<Comment> comments = new ArrayList<>();

      // just 24 comments per request
      for (int i = startIndex; i < endIndex; i++) {

        // get edge object
        JSONObject edge = edges.getJSONObject(i);

        // get node of selected edge
        JSONObject node = edge.getJSONObject("node");

        // get id of posts (not userId)
        String id = node.getString("id");

        // get text of comment
        String text = node.getString("text");

        // get date of comment
        Date takenAt = new Date(Long.parseLong(String.valueOf(node.getLong("created_at"))) * 1000);

        // get owner
        JSONObject owner = node.getJSONObject("owner");

        // get profile pic url of comment
        String profile_pic_url = owner.getString("profile_pic_url");
        String username = owner.getString("username");

        // create post and add to list
        Comment comment = new Comment(id, text, takenAt, profile_pic_url, username);
        comments.add(comment);
      }
      return comments;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
      super.onPostExecute(aVoid);
      if (isCancelled()) return;

      // get reference from fragment
      final PostFragment fragment = fragmentReference.get();
      if (fragment == null) return;

      // make progressBar gone
      fragment.hideProgressBar();

      // set adapters
      if (fragment.post.getIs_sideCar()) {
        // post is sidecar
        if (fragment.postIsFromDeepLink) {
          fragment.imageViewPost.setVisibility(GONE);
          fragment.viewPager2.setVisibility(VISIBLE);
        }

        // if sidecarUrls are null, try to show the post image as string if it exists (post to show
        // might be bookmark)
        if (fragment.post.getSidecar() == null && fragment.postHasThumbnailImageAsString()) {
          fragment.showImageThumbnailOfBookmark();
          return;
        }

        try {
          // set adapter for sidecars
          fragment.statePagerAdapterSideCar =
              new StatePagerAdapterSideCar(fragment.requireActivity(), fragment.post);
          fragment.viewPager2.setSaveFromParentEnabled(false);
          fragment.viewPager2.setAdapter(fragment.statePagerAdapterSideCar);
          // attach tab layout to viewpager
          new TabLayoutMediator(
                  fragment.tabLayoutViewpager, fragment.viewPager2, (tab, position) -> {})
              .attach();

        } catch (IllegalStateException | NullPointerException e) {
          Log.d("PostFragment", Log.getStackTraceString(e));

          FragmentHelper.notifyUserOfProblem(fragment, Error.POST_NOT_AVAILABLE_ANYMORE);

          fragment.disableButtonsUnderPost();

          return;
        }
      } else if (fragment.post.getIs_video()) {
        // is only video
        // if videoUrl is null, try to show the post image as string if it exists (post to show
        // might be bookmark)
        if (fragment.post.getVideoUrl() == null && fragment.postHasThumbnailImageAsString()) {
          fragment.showImageThumbnailOfBookmark();
          return;
        }

        // load video from url
        fragment.loadVideoFromUrl(fragment.post.getVideoUrl());

      } else {
        // is only image
        if (fragment.getContext() == null) return;

        // set height of imageView
        fragment.setHeightOfImageViewPost();

        // if imageUrl is null, try to show the image as string if it exists (post to show might be
        // bookmark)
        if (fragment.post.getImageUrl() == null && fragment.postHasThumbnailImageAsString()) {
          fragment.showImageThumbnailOfBookmark();
          return;
        }

        // load image with glide (note: error retries loading the image from the url)
        Glide.with(fragment.requireContext())
            .load(fragment.post.getImageUrl())
            .error(Glide.with(fragment.requireContext()).load(fragment.post.getImageUrl()))
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .dontAnimate()
            .into(fragment.imageViewPost);
      }

      // set basic post info like likes, caption, username etc.
      fragment.setBasicPostInfo();

      // set up download Button
      fragment.setupImageButtonDownload();

      if (fragment.postIsFromDeepLink) {
        // do bookmark button initialize again
        fragment.initializeBookmarkedButton();
      }

      // add post header
      fragment.listComments.addHeaderView(fragment.header);

      // update comment list view
      fragment.listComments.deferNotifyDataSetChanged();
      fragment.listComments.invalidateViews();

      // get comments to post
      try {
        // workaround to see comments (actual fetching does not work anymore)
        if (fragment.comments != null && fragment.bFirstAdapterFetch) {
          // set adapter on first fetch
          fragment.bFirstAdapterFetch = false;

          try {
            ListAdapterComment adapter =
                new ListAdapterComment(
                    fragment.getContext(), R.layout.list_item_comment, fragment.comments, fragment);
            fragment.listComments.setAdapter(adapter);
          } catch (NullPointerException e) {
            Log.d("PostFragment", Log.getStackTraceString(e));
          }
        } else {
          fragment.listComments.deferNotifyDataSetChanged();

          fragment.progressBarComments.setVisibility(GONE);
          fragment.buttonLoadMoreComments.setVisibility(GONE);
        }

        fragment.buttonLoadMoreComments.setVisibility(GONE);

      } catch (NullPointerException e) {
        try {
          ListAdapterComment adapter =
              new ListAdapterComment(
                  fragment.getContext(),
                  R.layout.list_item_comment,
                  new ArrayList<Comment>(),
                  fragment);
          fragment.listComments.setAdapter(adapter);
        } catch (NullPointerException ee) {
          Log.d("PostFragment", Log.getStackTraceString(e));
        }
      }
    }
  }
}
