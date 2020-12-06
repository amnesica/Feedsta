package com.amnesica.feedsta.models;

import java.util.Date;

/**
 * Represents a comment from a post
 */
@SuppressWarnings("CanBeFinal")
public class Comment {

    private final String id;
    private final String text;
    private final Date created_at;
    private final String ownerProfilePicUrl;
    private final String username;

    // constructor
    public Comment(String id, String text, Date created_at, String ownerProfilePicUrl, String username) {
        this.id = id;
        this.text = text;
        this.created_at = created_at;
        this.ownerProfilePicUrl = ownerProfilePicUrl;
        this.username = username;
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public Date getCreated_at() {
        return created_at;
    }

    public String getOwnerProfilePicUrl() {
        return ownerProfilePicUrl;
    }

    public String getUsername() {
        return username;
    }

}
