package com.amnesica.feedsta.asynctasks;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.amnesica.feedsta.Post;
import com.amnesica.feedsta.R;
import com.amnesica.feedsta.helper.FeedObject;
import com.amnesica.feedsta.helper.NetworkHandler;
import com.amnesica.feedsta.helper.StorageHelper;
import com.amnesica.feedsta.models.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

@SuppressWarnings("deprecation")
/**
 * Async task to download multiple posts. Shows a progressDialog when downloading posts
 */
public class BatchDownloadPosts extends AsyncTask<Void, Integer, Void> {

    WeakReference<Fragment> fragmentWeakReference;
    private final List<Post> postsToDownload;
    private ProgressDialog progressDialogBatch;
    private NetworkHandler sh;

    private int downloadedItems = 0;
    private int progressMaxSize = 0;

    private boolean bSomethingsWrong = false;

    public BatchDownloadPosts(Fragment fragment, List<Post> postsToDownload, ProgressDialog progressDialogBatch) {
        this.fragmentWeakReference = new WeakReference<>(fragment);
        this.progressDialogBatch = progressDialogBatch;
        this.postsToDownload = postsToDownload;
    }

    @Override
    protected void onPreExecute() {
        showProgressDialog();
    }

    @Override
    protected Void doInBackground(Void... voids) {
        if (!isCancelled()) {
            if (fragmentWeakReference.get() != null) {

                // set initial max size to zero
                progressDialogBatch.setMax(0);

                // get all necessary info (urls of videoUrl and all sideCarUrls)
                if (listContainsVideosOrSideCarsOrPostsWithNoUrl()) {
                    getAllVideoOrImageOrSidecarUrls();
                }

                // if size is still the same -> start download of images and videos
                if (!bSomethingsWrong) {
                    // set max size of progressDialog
                    progressMaxSize = calcProgressDialogMaxSize();
                    // set length of progressDialog
                    progressDialogBatch.setMax(progressMaxSize);

                    // start downloading items
                    for (Post post : postsToDownload) {
                        if (!post.getIs_video() && !post.getIs_sideCar()) { // post is image
                            downloadImage(post, null);
                        } else if (post.getIs_video() && !post.getIs_sideCar()) { // post is video
                            downloadVideo(post, null);
                        } else if (post.getIs_sideCar()) { // post is sidecar
                            downloadAllSideCarItems(post);
                        }
                    }
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
            try {
                if (fragmentWeakReference.get() != null) {
                    Fragment fragment = fragmentWeakReference.get();

                    if (downloadedItems == progressMaxSize && !bSomethingsWrong) {
                        Toast.makeText(fragment.requireContext(), fragment.requireContext().getString(R.string.batch_download_successful), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(fragment.requireContext(), fragment.requireContext().getString(R.string.batch_download_failed), Toast.LENGTH_SHORT).show();
                    }
                    progressDialogBatch.dismiss();
                }
            } catch (Exception e) {
                Log.d("BatchDownloadPosts", Log.getStackTraceString(e));
            }
        }
    }

    /**
     * Get all missing video and image urls
     */
    private void getAllVideoOrImageOrSidecarUrls() {
        if (!isCancelled()) {
            sh = new NetworkHandler();
            for (Post post : postsToDownload) {
                URL url = makeValidURLForVideo(post);
                GetMoreInfoFromPost(url, post);
            }
        }
    }

    /**
     * Makes a valid url to fetch from Instagram
     *
     * @param post Post
     * @return URL
     */
    private URL makeValidURLForVideo(Post post) {
        if (!isCancelled()) {
            String urlAddress = "https://www.instagram.com/p/" + post.getShortcode() + "/?__a=1";
            return new URL(urlAddress, post.getShortcode(), FeedObject.ACCOUNT);
        }
        return null;
    }

    /**
     * Gets more info from post.
     * Needed if post is video or sideCar to get all urls
     *
     * @param url  url of post
     * @param post Post
     */
    private void GetMoreInfoFromPost(URL url, Post post) {
        if (!isCancelled()) {
            String newUrl = url.url;

            // get json string from url
            String jsonStr = sh.makeServiceCall(newUrl, this.getClass().getSimpleName());

            if (jsonStr != null) {
                // file overall as json object
                JSONObject jsonObj;
                try {
                    jsonObj = new JSONObject(jsonStr);

                    // getting through overall structure
                    JSONObject graphql = jsonObj.getJSONObject("graphql");
                    JSONObject shortcode_media = graphql.getJSONObject("shortcode_media");

                    // get username and imageUrl
                    post.setUsername(shortcode_media.getJSONObject("owner").getString("username"));
                    post.setImageUrl(shortcode_media.getString("display_url"));

                    // get sidecar urls if post is sidecar
                    if (post.getIs_sideCar()) {
                        JSONObject edge_sidecar_to_children = shortcode_media.getJSONObject("edge_sidecar_to_children");
                        JSONArray edges = edge_sidecar_to_children.getJSONArray("edges");

                        // storing Urls with "index" - "video or image" - "url"
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
                        post.setSidecarUrls(sidecarUrls);
                    } else if (post.getIs_video()) {
                        // get video url of just a normal video - no sidecar
                        String video_url = shortcode_media.getString("video_url");
                        post.setVideoUrl(video_url);
                    }
                } catch (JSONException e) {
                    Log.d("BatchDownloadPosts", Log.getStackTraceString(e));
                    bSomethingsWrong = true;
                }
            } else {
                bSomethingsWrong = true;
            }
        }
    }

    /**
     * Checks, if list of posts to download contains video or sideCar posts -> more data needs to be fetched
     *
     * @return true, if list of posts to download contains video or sideCar posts
     */
    private boolean listContainsVideosOrSideCarsOrPostsWithNoUrl() {
        for (Post post : postsToDownload) {
            if (post.getIs_sideCar() || post.getIs_video() ||
                    (!post.getIs_sideCar() && !post.getIs_video() && post.getImageUrl() == null)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Downloads an image from a post
     *
     * @param post            Post
     * @param imageUrlSidecar url of image if it is from a sideCar post
     */
    private void downloadImage(Post post, String imageUrlSidecar) {
        if (!isCancelled()) {
            if (fragmentWeakReference.get() != null) {
                try {
                    Fragment fragment = fragmentWeakReference.get();
                    java.net.URL url;
                    if (imageUrlSidecar == null) {
                        url = new java.net.URL(post.getImageUrl());
                    } else {
                        url = new java.net.URL(imageUrlSidecar);
                    }

                    HttpURLConnection connection = (HttpURLConnection) url
                            .openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    InputStream input = connection.getInputStream();
                    Bitmap myBitmap = BitmapFactory.decodeStream(input);

                    // save to storage
                    boolean saved = StorageHelper.saveImage(myBitmap, post.getUsername(), fragment.requireContext());

                    input.close();

                    if (saved) {
                        publishProgress(downloadedItems += 1);
                    } else {
                        throw new Exception();
                    }
                } catch (Exception e) {
                    Log.d("BatchDownloadPosts", Log.getStackTraceString(e));
                    bSomethingsWrong = true;
                }
            }
        }
    }

    /**
     * Downloads a video from a post
     *
     * @param post            Post
     * @param videoUrlSidecar url of video if it is from a sideCar post
     */
    private void downloadVideo(Post post, String videoUrlSidecar) {
        if (!isCancelled()) {
            if (fragmentWeakReference.get() != null) {
                try {
                    Fragment fragment = fragmentWeakReference.get();
                    java.net.URL url;
                    if (videoUrlSidecar == null) {
                        url = new java.net.URL(post.getVideoUrl());
                    } else {
                        url = new java.net.URL(videoUrlSidecar);
                    }

                    // open a connection to that URL
                    HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
                    int response = con.getResponseCode();

                    InputStream inputStream = con.getInputStream();
                    byte[] buffer = new byte[7 * 1024];

                    boolean saved = StorageHelper.saveVideo(inputStream, buffer, post.getUsername(), fragment.requireContext());

                    if (saved) {
                        publishProgress(downloadedItems += 1);
                    } else {
                        throw new Exception();
                    }
                } catch (Exception e) {
                    Log.d("BatchDownloadPosts", Log.getStackTraceString(e));
                    bSomethingsWrong = true;
                }
            }
        }
    }

    /**
     * Downloads all posts of the sideCar post
     *
     * @param post Post
     */
    private void downloadAllSideCarItems(Post post) {
        if (!isCancelled() && fragmentWeakReference.get() != null) {
            Iterator it = post.getSidecarUrls().entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();

                int index = (int) pair.getKey();
                ArrayList<String> typeAndUrl;
                typeAndUrl = (ArrayList<String>) pair.getValue();

                if (typeAndUrl != null) {
                    if (typeAndUrl.get(0).equals("image")) { // post is image
                        downloadImage(post, typeAndUrl.get(1));
                    } else if (typeAndUrl.get(0).equals("video")) { //  post is video
                        downloadVideo(post, typeAndUrl.get(1));
                    }
                } else {
                    bSomethingsWrong = true;
                }

                it.remove(); // avoids a ConcurrentModificationException
            }
        }
    }

    /**
     * Calculates the size of the download with all posts of all sideCars
     *
     * @return int
     */
    private int calcProgressDialogMaxSize() {
        int maxSize = 0;
        for (Post post : postsToDownload) {
            if (post != null) {
                if (!post.getIs_sideCar()) {
                    maxSize += 1;
                } else {
                    maxSize += post.getSidecarUrls().size();
                }
            }
        }
        return maxSize;
    }

    /**
     * Initializes and shows a progressDialog
     */
    private void showProgressDialog() {
        if (!isCancelled()) {
            if (fragmentWeakReference.get() != null) {
                Fragment fragment = fragmentWeakReference.get();

                progressDialogBatch = new ProgressDialog(fragment.requireContext());
                progressDialogBatch.setTitle(fragment.requireContext().getString(R.string.progress_dialog_title_download_selected_posts));
                progressDialogBatch.setMessage(fragment.requireContext().getString(R.string.progress_dialog_message_download_selected_posts));
                progressDialogBatch.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialogBatch.setProgress(0);
                progressDialogBatch.show();
            }
        }
    }
}
