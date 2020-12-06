package com.amnesica.feedsta.adapter;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.amnesica.feedsta.R;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
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

import static android.view.View.VISIBLE;

/**
 * Displays a video in a sideCar
 */
public class PostVideoFragment extends Fragment {

    // view stuff
    private View v;
    private ProgressBar progressBar;

    // exoplayer stuff
    private SimpleExoPlayer player;
    private String sVideoUrl;
    private PlayerView playerView;
    private FrameLayout mainMediaFrameLayout;
    private ImageView fullScreenIcon;
    private Dialog fullScreenDialog;
    private DataSource.Factory dataSourceFactory;
    private int resumeWindow;
    private long resumePosition;
    private boolean exoPlayerFullscreen = false;
    private boolean videoMuted = true;
    private boolean fullscreenIsPortrait = false;

    // states
    private final String STATE_RESUME_WINDOW = "resumeWindow";
    private final String STATE_RESUME_POSITION = "resumePosition";
    private final String STATE_PLAYER_FULLSCREEN = "playerFullscreen";
    private final String STATE_VIDEO_MUTED = "videoMuted";
    private final String STATE_FULLSCREEN_IS_PORTRAIT = "fullscreenIsPortrait";

    static Fragment newInstance(String videoUrl) {
        PostVideoFragment f = new PostVideoFragment();
        Bundle b = new Bundle();

        // put videoUrl as argument
        b.putString("videoUrl", videoUrl);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.fragment_viewpager_video_post, container, false);

        // get views
        progressBar = v.findViewById(R.id.progress_bar);

        // get videoUrl from arguments
        String videoUrl = null;
        if (getArguments() != null) {
            videoUrl = getArguments().getString("videoUrl");
        }
        if (videoUrl != null) {
            sVideoUrl = videoUrl;
        }

        // get positions of video
        if (savedInstanceState != null) {
            resumeWindow = savedInstanceState.getInt(STATE_RESUME_WINDOW);
            resumePosition = savedInstanceState.getLong(STATE_RESUME_POSITION);
            exoPlayerFullscreen = savedInstanceState.getBoolean(STATE_PLAYER_FULLSCREEN);
            videoMuted = savedInstanceState.getBoolean(STATE_VIDEO_MUTED);
            fullscreenIsPortrait = savedInstanceState.getBoolean(STATE_FULLSCREEN_IS_PORTRAIT);
        }

