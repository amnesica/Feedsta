package com.amnesica.feedsta.models;

import androidx.annotation.Nullable;

import org.json.JSONArray;

/**
 * Represents a specific url from Instagram
 */
@SuppressWarnings({"WeakerAccess", "CanBeFinal"})
public class URL {

    public Enum FeedObject;
    public String url;
    public JSONArray jsonArrayEdges;
    public int edgesTotalOfPage;
    public String endCursor;
    public String tag;
    public Boolean hasNextPage;

    // constructor
    public URL(String url, String tag, @Nullable Enum FeedObject) {
        this.url = url;
        this.tag = tag;
        this.FeedObject = FeedObject;
    }
}
