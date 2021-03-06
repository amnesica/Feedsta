package com.amnesica.feedsta.fragments;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.ViewPager;

import com.amnesica.feedsta.Account;
import com.amnesica.feedsta.Post;
import com.amnesica.feedsta.R;
import com.amnesica.feedsta.adapter.ListAdapterComment;
import com.amnesica.feedsta.adapter.StatePagerAdapterSideCar;
import com.amnesica.feedsta.helper.EditBookmarksType;
import com.amnesica.feedsta.helper.Error;
import com.amnesica.feedsta.helper.FeedObject;
import com.amnesica.feedsta.helper.FragmentHelper;
import com.amnesica.feedsta.helper.NetworkHandler;
import com.amnesica.feedsta.helper.StorageHelper;
import com.amnesica.feedsta.interfaces.FragmentCallback;
import com.amnesica.feedsta.models.Comment;
import com.amnesica.feedsta.models.URL;
import com.amnesica.feedsta.views.BtmSheetDialogAddCollection;
import com.amnesica.feedsta.views.BtmSheetDialogSelectCollection;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;

import javax.net.ssl.HttpsURLConnection;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static androidx.core.content.ContextCompat.getSystemService;
import static androidx.fragment.app.FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT;
import static com.amnesica.feedsta.helper.StaticIdentifier.permsRequestCode;
import static com.amnesica.feedsta.helper.StaticIdentifier.permsWriteOnly;
import static com.amnesica.feedsta.helper.StaticIdentifier.query_hash;

/**
 * Fragment for displaying a post
 */
@SuppressWarnings({"deprecation"})
public class PostFragment extends Fragment implements FragmentCallback {

    // view stuff
    private TextView textLikes;
    private TextView textOwnerIdOrUsername;
    private TextView textCaption;
    private TextView textUsernameAppBar;
    private TextView textDate;
    private ImageView imagePost;
    private ImageView imageProfilePicPostOwner;
    private ListView listComments;
    private ImageButton buttonLoadMoreComments;
    private ProgressBar progressBarComments;
    private View header;
    private ViewPager viewPager;
    private ProgressBar progressBarVideo;
    private ImageButton buttonBookmark;
    private StatePagerAdapterSideCar statePagerAdapterSideCar;
    private ProgressBar progressBar;
    private ImageButton imageButtonDownload;
    private ImageButton buttonCopyLink;
    private ProgressDialog progressDialogBatch;

    // booleans
    private Boolean scrolling;
    private Boolean bFirstFetch;
    private Boolean bFirstAdapterFetch;
    private Boolean bBookmarked = false;

    // exoplayer for videoView
    private SimpleExoPlayer player;
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

    static PostFragment newInstance(Post post) {
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

    @SuppressLint("InflateParams")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_post, container, false);

        // get post from arguments
        post = null;
        if (getArguments() != null) {
            if (getArguments().getSerializable("shortcode") != null) {
                String shortcode = (String) getArguments().getSerializable("shortcode");
                // initial post with only shortcode
                post = new Post("",
                        shortcode,
                        null,
                        false,
                        "",
                        false,
                        null);
                postIsFromDeepLink = true;
            } else {
                post = (Post) getArguments().getSerializable("post");
            }
        }

        // toolbar with back arrow and title
        Toolbar toolbar = v.findViewById(R.id.toolbar);
        toolbar.setTitle(getResources().getString(R.string.toolbar_title_post));
        FragmentHelper.setupToolbarWithBackButton(toolbar, new WeakReference<>(PostFragment.this));

        // set progressBar
        progressBar = v.findViewById(R.id.progressBarPost);
        progressBar.setVisibility(VISIBLE);

        // define header and get items from header
        header = getLayoutInflater().inflate(R.layout.listview_header_post, null);
        setUpHeaderView(header);

        // setup Footer
        View footer = getLayoutInflater().inflate(R.layout.listview_footer_post, null);
        setUpFooterView(footer);

        // find comments list
        listComments = v.findViewById(R.id.listViewComments);
        setUpListComments(listComments, footer);

        // initialize setting of bookmarkButton
        initializeBookmarkedButton();

