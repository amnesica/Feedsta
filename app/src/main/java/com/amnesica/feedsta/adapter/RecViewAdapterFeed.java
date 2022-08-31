package com.amnesica.feedsta.adapter;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.amnesica.feedsta.R;
import com.amnesica.feedsta.adapter.sidecar.StatePagerAdapterSideCar;
import com.amnesica.feedsta.asynctasks.CopyLink;
import com.amnesica.feedsta.asynctasks.bookmarks.BookmarkPost;
import com.amnesica.feedsta.asynctasks.download.DownloadMedia;
import com.amnesica.feedsta.helper.Error;
import com.amnesica.feedsta.helper.FragmentHelper;
import com.amnesica.feedsta.helper.ImageHelper;
import com.amnesica.feedsta.helper.StorageHelper;
import com.amnesica.feedsta.models.Post;
import com.amnesica.feedsta.views.ExpandableTextView;
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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

public class RecViewAdapterFeed extends RecyclerView.Adapter<RecViewAdapterFeed.Viewholder> {

  private final Context context;
  private final ArrayList<Post> posts;
  private final Fragment fragment;
  private final ArrayList<SimpleExoPlayer> listExoplayer = new ArrayList<>();

  // constructor
  public RecViewAdapterFeed(Context context, ArrayList<Post> posts, Fragment fragment) {
    this.context = context;
    this.posts = posts;
    this.fragment = fragment;
  }

