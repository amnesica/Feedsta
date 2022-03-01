package com.amnesica.feedsta;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/**
 * Represents a single post
 */
public class Post implements Serializable {
    // serialVersionUID (same as computed! - use the one from the exception message)
    private static final long serialVersionUID = 6519669658345894950L;

    private String id;
    private String imageUrl;
    private String videoUrl;
    private HashMap<Integer, ArrayList<String>> sidecarUrls;
    private String imageUrlThumbnail;
    private String imageUrlProfilePicOwner;
    private int likes;
    private String ownerId;
    private String username;
    private int comments;
    private String caption;
    private String shortcode;
    private Date takenAtDate; // hint: taken_at_timestamp * 1000 = date
    private Boolean is_video;
    private Boolean is_sideCar;
    private Boolean isChecked = false;
    private String category;
    private int height;
    private String imageThumbnail;

    // constructor (empty)
    public Post() {
    }

    // constructor (main constructor)
    public Post(String id,
                String imageUrl,
                int likes,
                String ownerId,
                int comments,
                String caption,
                String shortcode,
                Date takenAtDate,
                Boolean is_video,
                String username,
                String imageUrlProfilePicOwner,
                String imageUrlThumbnail,
                Boolean is_sideCar) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.likes = likes;
        this.ownerId = ownerId;
        this.comments = comments;
        this.caption = caption;
        this.shortcode = shortcode;
        this.takenAtDate = takenAtDate;
        this.is_video = is_video;
        this.username = username;
        this.imageUrlProfilePicOwner = imageUrlProfilePicOwner;
        this.imageUrlThumbnail = imageUrlThumbnail;
        this.is_sideCar = is_sideCar;
    }

    // constructor (to initialize post from FeedFragment)
    public Post(String id,
                String shortcode,
                Date takenAtDate,
                boolean is_video,
                String imageUrlThumbnail,
                Boolean is_sideCar,
                String category,
                String imageThumbnail) {
        this.id = id;
        this.shortcode = shortcode;
        this.takenAtDate = takenAtDate;
        this.is_video = is_video;
        this.imageUrlThumbnail = imageUrlThumbnail;
        this.is_sideCar = is_sideCar;
        this.category = category;
        this.imageThumbnail = imageThumbnail;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    // Minimal representation of account to store in internal storage
    // hint: do not change!
    @Deprecated
    public Post getStorageRep() {
        return new Post(id, shortcode, takenAtDate, is_video, imageUrlThumbnail, is_sideCar, category, imageThumbnail);
    }

    public HashMap<Integer, ArrayList<String>> getSidecarUrls() {
        return sidecarUrls;
    }

    public void setSidecarUrls(HashMap<Integer, ArrayList<String>> sidecarUrls) {
        this.sidecarUrls = sidecarUrls;
    }

    public Boolean getIs_sideCar() {
        return is_sideCar;
    }

    public void setIs_sideCar(Boolean is_sideCar) {
        this.is_sideCar = is_sideCar;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getImageUrlThumbnail() {
        return imageUrlThumbnail;
    }

    public void setImageUrlThumbnail(String imageUrlThumbnail) {
        this.imageUrlThumbnail = imageUrlThumbnail;
    }

    public String getImageUrlProfilePicOwner() {
        return imageUrlProfilePicOwner;
    }

    public void setImageUrlProfilePicOwner(String imageUrlProfilePicOwner) {
        this.imageUrlProfilePicOwner = imageUrlProfilePicOwner;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Boolean getIs_video() {
        return is_video;
    }

    public void setIs_video(Boolean is_video) {
        this.is_video = is_video;
    }

    public Date getTakenAtDate() {
        return takenAtDate;
    }

    public void setTakenAtDate(Date takenAtDate) {
        this.takenAtDate = takenAtDate;
    }

    public String getShortcode() {
        return shortcode;
    }

    public void setShortcode(String shortcode) {
        this.shortcode = shortcode;
    }

    public int getComments() {
        return comments;
    }

    public void setComments(int comments) {
        this.comments = comments;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getLikes() { // get likes as string
        return String.valueOf(likes);
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
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

    public Boolean getChecked() {
        return isChecked;
    }

    public void setChecked(Boolean checked) {
        isChecked = checked;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getImageThumbnail() {
        return imageThumbnail;
    }

    public void setImageThumbnail(String imageThumbnail) {
        this.imageThumbnail = imageThumbnail;
    }
}