        // check connection and set textFields and sidecar
        checkConnectionAndSetPostInfo();

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
     * Calls CheckConnectionAndGetPostInfo, checks internet connection and starts fetching
     */
    private void checkConnectionAndSetPostInfo() {
        new CheckConnectionAndGetPostInfo(PostFragment.this).execute();
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

    /**
     * Get more info about the post (image, video, sidecar) and setup download button
     */
    private void getPostInfoAndContents() {
        if (post != null) {
            if (post.getIs_sideCar()) {
                // get all sidecar element urls
                startGetMorePostInfoTask();
            }
            if (!post.getIs_sideCar() && !post.getIs_video()) {
                viewPager.setVisibility(GONE);
                imagePost.setVisibility(VISIBLE);

                // more image info
                startGetMorePostInfoTask();
            }
            if (!post.getIs_sideCar() && post.getIs_video()) {
                viewPager.setVisibility(GONE);
                imagePost.setVisibility(GONE);

                // set progress bar to visible
                progressBarVideo = header.findViewById(R.id.singleProgress_bar);
                progressBarVideo.setVisibility(VISIBLE);

                // get Video Url
                startGetMorePostInfoTask();
            }

            // set up download Button
            setUpImageButtonDownload();

            // initialize booleans for first fetching
            bFirstFetch = true;
            bFirstAdapterFetch = true;
            scrolling = false;
        }
    }

    /**
     * Sets up the download button and asks for storage permissions
     */
    private void setUpImageButtonDownload() {
        // button for download image or video
        imageButtonDownload = header.findViewById(R.id.buttonSaveImageOrVideo);
        imageButtonDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((post.getImageUrl() != null) || (post.getVideoUrl() != null)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(permsWriteOnly, permsRequestCode);
                    }
                }
            }
        });

        // setting downloadButton
        if (FragmentHelper.getThemeIsDarkTheme(getContext())) {
            imageButtonDownload.setBackgroundResource(R.drawable.ic_file_download_white_outline_24dp);
        } else {
            imageButtonDownload.setBackgroundResource(R.drawable.ic_file_download_black_outline_24dp);
        }
    }

    /**
     * Disables all buttons under the post
     */
    private void disableButtons() {
        if (buttonBookmark != null && buttonCopyLink != null && imageButtonDownload != null) {
            buttonBookmark.setEnabled(false);
            buttonCopyLink.setEnabled(false);
            imageButtonDownload.setEnabled(false);
        }
    }

    /**
     * Hides the progressBar
     */
    private void hideProgressBar() {
        if (this.getActivity() != null) {
            requireActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressBar.setVisibility(GONE);
                }
            });
        }
    }

    /**
     * Starts async task GetMoreInfoPost
     */
    private void startGetMorePostInfoTask() {
        try {
            GetMorePostInfo getMorePostInfo = new GetMorePostInfo(PostFragment.this);
            getMorePostInfo.execute();
        } catch (Exception e) {
            Log.d("PostFragment", Log.getStackTraceString(e));
        }
    }

    /**
     * Initializes bookmarked button. Button is shown completely black if bookmark already exits
     */
    private void initializeBookmarkedButton() {
        // setting bookmarkButton
        bBookmarked = StorageHelper.checkIfAccountOrPostIsInFile(post, StorageHelper.filename_bookmarks, getContext());

        // set button background resource for bookmarkButton
        if (bBookmarked != null && bBookmarked) { // saved
            if (FragmentHelper.getThemeIsDarkTheme(getContext())) {
                buttonBookmark.setBackgroundResource(R.drawable.ic_bookmark_white_24dp);
            } else {
                buttonBookmark.setBackgroundResource(R.drawable.ic_bookmark_black_24dp);
            }
        } else { // not saved yet
            if (FragmentHelper.getThemeIsDarkTheme(getContext())) {
                buttonBookmark.setBackgroundResource(R.drawable.ic_bookmark_border_white_24dp);
            } else {
                buttonBookmark.setBackgroundResource(R.drawable.ic_bookmark_border_black_24dp);
            }
        }
    }

    /**
     * Sets up listComments with the footer
     *
     * @param listComments listComments
     * @param footer       footer view
     */
    private void setUpListComments(ListView listComments, View footer) {
        // add footer view to comments list
        listComments.addFooterView(footer);

        // click on comment to go to commenter account
        listComments.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                if (comments != null && comments.get(position - 1) != null) {
                    // get comment from list
                    Comment commentToGoTo = comments.get(position - 1);

                    if (commentToGoTo != null) {
                        // create account
                        Account account = new Account(
                                commentToGoTo.getOwnerProfilePicUrl(),
                                commentToGoTo.getUsername(),
                                "",
                                false,
                                commentToGoTo.getId());
                        // go to profile
                        goToProfileFragment(account);
                    }
                }
            }
        });
    }

    /**
     * Sets up the footer View with button 'load more comments'
     *
     * @param footer footer view
     */
    private void setUpFooterView(View footer) {
        // find button and set load more comments background resource
        buttonLoadMoreComments = footer.findViewById(R.id.buttonLoadMoreComments);

        if (FragmentHelper.getThemeIsDarkTheme(getContext())) {
            buttonLoadMoreComments.setBackgroundResource(R.drawable.ic_add_circle_outline_white_48dp);
        } else {
            buttonLoadMoreComments.setBackgroundResource(R.drawable.ic_add_circle_outline_black_48dp);
        }

        progressBarComments = footer.findViewById(R.id.progressbarComments);
    }

    /**
     * Find views for header elements and sets button bookmark and button CopyLink
     *
     * @param header header view
     */
    private void setUpHeaderView(View header) {
        // find header views
        textLikes = header.findViewById(R.id.likes);
        textOwnerIdOrUsername = header.findViewById(R.id.ownerIdOrUsername);
        textCaption = header.findViewById(R.id.caption);
        textUsernameAppBar = header.findViewById(R.id.textUsernameInBarUnderAppBar);
        textDate = header.findViewById(R.id.date);
        imageProfilePicPostOwner = header.findViewById(R.id.imageProfilePicPostOwner);
        imagePost = header.findViewById(R.id.singleImagePost);
        buttonBookmark = header.findViewById(R.id.buttonBookmark);
        viewPager = header.findViewById(R.id.viewpagerPost);

        // on click listener to show post image fullscreen to zoom into image
        imagePost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // start fullscreen image post fragment
                FullscreenImagePostFragment fullscreenImagePostFragment = FullscreenImagePostFragment.newInstance(post.getImageUrl());

                // add fullscreenImagePostFragment to FragmentManager
                FragmentHelper.addFragmentToContainer(fullscreenImagePostFragment, requireActivity().getSupportFragmentManager());
            }
        });

        // set button bookmark
        buttonBookmark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bookmarkPost();
            }
        });

        // set viewpager for sidecar and normal images and video
        viewPager.setOffscreenPageLimit(1);

        // clickListener go to users profile on tap on appBar
        RelativeLayout relBarUnderAppBar = header.findViewById(R.id.BarUnderAppBar);
        relBarUnderAppBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Account accountToGoTo = new Account(
                        post.getImageUrlProfilePicOwner(),
                        post.getUsername(),
                        "",
                        false,
                        post.getOwnerId());

                goToProfileFragment(accountToGoTo);
            }
        });

        // set button copyLink
        buttonCopyLink = header.findViewById(R.id.buttonCopyLink);

        if (FragmentHelper.getThemeIsDarkTheme(getContext())) {
            buttonCopyLink.setBackgroundResource(R.drawable.ic_content_copy_white_24dp);
        } else {
            buttonCopyLink.setBackgroundResource(R.drawable.ic_content_copy_black_24dp);
        }

        buttonCopyLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String urlToCopy;
                // get url string from post
                if (post != null) {

                    urlToCopy = createUrlForCopyPost();

                    // copy urlToCopy to clipboard
                    if (getContext() != null) {
                        ClipboardManager clipboard = getSystemService(requireContext(), ClipboardManager.class);
                        ClipData clip = ClipData.newPlainText("urlToCopy", urlToCopy);

                        if (clipboard != null) {
                            clipboard.setPrimaryClip(clip);
                            // make toast that link has been copied
                            FragmentHelper.showToast(getResources().getString(R.string.link_copied), requireActivity(), requireContext());
                        }

                        // change icon to copied
                        if (FragmentHelper.getThemeIsDarkTheme(getContext())) {
                            buttonCopyLink.setBackgroundResource(R.drawable.ic_content_copy_white_pressed_24dp);
                        } else {
                            buttonCopyLink.setBackgroundResource(R.drawable.ic_content_copy_black_pressed_24dp);
                        }
                    }
                }
            }
        });
    }

    /**
     * Creates a specific string to copy to clipboard.
     * Adds advertising string or returns just the url
     * @return String
     */
    private String createUrlForCopyPost() {
        if (post.getShortcode() != null) {
            if (FragmentHelper.addAdvertisingStringToClipboard(PostFragment.this)) {
                return "https://www.instagram.com/p/" + post.getShortcode() + getResources().getString(R.string.copy_post_second_part);
            } else {
                return "https://www.instagram.com/p/" + post.getShortcode();
            }
        }
        return "";
    }

    /**
     * Go to profileFragment with click on postToSend
     *
     * @param accountToGoTo account to go to
     */
    private void goToProfileFragment(Account accountToGoTo) {
        // new profileFragment
        ProfileFragment profileFragment = ProfileFragment.newInstance(accountToGoTo);

        // release and stop video player if exits
        releasePlayer();

        // add fragment to container
        FragmentHelper.addFragmentToContainer(profileFragment, requireActivity().getSupportFragmentManager());
    }

    /**
     * Gets comments to post (hint: method before workaround with only 24 comments!)
     */
    private void getCommentsToPost() throws NullPointerException {
        // load more comments
        if (post.getComments() > 24) {
            // button to load more comments
            buttonLoadMoreComments.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    buttonLoadMoreComments.setVisibility(GONE);
                    progressBarComments.setVisibility(VISIBLE);

                    // start fetching comments
                    startGetPostComments();
                }
            });
        } else {
            // set button invisible if not enough comments
            buttonLoadMoreComments.setVisibility(GONE);
            progressBarComments.setVisibility(View.GONE);
        }

        if (post.getComments() != 0) {
            // start fetching comments - no need for hiding the button "load more comments"
            startGetPostComments();
        } else {
            // no comments on post
            try {
                ListAdapterComment adapter = new ListAdapterComment(
                        getContext(), R.layout.list_item_comment, new ArrayList<Comment>());
                listComments.setAdapter(adapter);
            } catch (NullPointerException e) {
                Log.d("PostFragment", Log.getStackTraceString(e));
            }
        }
    }

    /**
     * Starts async task GetPostComments
     */
    private void startGetPostComments() {
        try {
            GetPostComments getPostComments = new GetPostComments(PostFragment.this);
            getPostComments.execute();
        } catch (Exception e) {
            Log.d("PostFragment", Log.getStackTraceString(e));
        }
    }

    /**
     * Saves a post in internal storage in its proper representation (StorageRep)
     */
    private void bookmarkPost() {
        if (getContext() != null) {
            if (!StorageHelper.checkIfAccountOrPostIsInFile(post, StorageHelper.filename_bookmarks, getContext())) {
                try {
                    bBookmarked = StorageHelper.storePostInInternalStorage(post, requireContext(), StorageHelper.filename_bookmarks);
                } catch (IOException e) {
                    Log.d("PostFragment", Log.getStackTraceString(e));
                }
            } else {
                // post is already bookmarked and needs to be deleted
                bBookmarked = StorageHelper.removePostFromInternalStorage(post, getContext(), StorageHelper.filename_bookmarks);

                // result is true when removed successfully
                if (bBookmarked) {
                    bBookmarked = false;
                }
            }

            // set button background resource
            if (bBookmarked) {
                // saved
                if (FragmentHelper.getThemeIsDarkTheme(getContext())) {
                    buttonBookmark.setBackgroundResource(R.drawable.ic_bookmark_white_24dp);
                } else {
                    buttonBookmark.setBackgroundResource(R.drawable.ic_bookmark_black_24dp);
                }

                // show snackBar with option to save bookmark in collection
                showSnackBarToSaveInCollection();

            } else {
                // not saved yet
                if (FragmentHelper.getThemeIsDarkTheme(getContext())) {
                    buttonBookmark.setBackgroundResource(R.drawable.ic_bookmark_border_white_24dp);
                } else {
                    buttonBookmark.setBackgroundResource(R.drawable.ic_bookmark_border_black_24dp);
                }

                // show snackBar that bookmark was removed
                showCustomSnackBarWithSpecificText(getResources().getString(R.string.post_removed_from_bookmarks));
            }
        }
    }

    /**
     * Shows a custom snackBar to show option to save post to collection
     */
    private void showSnackBarToSaveInCollection() {
        if (header != null) {
            final RelativeLayout relativeLayoutCustomSnackbar = header.findViewById(R.id.relLayCustomSnackBar);
            final TextView textViewLabel = header.findViewById(R.id.textViewLabel);
            final TextView textViewAction = header.findViewById(R.id.textViewAction);

            final Runnable[] runnable = new Runnable[1];
            final Handler[] handler = new Handler[1];

            requireActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    handler[0] = new Handler();
                }
            });

            requireActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    runnable[0] = new Runnable() {
                        @Override
                        public void run() {
                            relativeLayoutCustomSnackbar.setVisibility(GONE);
                        }
                    };
                }
            });

            // set text color based on theme
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = requireContext().getTheme();
            theme.resolveAttribute(R.attr.colorAccent, typedValue, true);
            @ColorInt final int textColor = typedValue.data;

            requireActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textViewLabel.setTextColor(textColor);
                    textViewAction.setTextColor(textColor);
                }
            });

            // set background color based on theme
            theme.resolveAttribute(R.attr.colorPrimary, typedValue, true);
            @ColorInt final int backgroundColor = typedValue.data;

            requireActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    relativeLayoutCustomSnackbar.setBackgroundColor(backgroundColor);
                }
            });

            // set Texts
            requireActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textViewLabel.setText(R.string.snackbar_bookmark_saved);
                    textViewAction.setText(R.string.snackbar_action_save_to_collection);
                }
            });

            // set OnClickListener when clicked on action
            textViewAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            handler[0].removeCallbacks(runnable[0]);
                            relativeLayoutCustomSnackbar.setVisibility(GONE);
                        }
                    });
                }
            });

            // set action but check first if there are collections
            if (FragmentHelper.collectionsAlreadyExist(requireContext())) {
                textViewAction.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        requireActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                handler[0].removeCallbacks(runnable[0]);
                                relativeLayoutCustomSnackbar.setVisibility(GONE);
                            }
                        });

                        if (StorageHelper.checkIfAccountOrPostIsInFile(post, StorageHelper.filename_bookmarks, getContext())) {
                            // open dialog to select existing collection for bookmark
                            BtmSheetDialogSelectCollection btmSheetDialogSelectCollection = new BtmSheetDialogSelectCollection(EditBookmarksType.SAVE_BOOKMARKS);

                            // set listener to save posts after selection
                            btmSheetDialogSelectCollection.setOnFragmentCallbackListener(PostFragment.this);

                            // show dialog
                            btmSheetDialogSelectCollection.show(requireActivity().getSupportFragmentManager(), BtmSheetDialogSelectCollection.class.getSimpleName());

                        } else {
                            FragmentHelper.showToast(getString(R.string.post_not_bookmarked_yet), requireActivity(), requireContext());
                        }
                    }
                });
            } else {
                textViewAction.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        requireActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                handler[0].removeCallbacks(runnable[0]);
                                relativeLayoutCustomSnackbar.setVisibility(GONE);
                            }
                        });

                        if (StorageHelper.checkIfAccountOrPostIsInFile(post, StorageHelper.filename_bookmarks, getContext())) {
                            // open dialog to add bookmark to collection
                            BtmSheetDialogAddCollection bottomSheetAddCollection = new BtmSheetDialogAddCollection(null, EditBookmarksType.SAVE_BOOKMARKS);

                            // set listener to sheet
                            bottomSheetAddCollection.setOnFragmentCallbackListener(PostFragment.this);

                            // show bottom sheet
                            bottomSheetAddCollection.show(requireActivity().getSupportFragmentManager(), BtmSheetDialogAddCollection.class.getSimpleName());
                        } else {
                            FragmentHelper.showToast(getString(R.string.post_not_bookmarked_yet), requireActivity(), requireContext());
                        }
                    }
                });
            }

            // make custom snackbar visible and trigger postDelay
            requireActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    relativeLayoutCustomSnackbar.setVisibility(VISIBLE);
                    handler[0].postDelayed(runnable[0], 4000);
                }
            });
        }
    }

    /**
     * Shows a custom snackbar on the bottom of the post
     * @param specificText String to display
     */
    @SuppressLint("SetTextI18n")
    public void showCustomSnackBarWithSpecificText(final String specificText) {
        if (header != null) {
            final RelativeLayout relativeLayoutCustomSnackbar = header.findViewById(R.id.relLayCustomSnackBar);
            final TextView textViewLabel = header.findViewById(R.id.textViewLabel);
            final TextView textViewAction = header.findViewById(R.id.textViewAction);

            final Runnable[] runnable = new Runnable[1];
            final Handler[] handler = new Handler[1];

            requireActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    handler[0] = new Handler();
                }
            });

            requireActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    runnable[0] = new Runnable() {
                        @Override
                        public void run() {
                            relativeLayoutCustomSnackbar.setVisibility(GONE);
                        }
                    };
                }
            });

            // set text color based on theme
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = requireContext().getTheme();
            theme.resolveAttribute(R.attr.colorAccent, typedValue, true);
            @ColorInt final int textColor = typedValue.data;

            requireActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textViewLabel.setTextColor(textColor);
                    textViewAction.setTextColor(textColor);
                }
            });

            // set background color based on theme
            theme.resolveAttribute(R.attr.colorPrimary, typedValue, true);
            @ColorInt final int backgroundColor = typedValue.data;

            requireActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    relativeLayoutCustomSnackbar.setBackgroundColor(backgroundColor);
                }
            });

            // set Texts
            requireActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textViewLabel.setText(specificText);
                    textViewAction.setText(R.string.okay);
                }
            });

            // set OnClickListener when clicked on action
            textViewAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            handler[0].removeCallbacks(runnable[0]);
                            relativeLayoutCustomSnackbar.setVisibility(GONE);
                        }
                    });
                }
            });

            requireActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    relativeLayoutCustomSnackbar.setVisibility(VISIBLE);
                    handler[0].postDelayed(runnable[0], 4000);
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (permsRequestCode == 200) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted. Continue the action or workflow
                // in your app
                if ((!post.getIs_video() && post.getSidecarUrls() == null) ||
                        (post.getSidecarUrls() != null && Objects.requireNonNull(post.getSidecarUrls().get(viewPager.getCurrentItem())).get(0).equals("image"))) {

                    // start save image from url
                    SaveImageFromUrl saveImageFromUrl = new SaveImageFromUrl(PostFragment.this, viewPager.getCurrentItem());
                    saveImageFromUrl.execute();

                } else if ((post.getIs_video() && post.getSidecarUrls() == null) ||
                        (post.getSidecarUrls() != null && Objects.requireNonNull(post.getSidecarUrls().get(viewPager.getCurrentItem())).get(0).equals("video"))) {

                    // reload video when it is a normal video, not sidecar (only Workaround)
                    if (post.getIs_video() && post.getSidecarUrls() == null) {
                        loadVideoFromUrl(post.getVideoUrl());
                    }

                    // start save video from url
                    SaveVideoFromUrl saveVideoFromUrl = new SaveVideoFromUrl(PostFragment.this, viewPager.getCurrentItem());
                    saveVideoFromUrl.execute();
                }
            } else {
                // Explain to the user that the feature is unavailable because
                // the features requires a permission that the user has denied.
                // At the same time, respect the user's decision. Don't link to
                // system settings in an effort to convince the user to change
                // their decision.
                FragmentHelper.showToast(getResources().getString(R.string.permission_denied), requireActivity(), requireContext());
            }
        }
    }

    @Override
    public void onResume() {
        // reset screen orientation if video was fullscreen before and app was exited
        if (exoPlayerFullscreen)
            closeFullscreenDialog();

        // resume with video
        if (header != null && header.findViewById(R.id.main_media_frame).getVisibility() == VISIBLE &&
                post != null && post.getVideoUrl() != null) {
            loadVideoFromUrl(post.getVideoUrl());
        }
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

        stopTasks();
    }

    @Override
    public void onStop() {
        super.onStop();
        stopTasks();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopTasks();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (hidden) {
            stopTasks();

        } else {
            // load video from url for normal video post
            if (post != null && post.getIs_video() && !post.getIs_sideCar()) {
                if (post.getVideoUrl() != null) {
                    loadVideoFromUrl(post.getVideoUrl());
                }
            }

            // if post is sidecar, reload whole adapter (Workaround)
            if (post != null && post.getIs_sideCar()) {
                if (viewPager != null && statePagerAdapterSideCar != null) {
                    //reload adapter
                    viewPager.setAdapter(statePagerAdapterSideCar);
                }
            }

            // reload bookmark info
            Boolean postAlreadyBookmarked = StorageHelper.checkIfAccountOrPostIsInFile(post, StorageHelper.filename_bookmarks, getContext());
            if (postAlreadyBookmarked) {
                // set button background resource
                if (FragmentHelper.getThemeIsDarkTheme(getContext())) {
                    buttonBookmark.setBackgroundResource(R.drawable.ic_bookmark_white_24dp);
                } else {
                    buttonBookmark.setBackgroundResource(R.drawable.ic_bookmark_black_24dp);
                }
            } else {
                // not saved yet
                if (FragmentHelper.getThemeIsDarkTheme(getContext())) {
                    buttonBookmark.setBackgroundResource(R.drawable.ic_bookmark_border_white_24dp);
                } else {
                    buttonBookmark.setBackgroundResource(R.drawable.ic_bookmark_border_black_24dp);
                }
                // reset category on post
                post.setCategory(null);
            }
        }
        super.onHiddenChanged(hidden);
    }

    /**
     * Releases and stops the video player
     */
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

    /**
     * Releases the video player and all players in registered fragments
     */
    private void stopTasks() {
        // release video player
        releasePlayer();

        // finish all video sidecar views with release of player
        if (statePagerAdapterSideCar != null) {
            statePagerAdapterSideCar.finishAllRegisteredFragments();
        }
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
                playerView = header.findViewById(R.id.exoplayer);
                playerView.setVisibility(VISIBLE);

                // make frame visible
                mainMediaFrameLayout = header.findViewById(R.id.main_media_frame);
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

                        //mute video
                        player.setVolume(0f);
                        videoMuted = true;
                    } else {

                        //unmute video
                        if (player != null) {
                            player.setVolume(1f);
                            videoMuted = false;
                        }
                    }
                }
            });
        } else { //show controls
            final ImageView volumeButton = playerView.findViewById(R.id.exo_volume_icon);
            // mute video clickListener
            volumeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (videoMuted) {
                        volumeButton.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.ic_baseline_volume_off_24));
                        if (player != null) {
                            player.setVolume(1f);
                        }
                        videoMuted = false;
                    } else {
                        volumeButton.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.ic_baseline_volume_up_24));
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
                    progressBarVideo.setVisibility(VISIBLE);
                } else {
                    progressBarVideo.setVisibility(View.INVISIBLE);
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
            // This is where we will resize view to fit aspect ratio of video
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
                        Log.d("PostFragment", Log.getStackTraceString(e));
                    }
                }

                // Get layout params of view
                // Use MyView.this to refer to the current MyView instance
                // inside a callback
                ViewGroup.LayoutParams p = mainMediaFrameLayout.getLayoutParams();
                int currWidth = mainMediaFrameLayout.getWidth();

                // Set new width/height of view
                // height or width must be cast to float as int/int will give 0
                // and distort view, e.g. 9/16 = 0 but 9.0/16 = 0.5625.
                // p.height is int hence the final cast to int.
                p.width = currWidth;
                p.height = (int) ((float) height / width * currWidth);

                // Redraw myView
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
            Log.d("PostFragment", Log.getStackTraceString(e));
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
            ((ViewGroup) playerView.getParent()).removeView(playerView);
            mainMediaFrameLayout.addView(playerView);
            exoPlayerFullscreen = false;
            fullScreenDialog.dismiss();
            fullScreenIcon.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.ic_baseline_fullscreen_24));
        } catch (Exception e) {
            Log.d("PostFragment", Log.getStackTraceString(e));
        }
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

    /**
     * Sets a new category (collection) to a post
     * @param category String
     * @param editMode EditBookmarksType
     */
    @Override
    public void savePostOrListToCollection(String category, EditBookmarksType editMode) {
        // hint: no async task here -> no long running operation
        if (post != null && category != null) {
            // set new category to single post
            boolean successful = FragmentHelper.setNewCategoryToPost(category, post,
                    requireContext(), PostFragment.this);

            // display "saved successfully" snackbar
            if (successful && post != null && post.getCategory() != null) {
                showCustomSnackBarWithSpecificText(getString(R.string.saved_in_collection_successful) +
                        post.getCategory());
            }
        }
    }

    @Override
    public void openBtmSheetDialogAddCollection(EditBookmarksType editMode) {
        // opens BottomSheetDialogAddCollection
        BtmSheetDialogAddCollection bottomSheetAddCollection = new BtmSheetDialogAddCollection(null, editMode);

        // set FragmentCallbackListener
        bottomSheetAddCollection.setOnFragmentCallbackListener(PostFragment.this);

        // show Fragment
        bottomSheetAddCollection.show(requireActivity().getSupportFragmentManager(), BtmSheetDialogAddCollection.class.getSimpleName());
    }

    /**
     * Saves a video from a url
     */
    @SuppressWarnings("CanBeFinal")
    private static class SaveVideoFromUrl extends AsyncTask<Void, Void, Void> {

        private final WeakReference<PostFragment> fragmentReference;
        private final int position;

        // constructor
        SaveVideoFromUrl(PostFragment context, int position) {
            fragmentReference = new WeakReference<>(context);
            this.position = position;
        }

        @Override
        protected void onPreExecute() {
            showProgressDialog();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if (!isCancelled()) {
                // get reference from fragment
                final PostFragment fragment = fragmentReference.get();
                if (fragment != null) {
                    try {
                        java.net.URL url;
                        if (fragment.post.getIs_sideCar() && Objects.requireNonNull(fragment.post.getSidecarUrls().get(position)).get(0).equals("video")) {
                            String videoUrl = Objects.requireNonNull(fragment.post.getSidecarUrls().get(position)).get(1);
                            url = new java.net.URL(videoUrl);
                        } else {
                            url = new java.net.URL(fragment.post.getVideoUrl());
                        }

                        // open a connection to that URL.
                        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
                        int response = con.getResponseCode();

                        InputStream inputStream = con.getInputStream();
                        byte[] buffer = new byte[7 * 1024];

                        Boolean saved = StorageHelper.saveVideo(inputStream, buffer, fragment.post.getUsername(), fragment.requireContext());

                        if (saved) {
                            if (fragment.getActivity() != null) {
                                try {
                                    fragment.requireActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (FragmentHelper.getThemeIsDarkTheme(fragment.requireContext())) {
                                                fragment.imageButtonDownload.setBackgroundResource(R.drawable.ic_file_download_white_24dp);
                                            } else {
                                                fragment.imageButtonDownload.setBackgroundResource(R.drawable.ic_file_download_black_24dp);
                                            }

                                            FragmentHelper.showToast(fragment.getResources().getString(R.string.video_saved), fragment.requireActivity(), fragment.requireContext());

                                            fragment.progressDialogBatch.dismiss();
                                        }
                                    });
                                } catch (IllegalStateException e) {
                                    throw new Exception();
                                }
                            }
                        } else {
                            throw new Exception();
                        }
                    } catch (Exception e) {
                        Log.d("PostFragment", Log.getStackTraceString(e));
                        if (fragment.getActivity() != null) {
                            try {
                                FragmentHelper.showToast(fragment.getResources().getString(R.string.video_saved_failed), fragment.requireActivity(), fragment.requireContext());
                            } catch (Exception e2) {
                                Log.d("PostFragment", Log.getStackTraceString(e));
                            }
                        }
                    }
                }
            }
            return null;
        }

        private void showProgressDialog() {
            if (!isCancelled()) {
                // get reference from fragment
                final PostFragment fragment = fragmentReference.get();

                if (fragment != null) {
                    fragment.progressDialogBatch = new ProgressDialog(fragment.requireContext());
                    fragment.progressDialogBatch.setTitle(fragment.requireContext().getString(R.string.progress_dialog_title_download_video));
                    fragment.progressDialogBatch.setMessage(fragment.requireContext().getString(R.string.progress_dialog_message_download_selected_posts));
                    fragment.progressDialogBatch.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    fragment.progressDialogBatch.setProgress(0);
                    fragment.progressDialogBatch.show();
                }
            }
        }
    }

    /**
     * Saves an image from a url
     */
    @SuppressWarnings("CanBeFinal")
    private static class SaveImageFromUrl extends AsyncTask<Void, Void, Void> {

        private final WeakReference<PostFragment> fragmentReference;
        private final int position;

        // constructor
        SaveImageFromUrl(PostFragment context, int position) {
            fragmentReference = new WeakReference<>(context);
            this.position = position;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if (!isCancelled()) {
                // get reference from fragment
                final PostFragment fragment = fragmentReference.get();
                if (fragment != null) {
                    try {
                        java.net.URL url;

                        if (fragment.post.getIs_sideCar() && Objects.requireNonNull(fragment.post.getSidecarUrls().get(position)).get(0).equals("image")) { //post is sidecar and image
                            String imageUrl = Objects.requireNonNull(fragment.post.getSidecarUrls().get(position)).get(1);
                            url = new java.net.URL(imageUrl);
                        } else { // post is image
                            url = new java.net.URL(fragment.post.getImageUrl());
                        }
                        HttpURLConnection connection = (HttpURLConnection) url
                                .openConnection();
                        connection.setDoInput(true);
                        connection.connect();
                        InputStream input = connection.getInputStream();
                        Bitmap myBitmap = BitmapFactory.decodeStream(input);

                        // save to storage
                        boolean saved = StorageHelper.saveImage(myBitmap, fragment.post.getUsername(), fragment.getContext());

                        input.close();

                        if (saved) {
                            try {
                                // change icon and display toast
                                fragment.requireActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            if (FragmentHelper.getThemeIsDarkTheme(fragment.requireContext())) {
                                                fragment.imageButtonDownload.setBackgroundResource(R.drawable.ic_file_download_white_24dp);
                                            } else {
                                                fragment.imageButtonDownload.setBackgroundResource(R.drawable.ic_file_download_black_24dp);
                                            }
                                        } catch (Exception e) {
                                            Log.d("PostFragment", Log.getStackTraceString(e));
                                        }
                                    }
                                });
                                FragmentHelper.showToast(fragment.getResources().getString(R.string.image_saved), fragment.requireActivity(), fragment.requireContext());
                            } catch (Exception e) {
                                throw new Exception();
                            }
                        } else {
                            throw new Exception();
                        }
                    } catch (Exception e) {
                        Log.d("PostFragment", Log.getStackTraceString(e));
                        if (fragment.getActivity() != null) {
                            FragmentHelper.showToast(fragment.getResources().getString(R.string.image_saved_failed), fragment.requireActivity(), fragment.requireContext());
                        }
                    }
                }
            }
            return null;
        }
    }

    /**
     * Checks internet connection and notifies user if there is no connection. Starts fetching at the end
     */
    @SuppressWarnings("CanBeFinal")
    private static class CheckConnectionAndGetPostInfo extends AsyncTask<Void, Void, Void> {

        private final WeakReference<PostFragment> fragmentReference;
        boolean isInternetAvailable = false;

        CheckConnectionAndGetPostInfo(PostFragment context) {
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
                final PostFragment fragment = fragmentReference.get();
                if (fragment != null) {
                    if (isInternetAvailable) {
                        // set textFields and sidecar
                        fragment.getPostInfoAndContents();
                    } else {
                        FragmentHelper.notifyUserOfProblem(fragment, Error.NO_INTERNET_CONNECTION);
                        try {
                            fragment.progressBar.setVisibility(GONE);
                        } catch (Exception e) {
                            Log.d("PostFragment", Log.getStackTraceString(e));
                        }

                        if (fragment != null) {
                            fragment.disableButtons();
                        }
                    }
                }
            }
        }
    }

    /**
     * Async tasks gets more info about post
     */
    @SuppressWarnings("CanBeFinal")
    private static class GetMorePostInfo extends AsyncTask<Void, Void, Void> {

        private final WeakReference<PostFragment> fragmentReference;
        private NetworkHandler sh;

        GetMorePostInfo(PostFragment context) {
            fragmentReference = new WeakReference<>(context);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (!isCancelled()) {
                // get reference from fragment
                final PostFragment fragment = fragmentReference.get();
                if (fragment != null) {
                    // make progressBar visible
                    try {
                        fragment.progressBar.setVisibility(View.VISIBLE);
                    } catch (Exception e) {
                        Log.d("PostFragment", Log.getStackTraceString(e));
                    }
                }
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if (!isCancelled()) {
                sh = new NetworkHandler();
                URL url;
                url = makeValidURLForVideo();
                GetMoreInfoFromPost(url);
            }
            return null;
        }

        /**
         * Makes valid URLs for posts
         *
         * @return valid url
         */
        private URL makeValidURLForVideo() {
            if (!isCancelled()) {
                // get reference from fragment
                final PostFragment fragment = fragmentReference.get();
                if (fragment != null) {
                    String urlAddress = "https://www.instagram.com/p/" + fragment.post.getShortcode() + "/?__a=1";
                    return fragment.url = new URL(urlAddress, fragment.post.getShortcode(), FeedObject.ACCOUNT);
                }
            }
            return null;
        }

        /**
         * Gets more info from post
         *
         * @param url url
         */
        private void GetMoreInfoFromPost(URL url) {
            if (!isCancelled()) {
                // get reference from fragment
                final PostFragment fragment = fragmentReference.get();
                if (fragment != null) {
                    String newUrl = url.url;

                    // get json string from url
                    String jsonStr = sh.makeServiceCall(newUrl, this.getClass().getSimpleName());

                    if (jsonStr != null) {
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
                                fragment.post.setTakenAtDate(new Date(Long.parseLong(shortcode_media.getString("taken_at_timestamp")) * 1000));
                                fragment.post.setIs_video(shortcode_media.getBoolean("is_video"));
                                fragment.post.setImageUrlThumbnail(shortcode_media.getJSONArray("display_resources").getJSONObject(0).getString("src"));
                                fragment.post.setIs_sideCar(shortcode_media.getString("__typename").equals("GraphSidecar"));
                            }

                            // get likes, username, ownerId, comments (int), caption and imageUrl (full size)
                            fragment.post.setLikes(shortcode_media.getJSONObject("edge_media_preview_like").getInt("count"));
                            fragment.post.setUsername(shortcode_media.getJSONObject("owner").getString("username"));
                            fragment.post.setOwnerId(shortcode_media.getJSONObject("owner").getString("id"));
                            fragment.post.setComments(shortcode_media.getJSONObject("edge_media_preview_comment").getInt("count"));

                            // get height of image for single image post
                            fragment.post.setHeight(shortcode_media.getJSONArray("display_resources").getJSONObject(2).getInt("config_height"));

                            // get caption of post
                            JSONArray edgesCaption = shortcode_media.getJSONObject("edge_media_to_caption").getJSONArray("edges");
                            String caption = null;
                            if (edgesCaption.length() != 0) {
                                JSONObject captionNode = edgesCaption.getJSONObject(0);
                                JSONObject captionNodeString = captionNode.getJSONObject("node");
                                caption = captionNodeString.getString("text");
                            }
                            fragment.post.setCaption(caption);
                            fragment.post.setImageUrl(shortcode_media.getString("display_url"));
                            fragment.post.setImageUrlProfilePicOwner(shortcode_media.getJSONObject("owner").getString("profile_pic_url"));

                            // hint: active workaround: only comments under shortcode-url can be fetched -> no more than 24 comments can be fetched at the moment
                            JSONObject edge_media_to_parent_comment = shortcode_media.getJSONObject("edge_media_to_parent_comment");

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

                            // no comments or comments disabled - but initialize comments to get a view
                            if (edgesComments.length() == 0) {
                                if (fragment.comments == null) {
                                    fragment.comments = new ArrayList<>();
                                }
                            } else {
                                // fetch edge data for comments
                                fetchEdgeDataComments(edgesComments, startIndex, endIndex);
                            }

                            // get sidecar urls if post is sidecar
                            if (fragment.post.getIs_sideCar()) {
                                JSONObject edge_sidecar_to_children = shortcode_media.getJSONObject("edge_sidecar_to_children");
                                JSONArray edges = edge_sidecar_to_children.getJSONArray("edges");

                                // storing urls with "index" - "video or image" - "url"
                                HashMap<Integer, ArrayList<String>> sidecarUrls = new HashMap<>();
                                ArrayList<String> listInsideMap;

                                // get sidecar Urls
                                for (int i = 0; i < edges.length(); i++) {

                                    // get edge from edges
                                    JSONObject edge = edges.getJSONObject(i);

                                    // get node of selected edge
                                    JSONObject node = edge.getJSONObject("node");

                                    if (node.getBoolean("is_video")) {
                                        // node is video -> get video_url
                                        String video_url = node.getString("video_url");

                                        listInsideMap = new ArrayList<>();

                                        // add info that it is a video
                                        listInsideMap.add("video");

                                        // add url of video
                                        listInsideMap.add(video_url);

                                        // put listInside in sidecarUrls hashMap with index i
                                        sidecarUrls.put(i, listInsideMap);

                                    } else { // node is image -> get image_url

                                        String image_url = node.getString("display_url");

                                        listInsideMap = new ArrayList<>();

                                        // add info that it is an image
                                        listInsideMap.add("image");

                                        // add url of image
                                        listInsideMap.add(image_url);

                                        // put listInside in sidecarUrls hashMap with index i
                                        sidecarUrls.put(i, listInsideMap);
                                    }
                                }
                                // set hashMap of post
                                fragment.post.setSidecarUrls(sidecarUrls);
                            } else if (fragment.post.getIs_video()) {
                                // get video url of just a normal video - no sidecar
                                String video_url = shortcode_media.getString("video_url");
                                fragment.post.setVideoUrl(video_url);
                            }
                        } catch (JSONException | IllegalStateException e) {
                            Log.d("PostFragment", Log.getStackTraceString(e));
                            if (!NetworkHandler.isInternetAvailable()) {
                                FragmentHelper.notifyUserOfProblem(fragment, Error.NO_INTERNET_CONNECTION);
                            } else {
                                // somethings wrong
                                FragmentHelper.notifyUserOfProblem(fragment, Error.SOMETHINGS_WRONG);
                            }
                            if (fragment != null) {
                                fragment.disableButtons();
                            }
                        }
                    } else {
                        if (!NetworkHandler.isInternetAvailable()) {
                            FragmentHelper.notifyUserOfProblem(fragment, Error.NO_INTERNET_CONNECTION);
                        } else {
                            // jsonStr is null, notify user
                            FragmentHelper.notifyUserOfProblem(fragment, Error.SOMETHINGS_WRONG);
                        }
                        if (fragment != null) {
                            fragment.disableButtons();
                        }
                    }
                }
            }
        }

        /**
         * Fetches comments from a post
         * @param edges JSONArray
         * @param startIndex int
         * @param endIndex int
         */
        private void fetchEdgeDataComments(JSONArray edges, int startIndex, int endIndex) {
            if (!isCancelled()) {
                // get reference from fragment
                final PostFragment fragment = fragmentReference.get();
                if (fragment != null) {
                    try {
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

                            // create post
                            Comment comment = new Comment(id, text, takenAt, profile_pic_url, username);
                            if (fragment.comments == null) {
                                fragment.comments = new ArrayList<>();
                            }
                            fragment.comments.add(comment);
                        }
                    } catch (JSONException | IllegalStateException e) {
                        Log.d("PostFragment", Log.getStackTraceString(e));
                        if (!NetworkHandler.isInternetAvailable()) {
                            FragmentHelper.notifyUserOfProblem(fragment, Error.NO_INTERNET_CONNECTION);
                        } else {
                            // jsonStr is null, notify user
                            FragmentHelper.notifyUserOfProblem(fragment, Error.SOMETHINGS_WRONG);
                        }
                        if (fragment != null) {
                            fragment.disableButtons();
                        }
                    }
                }
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (!isCancelled()) {
                // get reference from fragment
                final PostFragment fragment = fragmentReference.get();
                if (fragment != null) {

                    // make progressBar gone
                    fragment.hideProgressBar();

                    // set adapters
                    if (fragment.post.getIs_sideCar()) {
                        if (fragment.postIsFromDeepLink) {
                            fragment.imagePost.setVisibility(GONE);
                            fragment.viewPager.setVisibility(VISIBLE);
                        }
                        try {
                            // set adapter for sidecars
                            if (fragment.getChildFragmentManager() != null) {
                                fragment.statePagerAdapterSideCar = new StatePagerAdapterSideCar(fragment.getChildFragmentManager(), BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT, fragment.post);
                                fragment.viewPager.setSaveFromParentEnabled(false);
                                fragment.viewPager.setAdapter(fragment.statePagerAdapterSideCar);
                            }
                        } catch (IllegalStateException | NullPointerException e) {
                            Log.d("PostFragment", Log.getStackTraceString(e));
                            FragmentHelper.notifyUserOfProblem(fragment, Error.POST_NOT_AVAILABLE_ANYMORE);

                            if (fragment != null) {
                                fragment.disableButtons();
                            }
                            return;
                        }
                    } else if (fragment.post.getIs_video()) { // is only video
                        if (fragment.postIsFromDeepLink) {
                            // set progress bar to visible
                            try {
                                fragment.progressBarVideo = fragment.header.findViewById(R.id.singleProgress_bar);
                                fragment.progressBarVideo.setVisibility(VISIBLE);
                            } catch (Exception e) {
                                Log.d("PostFragment", Log.getStackTraceString(e));
                            }
                        }

                        // load video from url
                        fragment.loadVideoFromUrl(fragment.post.getVideoUrl());

                    } else { // is only image
                        if (fragment.getContext() != null) {

                            // set height of imageView and prevent OOM from previous option
                            try {
                                fragment.imagePost.getLayoutParams().height = fragment.post.getHeight();
                                fragment.imagePost.requestLayout();
                            } catch (Exception e) {
                                Log.d("PostFragment", Log.getStackTraceString(e));
                            }

                            // load image with glide (note: error retries loading the image from the url)
                            Glide.with(fragment.requireContext())
                                    .load(fragment.post.getImageUrl())
                                    .error(Glide.with(fragment.requireContext())
                                            .load(fragment.post.getImageUrl()))
                                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                                    .skipMemoryCache(true)
                                    .dontAnimate()
                                    .into(fragment.imagePost);
                        }
                    }

                    // set basic post info like likes, caption, username etc.
                    setBasicPostInfo();

                    if (fragment.postIsFromDeepLink) {
                        // do bookmark button initialize again
                        fragment.initializeBookmarkedButton();
                    }

                    // add header and update view
                    fragment.listComments.addHeaderView(fragment.header);
                    fragment.listComments.deferNotifyDataSetChanged();
                    fragment.listComments.invalidateViews();

                    // get comments to post
                    try {
                        // before workaround:
                        // fragment.getCommentsToPost();

                        // workaround:
                        if (fragment.comments != null && fragment.bFirstAdapterFetch) {
                            // set adapter on first fetch
                            fragment.bFirstAdapterFetch = false;

                            try {
                                ListAdapterComment adapter = new ListAdapterComment(
                                        fragment.getContext(), R.layout.list_item_comment, fragment.comments);
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
                            ListAdapterComment adapter = new ListAdapterComment(
                                    fragment.getContext(), R.layout.list_item_comment, new ArrayList<Comment>());
                            fragment.listComments.setAdapter(adapter);
                        } catch (NullPointerException ee) {
                            Log.d("PostFragment", Log.getStackTraceString(e));
                        }
                    }
                }
            }
        }


        /**
         * Set basic post info like likes, caption, username etc.
         */
        private void setBasicPostInfo() {
            if (!isCancelled()) {
                // get reference from fragment
                final PostFragment fragment = fragmentReference.get();
                if (fragment != null && fragment.getContext() != null) {

                    // load profile pic app bar
                    Glide.with(fragment.requireContext())
                            .load(fragment.post.getImageUrlProfilePicOwner())
                            .error(R.drawable.placeholder_image_post_error)
                            .dontAnimate()
                            .into(fragment.imageProfilePicPostOwner);

                    // set Likes
                    fragment.textLikes.setText(fragment.getResources().getString(R.string.likes, fragment.post.getLikes()));

                    // set caption
                    fragment.textCaption.setText(fragment.post.getCaption());

                    // set username or ownerId
                    if (fragment.post.getUsername() == null && fragment.post.getOwnerId() != null) {
                        fragment.textOwnerIdOrUsername.setText(fragment.post.getOwnerId());
                        fragment.textUsernameAppBar.setText(fragment.post.getOwnerId());
                    } else {
                        fragment.textOwnerIdOrUsername.setText(fragment.post.getUsername());
                        // set title in bar under app bar
                        fragment.textUsernameAppBar.setText(fragment.post.getUsername());
                    }

                    // set date
                    if (fragment.post.getTakenAtDate() != null) {
                        fragment.textDate.setText(DateFormat.getDateTimeInstance().format(fragment.post.getTakenAtDate()));
                    }
                }
            }
        }
    }

    /**
     * Gets the comments from a post
     */
    @SuppressWarnings("CanBeFinal")
    private static class GetPostComments extends AsyncTask<Void, Void, Void> {

        private final WeakReference<PostFragment> fragmentReference;
        private NetworkHandler sh;

        // constructor
        GetPostComments(PostFragment context) {
            fragmentReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if (!isCancelled()) {
                // get reference from fragment
                final PostFragment fragment = fragmentReference.get();
                if (fragment != null) {
                    sh = new NetworkHandler();
                    if (!fragment.scrolling) {
                        makeValidURLForComments();
                        GetComments(fragment.url);
                    }
                }
            }
            return null;
        }

        /**
         * Makes valid Urls from input tags
         */
        private void makeValidURLForComments() {
            if (!isCancelled()) {
                // get reference from fragment
                final PostFragment fragment = fragmentReference.get();
                if (fragment != null) {
                    if (fragment.post != null) {
                        String urlAddress = null;

                        // make url for comments (hint: url.endCursor is null at first page fetch)
                        if (fragment.url == null || (fragment.url.endCursor == null && fragment.bFirstFetch && fragment.bFirstAdapterFetch)) {
                            urlAddress = "https://www.instagram.com/graphql/query/?query_hash=" + query_hash + "&variables={\"shortcode\":\"" + fragment.post.getShortcode() + "\",\"first\":24,\"after\":\"\"}";
                            fragment.bFirstFetch = false;
                        } else if (fragment.url.hasNextPage != null && fragment.url.hasNextPage) {
                            urlAddress = "https://www.instagram.com/graphql/query/?query_hash=" + query_hash + "&variables={\"shortcode\":\"" + fragment.post.getShortcode() + "\",\"first\":24,\"after\":\"" + fragment.url.endCursor + "\"}";
                        }
                        fragment.url = new URL(urlAddress, fragment.post.getShortcode(), FeedObject.ACCOUNT);
                    }
                }
            }
        }

        /**
         * Get comment edges from url
         *
         * @param url url to fetch from
         */
        void GetComments(URL url) {
            if (!isCancelled()) {
                // get reference from fragment
                final PostFragment fragment = fragmentReference.get();
                if (fragment != null) {
                    String newUrl = url.url;

                    // get json string from url
                    String jsonStr = sh.makeServiceCall(newUrl, fragment.getClass().getSimpleName());

                    if (jsonStr != null) {
                        // something went wrong -> possible rate limit reached
                        if (!FragmentHelper.checkIfJsonStrIsValid(jsonStr, fragment)) {
                            return;
                        }
                        // file overall as json object
                        JSONObject jsonObj;
                        try {
                            jsonObj = new JSONObject(jsonStr);

                            // getting through overall structure
                            JSONObject graphql = jsonObj.getJSONObject("data");
                            JSONObject shortcode_media = graphql.getJSONObject("shortcode_media");
                            JSONObject edge_media_to_comment = shortcode_media.getJSONObject("edge_media_to_comment");

                            // save page_info and has_next_page
                            JSONObject page_info = edge_media_to_comment.getJSONObject("page_info");
                            if (page_info.getBoolean("has_next_page")) {
                                url.hasNextPage = true;
                                url.endCursor = page_info.getString("end_cursor");
                            } else {
                                url.hasNextPage = false;
                                url.endCursor = null;
                            }

                            JSONArray edges = edge_media_to_comment.getJSONArray("edges");
                            url.jsonArrayEdges = edges;
                            if (edges != null) {
                                url.edgesTotalOfPage = edges.length();
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

                            // no comments or comments disabled - but initialize comments to get a view
                            if (edges.length() == 0) {
                                if (fragment.comments == null) {
                                    fragment.comments = new ArrayList<>();
                                }
                                return;
                            }

                            // fetch edge data
                            fetchEdgeData(edges, startIndex, endIndex);

                        } catch (JSONException | IllegalStateException e) {
                            Log.d("PostFragment", Log.getStackTraceString(e));
                        }
                    } else {
                        if (!NetworkHandler.isInternetAvailable()) {
                            FragmentHelper.notifyUserOfProblem(fragment, Error.NO_INTERNET_CONNECTION);
                        } else {
                            // jsonStr is null, notify user
                            FragmentHelper.notifyUserOfProblem(fragment, Error.SOMETHINGS_WRONG);
                        }
                        if (fragment != null) {
                            fragment.disableButtons();
                        }
                    }
                }
            }
        }

        /**
         * Fetches data from url
         *
         * @param edges      edges
         * @param startIndex startIndex
         * @param endIndex   endIndex
         */
        private void fetchEdgeData(JSONArray edges, int startIndex, int endIndex) {
            if (!isCancelled()) {
                // get reference from fragment
                final PostFragment fragment = fragmentReference.get();
                if (fragment != null) {

                    try {
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

                            // create post
                            Comment comment = new Comment(id, text, takenAt, profile_pic_url, username);
                            if (fragment.comments == null) {
                                fragment.comments = new ArrayList<>();
                            }
                            fragment.comments.add(comment);
                        }
                    } catch (JSONException | IllegalStateException e) {
                        Log.d("PostFragment", Log.getStackTraceString(e));
                        if (!NetworkHandler.isInternetAvailable()) {
                            FragmentHelper.notifyUserOfProblem(fragment, Error.NO_INTERNET_CONNECTION);
                        } else {
                            // jsonStr is null, notify user
                            FragmentHelper.notifyUserOfProblem(fragment, Error.SOMETHINGS_WRONG);
                        }
                        if (fragment != null) {
                            fragment.disableButtons();
                        }
                    }
                }
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if (!isCancelled()) {
                // get reference from fragment
                final PostFragment fragment = fragmentReference.get();
                if (fragment != null) {
                    try {

                        if (fragment.comments != null && fragment.bFirstAdapterFetch) {
                            // set adapter on first fetch
                            fragment.bFirstAdapterFetch = false;

                            try {
                                ListAdapterComment adapter = new ListAdapterComment(
                                        fragment.getContext(), R.layout.list_item_comment, fragment.comments);
                                fragment.listComments.setAdapter(adapter);
                            } catch (NullPointerException e) {
                                Log.d("PostFragment", Log.getStackTraceString(e));
                            }
                        } else {
                            fragment.listComments.deferNotifyDataSetChanged();
                            fragment.progressBarComments.setVisibility(GONE);
                            fragment.buttonLoadMoreComments.setVisibility(VISIBLE);
                        }
                        // hide "load more comment" button where there are no more comments
                        if (fragment.url.hasNextPage == null || !fragment.url.hasNextPage) {
                            fragment.buttonLoadMoreComments.setVisibility(GONE);
                        }

                    } catch (Exception e) {
                        if (!NetworkHandler.isInternetAvailable()) {
                            FragmentHelper.notifyUserOfProblem(fragment, Error.NO_INTERNET_CONNECTION);
                        } else {
                            FragmentHelper.notifyUserOfProblem(fragment, Error.SOMETHINGS_WRONG);
                        }
                        if (fragment != null) {
                            fragment.disableButtons();
                        }
                    }
                } else {
                    if (!NetworkHandler.isInternetAvailable()) {
                        FragmentHelper.notifyUserOfProblem(fragment, Error.NO_INTERNET_CONNECTION);
                    } else {
                        FragmentHelper.notifyUserOfProblem(fragment, Error.SOMETHINGS_WRONG);
                    }
                    if (fragment != null) {
                        fragment.disableButtons();
                    }
                }
            }
        }
    }
}