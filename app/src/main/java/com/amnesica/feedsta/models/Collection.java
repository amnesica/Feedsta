package com.amnesica.feedsta.models;

/**
 * Represents a collection of bookmarks
 */
public class Collection {

    private String name;
    private String thumbnailUrl;
    private Boolean isChecked = false;
    private String imageThumbnail;

    // constructor
    public Collection(String name, String thumbnailUrl, String imageThumbnail) {
        this.name = name;
        this.thumbnailUrl = thumbnailUrl;
        this.imageThumbnail = imageThumbnail;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public boolean isChecked() {
        if (isChecked != null) {
            return isChecked;
        }
        return false;
    }

    public void toggleChecked() {
        isChecked = !isChecked;
    }

    public String getImageThumbnail() {
        return imageThumbnail;
    }

    public void setImageThumbnail(String imageThumbnail) {
        this.imageThumbnail = imageThumbnail;
    }
}