  @NonNull
  @Override
  public RecViewAdapterFeed.Viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    View view = inflater.inflate(R.layout.cardview_feed_post, parent, false);
    return new Viewholder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull RecViewAdapterFeed.Viewholder holder, int position) {
    Post post = posts.get(position);
    if (post == null || context == null || fragment == null) return;
    holder.bindPostToHolder(post);

    setBasicPostInfoInViewholder(holder, post);

    if (post.getIs_sideCar()) {
      showSidecarInViewholder(holder, post);
    } else if (!post.getIs_video()) {
      showSingleImageInViewholder(holder, post);
    } else if (post.getIs_video()) {
      showSingleVideoInViewholder(holder, post);
    }
  }

  private void setBasicPostInfoInViewholder(final Viewholder holder, final Post post) {
    setupImageProfilePicture(holder, post);

    setupButtonsBelowPost(holder, post);

    setLikes(holder, post.getLikes());

    setExpandableCaption(holder, post.getCaption());

    setUsername(holder, post.getUsername());

    createListenerGoToProfileOnUsernameAppBar(holder);

    setTakenAtDate(holder, post.getTakenAtDate());
  }

  private void createListenerGoToProfileOnUsernameAppBar(Viewholder holder) {
    holder.textUsernameAppBar.setOnClickListener(
        view -> FragmentHelper.goToProfileFragment(holder.post.getUsername(), fragment));
  }

  private void setLikes(Viewholder holder, int likes) {
    holder.textLikes.setText(
        context.getResources().getString(R.string.likes, String.valueOf(likes)));
  }

  private void setTakenAtDate(Viewholder holder, @NonNull Date takenAtDate) {
    holder.textDate.setText(DateFormat.getDateTimeInstance().format(takenAtDate));
  }

  private void setUsername(Viewholder holder, @NonNull String username) {
    holder.textOwnerIdOrUsername.setText(username);
    holder.textUsernameAppBar.setText(username);
  }

  private void setExpandableCaption(Viewholder holder, @NonNull String caption) {
    holder.textCaption.makeExpandable(
        FragmentHelper.createSpannableStringWithClickableLinks(caption, fragment), 3);
  }

  private void setupImageProfilePicture(final Viewholder holder, final Post post) {
    ImageHelper.loadImageWithGlide(
        holder.imageProfilePicPostOwner,
        post.getImageUrlProfilePicOwner(),
        R.drawable.placeholder_image_error,
        context);

    ImageHelper.setupListenerToShowFullscreenProfileImage(
        (AppCompatActivity) context,
        post.getImageUrlProfilePicOwner(),
        holder.imageProfilePicPostOwner);
  }

  private void setupButtonsBelowPost(Viewholder holder, Post post) {
    holder.buttonCopyLink.setBackgroundResource(R.drawable.ic_content_copy_24dp);
    holder.buttonCopyLink.setOnClickListener(
        listener -> new CopyLink(post, fragment, holder.buttonCopyLink).copyLink());

    holder.buttonDownload.setBackgroundResource(R.drawable.ic_file_download_outline_24dp);
    holder.buttonDownload.setOnClickListener(
        listener ->
            new DownloadMedia(
                    post, holder.buttonDownload, fragment, holder.viewPager2.getCurrentItem())
                .startDownloadMedia());

    setInitialButtonBookmarkBackground(holder.buttonBookmark, post, fragment);
    holder.buttonBookmark.setOnClickListener(
        listener -> new BookmarkPost(post, fragment, holder.buttonBookmark).bookmarkPost());
  }

  private void setInitialButtonBookmarkBackground(
      ImageButton buttonBookmark, Post post, Fragment fragment) {
    boolean bookmarked =
        StorageHelper.checkIfAccountOrPostIsInFile(
            post, StorageHelper.FILENAME_BOOKMARKS, fragment.requireContext());

    if (bookmarked) {
      // saved
      buttonBookmark.setBackgroundResource(R.drawable.ic_bookmark_24dp);
    } else {
      // not saved yet
      buttonBookmark.setBackgroundResource(R.drawable.ic_bookmark_border_24dp);
    }
  }

  private void showSingleVideoInViewholder(final Viewholder holder, final Post post) {
    showOnlyMediaFrame(holder);
    setupPreviewVideoThumbnail(holder, post);
    releasePlayer(holder.player);
  }

  private void showOnlyMediaFrame(final Viewholder holder) {
    holder.viewPager2.setVisibility(GONE);
    holder.imageViewPost.setVisibility(GONE);
    holder.mainMediaFrameLayout.setVisibility(VISIBLE);
  }

  private void setupPreviewVideoThumbnail(final Viewholder holder, final Post post) {
    setVideoThumbnail(holder, post);
    holder.imageViewPlayerPreview.setOnClickListener(
        view -> {
          loadVideoFromUrl(holder, post);
          holder.player.setPlayWhenReady(true);
          holder.playerView.setVisibility(VISIBLE);
        });
  }

  private void setVideoThumbnail(Viewholder holder, Post post) {
    if (holder.playerView.getVisibility() == VISIBLE) {
      holder.playerView.setVisibility(GONE);
      holder.mainMediaFrameLayout.getLayoutParams().height = post.getHeight();
    }

    ImageHelper.loadFirstVideoFrameWithGlide(
        holder.imageViewPlayerPreview,
        post.getVideoUrl(),
        R.drawable.placeholder_video_error,
        context);
    holder.imageViewPlayerPreview.getLayoutParams().height = post.getHeight();
    holder.imageViewPlayerPreview.setVisibility(VISIBLE);
  }

  private void showSingleImageInViewholder(Viewholder holder, Post post) {
    showOnlyImageViewPost(holder);

    ImageHelper.loadImageWithGlide(
        holder.imageViewPost, post.getImageUrl(), R.drawable.placeholder_image_error, context);

    ImageHelper.setupListenerToShowFullscreenPostImage(
        (AppCompatActivity) context, post.getImageUrl(), holder.imageViewPost);
  }

  private void showOnlyImageViewPost(Viewholder holder) {
    holder.viewPager2.setVisibility(GONE);
    holder.imageViewPost.setVisibility(VISIBLE);
  }

  private void showSidecarInViewholder(Viewholder holder, Post post) {
    if (holder == null || post == null) return;

    showOnlyViewpager(holder);

    try {
      setAdapterForSidecar(holder, post);
    } catch (IllegalStateException | NullPointerException e) {
      Log.d("RecViewAdapterFeed", Log.getStackTraceString(e));

      FragmentHelper.notifyUserOfProblem(fragment, Error.POST_NOT_AVAILABLE_ANYMORE);
    }
  }

  private void showOnlyViewpager(Viewholder holder) {
    holder.imageViewPost.setVisibility(GONE);
    holder.viewPager2.setVisibility(VISIBLE);
  }

  private void setAdapterForSidecar(Viewholder holder, Post post) {
    holder.statePagerAdapterSideCar =
        new StatePagerAdapterSideCar(fragment.requireActivity(), post);

    holder.viewPager2.setOffscreenPageLimit(1);
    holder.viewPager2.setSaveFromParentEnabled(false);
    holder.viewPager2.setAdapter(holder.statePagerAdapterSideCar);

    // attach tab layout to viewpager
    new TabLayoutMediator(holder.tabLayoutViewpager, holder.viewPager2, (tab, position) -> {})
        .attach();
  }

  /** Releases player and stops playing video */
  void releasePlayer(final SimpleExoPlayer player) {
    if (player != null) {
      player.setPlayWhenReady(false);
    }
  }

  void initVideoPlayerAgainAfterDetaching(final Viewholder holder) {
    if (holder.player != null) {
      setupPreviewVideoThumbnail(holder, holder.post);
      showSingleVideoInViewholder(holder, holder.post);
    }
  }

  private void loadVideoFromUrl(Viewholder holder, Post post) {
    if (post.getVideoUrl() == null || context == null) return;

    makeMainMediaLayoutVisibleAndSetHeight(holder, post);

    initDialogToHoldFullscreenVideo(holder);

    initFullscreenVideoButton(holder);

    initExoPlayer(holder, post);
  }

  private void makeMainMediaLayoutVisibleAndSetHeight(Viewholder holder, Post post) {
    holder.mainMediaFrameLayout.setVisibility(VISIBLE);
    holder.mainMediaFrameLayout.getLayoutParams().height = post.getHeight();
  }

  private void initExoPlayer(final Viewholder holder, Post post) {
    createExoPlayerInstance(holder, post);

    holder.playerView.setPlayer(holder.player);

    hideControlsInitially(holder);

    setupAdjustmentOfVideoSizeWhenSizeChanges(holder);

    seekToPreviousPositionIfThereIsOne(holder);
  }

  private void hideControlsInitially(Viewholder holder) {
    holder.playerView.setControllerAutoShow(false);
    holder.playerView.hideController();

    boolean showControls = shouldVideoControlsBeDisplayed(context);
    if (!showControls) {
      setupMuteUnmuteOnVideoClick(holder);
    } else {
      setupVideoVolumeButton(holder);
    }

    if (!showControls) {
      // hide controller when visibility of playerView changes
      holder.playerView.setControllerVisibilityListener(
          i -> {
            if (i == 0) {
              holder.playerView.hideController();
            }
          });
    }
  }

  private void seekToPreviousPositionIfThereIsOne(Viewholder holder) {
    boolean haveResumePosition = holder.resumeWindow != C.INDEX_UNSET;
    if (haveResumePosition) {
      holder.player.seekTo(holder.resumeWindow, holder.resumePosition);
      if (holder.videoMuted) {
        holder.player.setVolume(0f);
      } else {
        holder.player.setVolume(1f);
      }
    }
  }

  private void createExoPlayerInstance(@NonNull final Viewholder holder, final Post post) {
    holder.player = ExoPlayerFactory.newSimpleInstance(fragment.requireActivity());
    holder.player.addListener(
        new Player.EventListener() {
          @Override
          public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            Player.EventListener.super.onPlayerStateChanged(playWhenReady, playbackState);
            if (playbackState == Player.STATE_READY) {
              holder.imageViewPlayerPreview.setVisibility(GONE);
            }
          }
        });

    // add exoplayer to list to be able to stop video playing later (used in FeedFragment when
    // another Fragment opens)
    listExoplayer.add(holder.player);

    // create data source factory
    createDataSourceFactory(holder);

    // provide media to be played by exoplayer with videoUrl
    MediaSource videoSource = getMediaSource(holder, post);
    holder.player.prepare(videoSource);

    // loop video
    holder.player.setRepeatMode(Player.REPEAT_MODE_ALL);

    // mute initially
    holder.player.setVolume(0f);

    // play video when ready
    holder.player.setPlayWhenReady(false);
  }

  @NonNull
  private MediaSource getMediaSource(@NonNull Viewholder holder, Post post) {
    return new ProgressiveMediaSource.Factory(holder.dataSourceFactory)
        .createMediaSource(Uri.parse(post.getVideoUrl()));
  }

  private void createDataSourceFactory(@NonNull Viewholder holder) {
    holder.dataSourceFactory =
        new DefaultDataSourceFactory(
            context, Util.getUserAgent(context, fragment.requireActivity().getPackageName()));
  }

  /**
   * Set up VideoListener to adjust size and aspect ratio of video when size changes, e.g. when
   * fullscreen mode is entered. Boolean fullscreenIsPortrait is set to true here
   */
  private void setupAdjustmentOfVideoSizeWhenSizeChanges(final Viewholder holder) {
    holder.player.addVideoListener(
        new VideoListener() {
          //  this is where we will resize view to fit aspect ratio of video
          @Override
          public void onVideoSizeChanged(
              int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {

            if (width <= height) {
              // set boolean to stay in portrait mode fullscreen if video was made in portrait
              // mode
              holder.fullscreenIsPortrait = true;
              try {
                // lock orientation in portrait mode then
                forceOrientation(fragment, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
              } catch (Exception e) {
                Log.d("PostVideoFragment", Log.getStackTraceString(e));
              }
            }

            //  get layout params of view
            ViewGroup.LayoutParams p = holder.mainMediaFrameLayout.getLayoutParams();
            int currWidth = holder.mainMediaFrameLayout.getWidth();

            //  set new width/height of view. Height or width must be cast to float as int/int
            //  will give 0 and distort view, e.g. 9/16 = 0 but 9.0/16 = 0.5625. p.height is
            //  int hence the final cast to int
            p.width = currWidth;
            p.height = (int) ((float) height / width * currWidth);

            //  redraw layout
            holder.mainMediaFrameLayout.requestLayout();
          }
        });
  }

  private void forceOrientation(final Fragment fragment, int requestedOrientation) {
    fragment.requireActivity().setRequestedOrientation(requestedOrientation);
  }

  private void setupVideoVolumeButton(final Viewholder holder) {
    holder.volumeButton.setOnClickListener(
        view -> {
          if (holder.videoMuted) {
            holder.volumeButton.setImageDrawable(
                ContextCompat.getDrawable(
                    fragment.requireActivity(), R.drawable.ic_baseline_volume_up_24dp));
            if (holder.player != null) {
              holder.player.setVolume(1f);
            }
            holder.videoMuted = false;
          } else {
            holder.volumeButton.setImageDrawable(
                ContextCompat.getDrawable(
                    fragment.requireActivity(), R.drawable.ic_baseline_volume_off_24dp));
            if (holder.player != null) {
              holder.player.setVolume(0f);
            }
            holder.videoMuted = true;
          }
        });
  }

  /** Set up listener to mute or unmute video on click */
  private void setupMuteUnmuteOnVideoClick(final Viewholder holder) {
    holder.playerView.setOnClickListener(
        view -> {
          // single click -> mute/unmute video
          if (holder.player != null
              && holder.player.isPlaying()
              && holder.player.getVolume() > 0f) {

            // mute video
            holder.player.setVolume(0f);
            holder.videoMuted = true;
          } else {

            // unmute video
            if (holder.player != null) {
              holder.player.setVolume(1f);
              holder.videoMuted = false;
            }
          }
        });
  }

  /** Initialize dialog with fullscreen video */
  private void initDialogToHoldFullscreenVideo(final Viewholder holder) {
    holder.fullScreenDialog =
        new Dialog(fragment.requireActivity(), android.R.style.Theme_Black_NoTitleBar_Fullscreen) {
          public void onBackPressed() {
            if (holder.exoPlayerIsInFullscreenMode) closeFullscreenDialog(holder);
            super.onBackPressed();
          }
        };
  }

  /** Opens the dialog with the fullscreen video */
  private void openFullscreenDialog(final Viewholder holder) {
    try {
      if (holder.fullscreenIsPortrait) {
        // stay in portrait mode if video was made in portrait mode
        forceOrientation(fragment, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
      } else {
        // change to landscape mode otherwise
        forceOrientation(fragment, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
      }
    } catch (Exception e) {
      Log.d("PostVideoFragment", Log.getStackTraceString(e));
    }

    // remove old view and show video fullscreen in dialog
    ((ViewGroup) holder.playerView.getParent()).removeView(holder.playerView);
    holder.fullScreenDialog.addContentView(
        holder.playerView,
        new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    holder.fullScreenIcon.setImageDrawable(
        ContextCompat.getDrawable(
            fragment.requireActivity(), R.drawable.ic_baseline_fullscreen_exit_24dp));
    holder.exoPlayerIsInFullscreenMode = true;
    holder.fullScreenDialog.show();
  }

  private void closeFullscreenDialog(final Viewholder holder) {
    try {
      // force orientation to portrait mode
      forceOrientation(fragment, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    } catch (Exception e) {
      Log.d("PostVideoFragment", Log.getStackTraceString(e));
    }

    ((ViewGroup) holder.playerView.getParent()).removeView(holder.playerView);
    holder.mainMediaFrameLayout.addView(holder.playerView);

    holder.exoPlayerIsInFullscreenMode = false;

    holder.fullScreenDialog.dismiss();

    // set image to open fullscreen again
    holder.fullScreenIcon.setImageDrawable(
        ContextCompat.getDrawable(
            fragment.requireActivity(), R.drawable.ic_baseline_fullscreen_24dp));
  }

  private void initFullscreenVideoButton(final Viewholder holder) {
    final PlayerControlView controlView = holder.playerView.findViewById(R.id.exo_controller);
    holder.fullScreenIcon = controlView.findViewById(R.id.exo_fullscreen_icon);

    final FrameLayout fullScreenButton = controlView.findViewById(R.id.exo_fullscreen_button);
    fullScreenButton.setOnClickListener(
        view -> {
          if (!holder.exoPlayerIsInFullscreenMode) openFullscreenDialog(holder);
          else closeFullscreenDialog(holder);
        });
  }

  private boolean shouldVideoControlsBeDisplayed(Context context) {
    if (context == null) return false;

    // get the amount of columns from settings
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    if (preferences != null) {
      if (preferences.contains(context.getResources().getString(R.string.videoControls))) {
        return preferences.getBoolean(
            context.getResources().getString(R.string.videoControls), false);
      }
    }

    return false;
  }

  public void releaseAllExoPlayers() {
    for (SimpleExoPlayer player : listExoplayer) {
      releasePlayer(player);
    }
  }

  @Override
  public int getItemCount() {
    return posts.size();
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public int getItemViewType(int position) {
    return position;
  }

  @Override
  public void onViewDetachedFromWindow(@NonNull Viewholder holder) {
    if (holder.statePagerAdapterSideCar != null) {
      holder.statePagerAdapterSideCar.finishAllRegisteredFragments();
    }

    releasePlayer(holder.player);
    super.onViewDetachedFromWindow(holder);
  }

  @Override
  public boolean onFailedToRecycleView(@NonNull Viewholder holder) {
    releasePlayer(holder.player);
    return super.onFailedToRecycleView(holder);
  }

  @Override
  public void onViewAttachedToWindow(@NonNull Viewholder holder) {
    initVideoPlayerAgainAfterDetaching(holder);
    super.onViewAttachedToWindow(holder);
  }

  @Override
  public void onViewRecycled(@NonNull Viewholder holder) {
    releasePlayer(holder.player);
    super.onViewRecycled(holder);
  }

  public static class Viewholder extends RecyclerView.ViewHolder {
    private final ImageView imageViewPost, imageProfilePicPostOwner;
    private final ViewPager2 viewPager2;
    private StatePagerAdapterSideCar statePagerAdapterSideCar;
    private final ImageButton buttonBookmark, buttonDownload, buttonCopyLink;
    private final TextView textLikes, textOwnerIdOrUsername, textUsernameAppBar, textDate;
    private final ExpandableTextView textCaption;
    private final TabLayout tabLayoutViewpager;

    // exoplayer stuff
    private SimpleExoPlayer player;
    private final PlayerView playerView;
    private final ImageView imageViewPlayerPreview;
    private final FrameLayout mainMediaFrameLayout;
    private ImageView fullScreenIcon;
    private Dialog fullScreenDialog;
    private final ImageView volumeButton;
    private DataSource.Factory dataSourceFactory;
    private int resumeWindow;
    private long resumePosition;
    private boolean exoPlayerIsInFullscreenMode = false;
    private boolean videoMuted = true;
    private boolean fullscreenIsPortrait = false;

    // necessary to enable video playing after view was detached
    private Post post;

    public Viewholder(@NonNull View itemView) {
      super(itemView);
      textLikes = itemView.findViewById(R.id.likes2);
      textOwnerIdOrUsername = itemView.findViewById(R.id.ownerIdOrUsername);
      textCaption = itemView.findViewById(R.id.caption);
      textUsernameAppBar = itemView.findViewById(R.id.textUsernameInBarUnderAppBar);
      textDate = itemView.findViewById(R.id.date);
      imageProfilePicPostOwner = itemView.findViewById(R.id.imageProfilePicPostOwner);
      imageViewPost = itemView.findViewById(R.id.singleImagePost);
      viewPager2 = itemView.findViewById(R.id.viewpagerPost);
      buttonBookmark = itemView.findViewById(R.id.buttonBookmark);
      buttonCopyLink = itemView.findViewById(R.id.buttonCopyLink);
      buttonDownload = itemView.findViewById(R.id.buttonSaveImageOrVideo);
      tabLayoutViewpager = itemView.findViewById(R.id.tabLayoutViewpager);

      // Exoplayer
      playerView = itemView.findViewById(R.id.exoplayer_feed);
      imageViewPlayerPreview = itemView.findViewById(R.id.exoplayer_feed_preview);
      mainMediaFrameLayout = itemView.findViewById(R.id.main_media_frame_feed);
      volumeButton = playerView.findViewById(R.id.exo_volume_icon);
    }

    void bindPostToHolder(final Post post) {
      this.post = post;
    }
  }
}
