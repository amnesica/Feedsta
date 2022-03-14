package com.amnesica.feedsta.models;

import java.io.Serializable;
import java.util.Objects;

/**
 * Minimal representation of account to store in internal storage. Hint: do changes very carefully!
 */
public class AccountStorage implements Serializable {

    // serialVersionUID
    private static final long serialVersionUID = 1L;

    private String id;
    private String imageProfilePicUrl;
    private String username;
    private String fullName;
    private Boolean is_private;
    private String imageThumbnail;

    // constructor
    public AccountStorage(String id, String imageProfilePicUrl, String username, String fullName,
                          Boolean is_private, String imageThumbnail) {
        this.id = id;
        this.imageProfilePicUrl = imageProfilePicUrl;
        this.username = username;
        this.fullName = fullName;
        this.is_private = is_private;
        this.imageThumbnail = imageThumbnail;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public Boolean getIs_private() {
        return is_private;
    }

    public void setIs_private(Boolean is_private) {
        this.is_private = is_private;
    }

    public String getImageThumbnail() {
        return imageThumbnail;
    }

    public void setImageThumbnail(String imageThumbnail) {
        this.imageThumbnail = imageThumbnail;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccountStorage that = (AccountStorage) o;
        return Objects.equals(id, that.id) && Objects.equals(imageProfilePicUrl, that.imageProfilePicUrl) &&
               Objects.equals(username, that.username) && Objects.equals(fullName, that.fullName) &&
               Objects.equals(is_private, that.is_private) && Objects.equals(imageThumbnail,
                                                                             that.imageThumbnail);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, imageProfilePicUrl, username, fullName, is_private, imageThumbnail);
    }
}
