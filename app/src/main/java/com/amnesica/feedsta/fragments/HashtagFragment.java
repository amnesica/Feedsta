package com.amnesica.feedsta.fragments;

import static android.view.View.GONE;
import static androidx.core.content.ContextCompat.getSystemService;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.amnesica.feedsta.R;
import com.amnesica.feedsta.adapter.GridViewAdapterPost;
import com.amnesica.feedsta.asynctasks.DownloadImage;
import com.amnesica.feedsta.helper.EndlessScrollListener;
import com.amnesica.feedsta.helper.Error;
import com.amnesica.feedsta.helper.FeedObject;
import com.amnesica.feedsta.helper.FragmentHelper;
import com.amnesica.feedsta.helper.NetworkHandler;
import com.amnesica.feedsta.models.Hashtag;
import com.amnesica.feedsta.models.Post;
import com.amnesica.feedsta.models.URL;
import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;

import in.srain.cube.views.GridViewWithHeaderAndFooter;

/**
 * Fragment for displaying posts of an hashtag in a gridView
 */
public class HashtagFragment extends Fragment {

    // view stuff
    private GridViewWithHeaderAndFooter gridViewHashtags;
    private EndlessScrollListener scrollListener;
    private ImageView imageHashtagPic;
    private View headerView;
    private ProgressBar progressBar;
    private GridViewAdapterPost adapter;
    private Toolbar toolbar;

    // fetch stuff
    private Hashtag hashtag;
    private ArrayList<Post> posts;
    private final boolean isImageFitToScreen = false;
    private Boolean bFirstFetch;
    private Boolean bFirstAdapterFetch;
    private int postCounter = 0;
    private Boolean addNextPageToPlaceholder = false;
    private URL url;

    // if hashtag is from deep link -> get more info about hashtag
    private Boolean hashtagIsFromDeepLink = false;

    static HashtagFragment newInstance(Hashtag hashtag) {
        Bundle args = new Bundle();
        args.putSerializable("hashtag", hashtag);
        HashtagFragment fragment = new HashtagFragment();
        fragment.setArguments(args);
        return fragment;
    }

    // public access for open deep link
    public static HashtagFragment newInstance(String name) {
        Bundle args = new Bundle();
        args.putSerializable("name", name);
        HashtagFragment fragment = new HashtagFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_hashtag, container, false);

        // retrieve account
        if (this.getArguments() != null) {
            if (getArguments().getSerializable("name") != null) {
                String name = (String) getArguments().getSerializable("name");
                // initial hashtag with only name (hint: id and search_result_subtitle not needed here)
                hashtag = new Hashtag(name, 0, 0, "", "");
                hashtagIsFromDeepLink = true;
            } else {
                hashtag = (Hashtag) getArguments().getSerializable("hashtag");
            }
        }

        setupToolbar(view);

        progressBar = view.findViewById(R.id.progressBarHashtag);
        progressBar.setProgress(0);

        setupGridViewWithHeaderAndFooter(view);

        // initialize booleans for first time fetching
        bFirstFetch = true;
        bFirstAdapterFetch = true;

        // start fetching posts for hashtag
        checkConnectionAndFetchHashtagImages();

