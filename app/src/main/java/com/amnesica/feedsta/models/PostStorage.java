package com.amnesica.feedsta.models;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * Minimal representation of post to store in internal storage. Hint: do changes very carefully!
 */
public class PostStorage implements Serializable {

    // serialVersionUID
    private static final long serialVersionUID = 1L;

    private String id;
    private String shortcode;
    private Date takenAtDate; // hint: taken_at_timestamp * 1000 = date
    private Boolean is_video;
    private String imageUrlThumbnail;
    private Boolean is_sideCar;
    private String category;
    private String imageThumbnail;
    private String imageUrlProfilePicOwner;
    private int likes;
    private String ownerId;
    private String username;
    private String caption;
    private int height;

    // constructor
    public PostStorage(String id, String shortcode, Date takenAtDate, Boolean is_video,
                       String imageUrlThumbnail, Boolean is_sideCar, String category, String imageThumbnail,
                       String imageUrlProfilePicOwner, int likes, String ownerId, String username,
                       String caption, int height) {
        this.id = id;
        this.shortcode = shortcode;
        this.takenAtDate = takenAtDate;
        this.is_video = is_video;
        this.imageUrlThumbnail = imageUrlThumbnail;
        this.is_sideCar = is_sideCar;
        this.category = category;
        this.imageThumbnail = imageThumbnail;
        this.imageUrlProfilePicOwner = imageUrlProfilePicOwner;
        this.likes = likes;
        this.ownerId = ownerId;
        this.username = username;
        this.caption = caption;
        this.height = height;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getShortcode() {
        return shortcode;
    }

    public void setShortcode(String shortcode) {
        this.shortcode = shortcode;
    }

    public Date getTakenAtDate() {
        return takenAtDate;
    }

    public void setTakenAtDate(Date takenAtDate) {
        this.takenAtDate = takenAtDate;
    }

    public Boolean getIs_video() {
        return is_video;
    }

    public void setIs_video(Boolean is_video) {
        this.is_video = is_video;
    }

    public String getImageUrlThumbnail() {
        return imageUrlThumbnail;
    }

    public void setImageUrlThumbnail(String imageUrlThumbnail) {
        this.imageUrlThumbnail = imageUrlThumbnail;
    }

    public Boolean getIs_sideCar() {
        return is_sideCar;
    }

    public void setIs_sideCar(Boolean is_sideCar) {
        this.is_sideCar = is_sideCar;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getImageThumbnail() {
        return imageThumbnail;
    }

    public void setImageThumbnail(String imageThumbnail) {
        this.imageThumbnail = imageThumbnail;
    }

    public String getImageUrlProfilePicOwner() {
        return imageUrlProfilePicOwner;
    }

    public void setImageUrlProfilePicOwner(String imageUrlProfilePicOwner) {
        this.imageUrlProfilePicOwner = imageUrlProfilePicOwner;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PostStorage that = (PostStorage) o;
        return likes == that.likes && height == that.height && Objects.equals(id, that.id) && Objects.equals(
                shortcode, that.shortcode) && Objects.equals(takenAtDate, that.takenAtDate) && Objects.equals(
                is_video, that.is_video) && Objects.equals(imageUrlThumbnail, that.imageUrlThumbnail) &&
               Objects.equals(is_sideCar, that.is_sideCar) && Objects.equals(category, that.category) &&
               Objects.equals(imageThumbnail, that.imageThumbnail) && Objects.equals(imageUrlProfilePicOwner,
                                                                                     that.imageUrlProfilePicOwner) &&
               Objects.equals(ownerId, that.ownerId) && Objects.equals(username, that.username) &&
               Objects.equals(caption, that.caption);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, shortcode, takenAtDate, is_video, imageUrlThumbnail, is_sideCar, category,
                            imageThumbnail, imageUrlProfilePicOwner, likes, ownerId, username, caption,
                            height);
    }
}
