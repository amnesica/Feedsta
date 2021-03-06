package com.amnesica.feedsta;

import java.io.Serializable;

/**
 * Represents an account or profile
 */
@SuppressWarnings("CanBeFinal")
public class Account implements Serializable {
    // serialVersionUID (same as computed! - use the one from the exception message)
    private static final long serialVersionUID = -1316217135523228502L;

    private String id;
    private String imageProfilePicUrl;
    private String username;
    private String fullName;
    private Boolean is_private;
    private String biography;
    private int edge_followed_by;
    private int edge_follow;
    private String external_url;
    private int itemCount;
    private Boolean is_verified = false;

    // constructor
    public Account(String imageProfilePicUrl, String username, String fullName, Boolean is_private, String id) {
        this.imageProfilePicUrl = imageProfilePicUrl;
        this.username = username;
        this.fullName = fullName;
        this.is_private = is_private;
        this.id = id;
    }

    // Minimal representation of account to store in internal storage
    // hint: do not change!
    @Deprecated
    public Account getStorageRep() {
        return new Account(imageProfilePicUrl, username, fullName, is_private, id);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    public String getExternal_url() {
        return external_url;
    }

    public void setExternal_url(String external_url) {
        this.external_url = external_url;
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    public int getEdge_followed_by() {
        return edge_followed_by;
    }

    public void setEdge_followed_by(int edge_followed_by) {
        this.edge_followed_by = edge_followed_by;
    }

    public int getEdge_follow() {
        return edge_follow;
    }

    public void setEdge_follow(int edge_follow) {
        this.edge_follow = edge_follow;
    }

    public Boolean getIs_private() {
        return is_private;
    }

    public void setIs_private(Boolean is_private) {
        this.is_private = is_private;
    }

    public String getImageProfilePicUrl() {
        return imageProfilePicUrl;
    }

    public void setImageProfilePicUrl(String imageProfilePicUrl) {
        this.imageProfilePicUrl = imageProfilePicUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Boolean getIs_verified() {
        return is_verified;
    }

    public void setIs_verified(Boolean is_verified) {
        this.is_verified = is_verified;
    }
}