        return view;
    }

    /**
     * Sets up the toolbar with specific navigation icon and the menu to copy the link to the hashtag or to
     * download the profile photo of the hashtag
     *
     * @param view View
     */
    private void setupToolbar(final View view) {
        if (view == null) return;
        toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_24dp);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requireActivity().onBackPressed();
            }
        });

        // set menu to copy hashtag
        toolbar.inflateMenu(R.menu.menu_hashtag);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.menu_copy_hashtag_link) {
                    copyHashtagToClipboard();
                } else if (item.getItemId() == R.id.menu_download_hashtag_profile_photo) {
                    downloadProfilePhoto();
                }
                return false;
            }
        });
    }

    /**
     * Puts url to hashtag in clipboard
     */
    private void copyHashtagToClipboard() {
        ClipboardManager clipboard = getSystemService(requireContext(), ClipboardManager.class);
        ClipData clip = ClipData.newPlainText("urlToCopy", createUrlForCopyHashtag());
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            // make toast that link has been copied
            FragmentHelper.showToast(getResources().getString(R.string.link_copied), requireActivity(),
                                     requireContext());
        }
    }

    /**
     * Creates a specific string to copy to clipboard. Adds advertising string or returns just the url
     *
     * @return String
     */
    private CharSequence createUrlForCopyHashtag() {
        if (hashtag.getName() == null) return "";

        if (FragmentHelper.addAdvertisingStringToClipboard(HashtagFragment.this)) {
            return "https://www.instagram.com/explore/tags/" + hashtag.getName() + getResources().getString(
                    R.string.copy_hashtag_second_part);
        } else {
            return "https://www.instagram.com/explore/tags/" + hashtag.getName();
        }
    }

    /**
     * Start fetching posts for hashtag
     */
    private void checkConnectionAndFetchHashtagImages() {
        if (hashtag != null) {
            new CheckConnectionAndFetchPosts(HashtagFragment.this).execute();
        }
    }

    /**
     * Set ScrollListener for gridView
     *
     * @param gridViewHashtags GridViewWithHeaderAndFooter
     */
    private void createEndlessScrollListener(GridViewWithHeaderAndFooter gridViewHashtags) {
        scrollListener = new EndlessScrollListener(gridViewHashtags, new EndlessScrollListener.RefreshList() {
            @Override
            public void onRefresh(int pageNumber) {
                // start fetching posts for hashtag
                checkConnectionAndFetchHashtagImages();
            }
        });
    }

    /**
     * Set gridView with header and footer and clickListener
     *
     * @param view View
     */
    private void setupGridViewWithHeaderAndFooter(final View view) {
        if (view == null) return;
        gridViewHashtags = view.findViewById(R.id.gridViewHashtagPosts);

        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        headerView = layoutInflater.inflate(R.layout.gridview_header_hashtags, null, false);
        imageHashtagPic = headerView.findViewById(R.id.accountOrHashtagProfilePic);

        // add onClickListener for imageHashtagPic for fullscreen
        imageHashtagPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isImageFitToScreen) {
                    // new fullscreenFragment
                    FullscreenProfileImageFragment fullscreenProfileImageFragment =
                            FullscreenProfileImageFragment.newInstance(hashtag.getProfile_pic_url());

                    // add fragment to container
                    FragmentHelper.addFragmentToContainer(fullscreenProfileImageFragment,
                                                          requireActivity().getSupportFragmentManager());
                }
            }
        });

        // add header view
        gridViewHashtags.addHeaderView(headerView);

        // set onItemClickListener on gridView
        gridViewHashtags.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // get post
                Post postToSend = posts.get(position);

                // new PostFragment
                PostFragment postFragment = PostFragment.newInstance(postToSend);

                // add fragment to container
                FragmentHelper.addFragmentToContainer(postFragment,
                                                      requireActivity().getSupportFragmentManager());
            }
        });
    }

    private void hideProgressBar() {
        requireActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(GONE);
            }
        });
    }

    @Override
    public void onResume() {
        // Slide down bottom navigation view if necessary
        FragmentHelper.slideDownBottomNavigationBar(getActivity());

        super.onResume();
    }

    /**
     * Set TextView specified with resource. boldText is shown bold and normalText is shown normal
     *
     * @param resource int
     * @param boldText String
     */
    private void updateResourceTextString(int resource, String boldText) {
        SpannableString str = new SpannableString(
                boldText + getResources().getString(R.string.normalString_hashtag_fragment));
        str.setSpan(new StyleSpan(Typeface.BOLD), 0, boldText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ((TextView) headerView.findViewById(resource)).setText(str);
    }

    /**
     * Updates the toolbar title with specified string title
     *
     * @param title String
     */
    private void updateToolbarTitle(String title) {
        if (title == null || toolbar == null) return;
        toolbar.setTitle(title);
    }

    /**
     * Starts async task to download a profile photo
     */
    private void downloadProfilePhoto() {
        if (hashtag == null) return;

        // start save image from url
        DownloadImage downloadImageAsyncTask = new DownloadImage(HashtagFragment.this,
                                                                 hashtag.getProfile_pic_url(),
                                                                 hashtag.getName());
        downloadImageAsyncTask.execute();
    }

    /**
     * Async task to check internet connection and notify user if there is no connection. Starts fetching at
     * the end
     */
    private static class CheckConnectionAndFetchPosts extends AsyncTask<Void, Void, Void> {

        private final WeakReference<HashtagFragment> fragmentReference;
        boolean isInternetAvailable = false;

        // constructor
        CheckConnectionAndFetchPosts(HashtagFragment context) {
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
            final HashtagFragment fragment = fragmentReference.get();
            if (fragment == null) return;

            if (isInternetAvailable) {
                new GetHashtagImages(fragment).execute();
            } else {
                FragmentHelper.notifyUserOfProblem(fragment, Error.NO_INTERNET_CONNECTION);
            }
        }
    }

    /**
     * Async task to fetch the posts of the hashtag
     */
    private static class GetHashtagImages extends AsyncTask<Void, Void, Void> {

        private final WeakReference<HashtagFragment> fragmentReference;
        private NetworkHandler sh;

        // constructor
        GetHashtagImages(HashtagFragment context) {
            fragmentReference = new WeakReference<>(context);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (isCancelled()) return;

            // get reference from fragment
            final HashtagFragment fragment = fragmentReference.get();
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
            final HashtagFragment fragment = fragmentReference.get();
            if (fragment == null) return null;

            sh = new NetworkHandler();

            // make valid URL from hashtag and create url
            makeValidUrl();

            // only fetch if there are more pages
            if (fragment.bFirstAdapterFetch ||
                (fragment.scrollListener != null && fragment.scrollListener.hasMorePages)) {
                fetchPageDataOfUrl(fragment.url);
                int startIndex = 0;
                int endIndex = fragment.url.edgesTotalOfPage;
                fetchEdgeData(fragment.url, fragment.url.jsonArrayEdges, startIndex, endIndex);
            }
            return null;
        }

        /**
         * Makes valid urls from input tags
         */
        private void makeValidUrl() {
            if (isCancelled()) return;

            // get reference from fragment
            final HashtagFragment fragment = fragmentReference.get();
            if (fragment == null) return;

            String urlAddress = null;

            // make url for comments (hint: url.endCursor is null at first page fetch)
            if (fragment.url == null ||
                (fragment.url.endCursor == null && fragment.bFirstFetch && fragment.bFirstAdapterFetch)) {
                urlAddress = "https://www.instagram.com/explore/tags/" + fragment.hashtag.getName() +
                             "/?__a=1&max_id=";
                fragment.bFirstFetch = false;
            } else if (fragment.url.hasNextPage != null && fragment.url.hasNextPage) {
                urlAddress = "https://www.instagram.com/explore/tags/" + fragment.hashtag.getName() +
                             "/?__a=1&max_id=" + fragment.url.endCursor;
                fragment.addNextPageToPlaceholder = true;
            } else if (fragment.url.hasNextPage != null) {
                // no more pages or posts
                fragment.scrollListener.noMorePages();
                return;
            }
            fragment.url = new URL(urlAddress, fragment.hashtag.getName(), FeedObject.HASHTAG);
        }

        /**
         * Fetches data from URL
         *
         * @param url url to fetch from
         */
        private void fetchPageDataOfUrl(URL url) {
            if (isCancelled()) return;

            // get reference from fragment
            final HashtagFragment fragment = fragmentReference.get();
            if (fragment == null) return;

            // fetch data for hashtag
            String jsonStr = null;
            if (url.url != null) {

                // get json string from url
                jsonStr = sh.makeServiceCall(url.url, fragment.getClass().getSimpleName());
            } else if (!fragment.scrollListener.hasMorePages) {
                // no more pages/posts
                return;
            }

            if (jsonStr == null) {
                FragmentHelper.showNetworkOrSomethingWrongErrorToUser(fragment);
                return;
            }

            try {
                if (!FragmentHelper.checkIfJsonStrIsValid(jsonStr, fragment)) {
                    // stop fetching
                    return;
                }

                // file overall as json object
                JSONObject jsonObj = new JSONObject(jsonStr);

                if (fragment.bFirstAdapterFetch) {
                    fragment.hashtag.setProfile_pic_url(jsonObj.getJSONObject("graphql")
                                                                .getJSONObject("hashtag")
                                                                .getString("profile_pic_url"));

                    // set missing information when hashtag is from deep link
                    if (fragment.hashtagIsFromDeepLink) {
                        fragment.hashtag.setMedia_count(jsonObj.getJSONObject("graphql").getJSONObject(
                                "hashtag").getJSONObject("edge_hashtag_to_media").getInt("count"));
                    }
                }

                // save page_info and has_next_page
                JSONObject page_info = jsonObj.getJSONObject("graphql").getJSONObject("hashtag")
                        .getJSONObject("edge_hashtag_to_media").getJSONObject("page_info");
                if (page_info.getBoolean("has_next_page")) {
                    url.hasNextPage = true;
                    url.endCursor = page_info.getString("end_cursor");
                } else {
                    url.hasNextPage = false;
                }

                JSONArray edges = jsonObj.getJSONObject("graphql").getJSONObject("hashtag").getJSONObject(
                        "edge_hashtag_to_media").getJSONArray("edges");

                url.jsonArrayEdges = edges;
                if (edges != null && !edges.isNull(0)) {
                    url.edgesTotalOfPage = edges.length();
                } else {
                    url.edgesTotalOfPage = 0;
                }
            } catch (JSONException e) {
                Log.d("HashtagFragment", Log.getStackTraceString(e));
            }
        }

        /**
         * Fetch data of a single edge (JSONObject)
         *
         * @param url        URL
         * @param edges      JSONArray
         * @param startIndex int
         * @param endIndex   int
         */
        private void fetchEdgeData(URL url, JSONArray edges, int startIndex, int endIndex) {
            if (isCancelled()) return;

            // get reference from fragment
            final HashtagFragment fragment = fragmentReference.get();
            if (fragment == null) return;

            try {
                // Just 10 posts per request
                for (int i = startIndex; i < endIndex; i++) {
                    if (edges.getJSONObject(i) != null) {
                        // get edge object
                        JSONObject edge = edges.getJSONObject(i);

                        // increase post counter
                        fragment.postCounter += 1;

                        // get node of selected edge
                        JSONObject node = edge.getJSONObject("node");

                        // check if post is sidecar
                        boolean is_sidecar = node.getString("__typename").equals("GraphSidecar");

                        // get caption of post
                        JSONArray edgesCaption = node.getJSONObject("edge_media_to_caption").getJSONArray(
                                "edges");
                        String caption = null;
                        if (edgesCaption.length() != 0 && !edgesCaption.isNull(0)) {
                            caption = edgesCaption.getJSONObject(0).getJSONObject("node").getString("text");
                        }

                        // Create post with username from account
                        Post post = new Post(node.getString("id"), node.getString("display_url"),
                                             Integer.parseInt(
                                                     node.getJSONObject("edge_liked_by").getString("count")),
                                             node.getJSONObject("owner").getString("id"), Integer.parseInt(
                                node.getJSONObject("edge_media_to_comment").getString("count")), caption,
                                             node.getString("shortcode"),
                                             new Date(node.getLong("taken_at_timestamp") * 1000),
                                             node.getBoolean("is_video"), fragment.hashtag.getName(),
                                             fragment.hashtag.getProfile_pic_url(),
                                             node.getString("thumbnail_src"), is_sidecar);

                        if (fragment.posts == null) {
                            fragment.posts = new ArrayList<>();

                            for (int j = 0; j < url.edgesTotalOfPage; j++) {
                                Post placeholder = new Post();
                                fragment.posts.add(placeholder);
                            }
                        }

                        if (fragment.posts != null &&
                            fragment.addNextPageToPlaceholder) { // add placeholder when fetching
                            // next page
                            fragment.addNextPageToPlaceholder = false; // only once per next page
                            for (int k = 0; k < url.edgesTotalOfPage; k++) {
                                Post placeholder = new Post();
                                fragment.posts.add(placeholder);
                            }
                        }
                        assert fragment.posts != null;
                        fragment.posts.set(fragment.postCounter - 1, post);
                    }
                }
            } catch (JSONException e) {
                Log.d("HashtagFragment", Log.getStackTraceString(e));

                FragmentHelper.showNetworkOrSomethingWrongErrorToUser(fragment);
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if (isCancelled()) return;

            // get reference from fragment
            final HashtagFragment fragment = fragmentReference.get();
            if (fragment == null) return;

            if (fragment.posts != null && fragment.bFirstAdapterFetch) {
                fragment.bFirstAdapterFetch = false;

                // set adapter
                setAdapter();

                // update textCountItems and toolbar title
                fragment.updateResourceTextString(
                        fragment.headerView.findViewById(R.id.textCountItems).getId(),
                        Integer.toString(fragment.hashtag.getMedia_count()));
                fragment.updateToolbarTitle("#" + fragment.hashtag.getName());

                // set profile pic
                Glide.with(fragment.requireContext()).load(fragment.hashtag.getProfile_pic_url()).error(
                        R.drawable.placeholder_image_post_error).dontAnimate().into(fragment.imageHashtagPic);
            } else {
                if (fragment.adapter != null) {
                    fragment.adapter.notifyDataSetChanged();
                }
                if (fragment.gridViewHashtags != null) {
                    fragment.gridViewHashtags.invalidateViews();
                }

                // only notify hasMorePages if there are more pages
                if (fragment.scrollListener != null && fragment.scrollListener.hasMorePages) {
                    fragment.scrollListener.notifyMorePages();
                }
            }
        }

        /**
         * Sets the adapter for the gridView
         */
        private void setAdapter() {
            if (isCancelled()) return;

            // get reference from fragment
            final HashtagFragment fragment = fragmentReference.get();
            if (fragment == null) return;

            // hide progressBar
            fragment.hideProgressBar();

            // set adapter
            fragment.adapter = new GridViewAdapterPost(fragment.getContext(), R.layout.gridview_item_image,
                                                       fragment.posts);
            fragment.gridViewHashtags.setAdapter(fragment.adapter);

            // create and set onScrollListener
            fragment.createEndlessScrollListener(fragment.gridViewHashtags);
            fragment.gridViewHashtags.setOnScrollListener(fragment.scrollListener);
        }
    }
}