        return v;
    }

    /**
     * Loads and plays a video with the video_url
     *
     * @param video_url String url of video
     */
    private void loadVideoFromUrl(String video_url) {
        if (video_url != null && getContext() != null) {
            dataSourceFactory = new DefaultDataSourceFactory(getContext(), Util.getUserAgent(getContext(), requireActivity().getPackageName()));

            if (playerView == null) {
                playerView = v.findViewById(R.id.exoplayer_vp);
                playerView.setVisibility(VISIBLE);

                // make frame visible
                mainMediaFrameLayout = v.findViewById(R.id.main_media_frame_vp);
                mainMediaFrameLayout.setVisibility(VISIBLE);

                initFullscreenDialog();
                initFullscreenButton();
            }

            initExoPlayer(video_url);

            if (exoPlayerFullscreen) {
                ((ViewGroup) playerView.getParent()).removeView(playerView);
                fullScreenDialog.addContentView(playerView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                fullScreenIcon.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.ic_baseline_fullscreen_exit_24));
                fullScreenDialog.show();
            }
        }
    }

    /**
     * Initialize Exoplayer video
     *
     * @param video_url String url of video
     */
    private void initExoPlayer(String video_url) {
        boolean showControls = shouldControlsBeDisplayed(requireContext());

        if (!showControls) {
            playerView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
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
                }
            });
        } else { // show controls
            final ImageView volumeButton = playerView.findViewById(R.id.exo_volume_icon);

            // mute video clickListener
            volumeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (videoMuted) {
                        volumeButton.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.ic_baseline_volume_up_24));
                        if (player != null) {
                            player.setVolume(1f);
                        }
                        videoMuted = false;
                    } else {
                        volumeButton.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.ic_baseline_volume_off_24));
                        if (player != null) {
                            player.setVolume(0f);
                        }
                        videoMuted = true;
                    }
                }
            });
        }

        // new player instance
        player = ExoPlayerFactory.newSimpleInstance(requireActivity());
        player.addListener(new ExoPlayer.EventListener() {
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                if (playbackState == ExoPlayer.STATE_BUFFERING) {
                    progressBar.setVisibility(VISIBLE);
                } else {
                    progressBar.setVisibility(View.INVISIBLE);
                }
            }
        });
        playerView.setPlayer(player);

        // hide controller initially
        playerView.setControllerAutoShow(false);
        playerView.hideController();

        if (!showControls) {
            playerView.setControllerVisibilityListener(new PlayerControlView.VisibilityListener() {
                @Override
                public void onVisibilityChange(int i) {
                    if (i == 0) {
                        playerView.hideController();
                    }
                }
            });
        }

        MediaSource videoSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(video_url));
        player.prepare(videoSource);

        // loop video
        player.setRepeatMode(Player.REPEAT_MODE_ALL);

        // mute initially
        player.setVolume(0f);

        // play video when ready
        player.setPlayWhenReady(true);

        // rescale and resize video to screen
        player.addVideoListener(new VideoListener() {
            //  This is where we will resize view to fit aspect ratio of video
            @Override
            public void onVideoSizeChanged(
                    int width,
                    int height,
                    int unappliedRotationDegrees,
                    float pixelWidthHeightRatio) {

                if (width <= height) {
                    fullscreenIsPortrait = true;
                    try {
                        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    } catch (Exception e) {
                        Log.d("PostVideoFragment", Log.getStackTraceString(e));
                    }
                }

                //  Get layout params of view
                //  Use MyView.this to refer to the current MyView instance
                //  inside a callback
                ViewGroup.LayoutParams p = mainMediaFrameLayout.getLayoutParams();
                int currWidth = mainMediaFrameLayout.getWidth();

                //  Set new width/height of view
                //  height or width must be cast to float as int/int will give 0
                //  and distort view, e.g. 9/16 = 0 but 9.0/16 = 0.5625.
                //  p.height is int hence the final cast to int.
                p.width = currWidth;
                p.height = (int) ((float) height / width * currWidth);

                //  Redraw myView
                mainMediaFrameLayout.requestLayout();
            }
        });

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
     * Initialize Dialog with fullscreen video
     */
    private void initFullscreenDialog() {
        fullScreenDialog = new Dialog(requireActivity(), android.R.style.Theme_Black_NoTitleBar_Fullscreen) {
            public void onBackPressed() {
                if (exoPlayerFullscreen)
                    closeFullscreenDialog();
                super.onBackPressed();
            }
        };
    }

    /**
     * Opens the dialog with the fullscreen video
     */
    private void openFullscreenDialog() {
        try {
            if (fullscreenIsPortrait) {
                requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } else {
                requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        } catch (Exception e) {
            Log.d("PostVideoFragment", Log.getStackTraceString(e));
        }

        ((ViewGroup) playerView.getParent()).removeView(playerView);
        fullScreenDialog.addContentView(playerView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        fullScreenIcon.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.ic_baseline_fullscreen_exit_24));
        exoPlayerFullscreen = true;
        fullScreenDialog.show();
    }

    /**
     * Closes the dialog the fullscreen video
     */
    private void closeFullscreenDialog() {
        try {
            requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } catch (Exception e) {
            Log.d("PostVideoFragment", Log.getStackTraceString(e));
        }

        ((ViewGroup) playerView.getParent()).removeView(playerView);
        mainMediaFrameLayout.addView(playerView);
        exoPlayerFullscreen = false;
        fullScreenDialog.dismiss();
        fullScreenIcon.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.ic_baseline_fullscreen_24));
    }

    /**
     * Initialize custom button in exoplayer controls
     * to open or close fullscreen video
     */
    private void initFullscreenButton() {
        PlayerControlView controlView = playerView.findViewById(R.id.exo_controller);
        fullScreenIcon = controlView.findViewById(R.id.exo_fullscreen_icon);
        FrameLayout fullScreenButton = controlView.findViewById(R.id.exo_fullscreen_button);
        fullScreenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!exoPlayerFullscreen)
                    openFullscreenDialog();
                else
                    closeFullscreenDialog();
            }
        });
    }

    /**
     * Checks if video controller should be displayed
     *
     * @param context Context
     * @return true, if video controller should be displayed
     */
    private boolean shouldControlsBeDisplayed(Context context) {
        if (context != null) {
            // get the amount of columns from settings
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            if (preferences != null) {
                if (preferences.contains(getResources().getString(R.string.videoControls))) {
                    return preferences.getBoolean(getResources().getString(R.string.videoControls), false);
                }
            }
        }
        return false;
    }

    @Override
    public void onStop() {
        super.onStop();
        // mute and release player
        releasePlayer();
    }

    @Override
    public void onResume() {
        // reset screen orientation if video was fullscreen before and app was exited
        if (exoPlayerFullscreen)
            closeFullscreenDialog();

        // resume with video
        if (sVideoUrl != null) {
            loadVideoFromUrl(sVideoUrl);
        }

        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_RESUME_WINDOW, resumeWindow);
        outState.putLong(STATE_RESUME_POSITION, resumePosition);
        outState.putBoolean(STATE_PLAYER_FULLSCREEN, exoPlayerFullscreen);
        outState.putBoolean(STATE_VIDEO_MUTED, videoMuted);
        outState.putBoolean(STATE_FULLSCREEN_IS_PORTRAIT, fullscreenIsPortrait);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        super.onPause();
        // mute and release player;
        if (playerView != null && player != null) {
            resumeWindow = player.getCurrentWindowIndex();
            resumePosition = Math.max(0, player.getContentPosition());

            player.release();
        }

        // dismiss fullscreen dialog
        if (fullScreenDialog != null) {
            fullScreenDialog.dismiss();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // mute and release player;
        releasePlayer();
    }

    /**
     * Releases player and stops playing video
     */
    void releasePlayer() {
        if (player != null && player.isPlaying()) {
            player.setVolume(0f);
            player.stop();
            player.release();
            player = null;
        }
    }
}
