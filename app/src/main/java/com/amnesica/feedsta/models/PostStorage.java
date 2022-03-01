package com.amnesica.feedsta.models;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * Minimal representation of post to store in internal storage
 * hint: do changes very carefully!
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

    // constructor
    public PostStorage(String id, String shortcode, Date takenAtDate, Boolean is_video, String imageUrlThumbnail, Boolean is_sideCar, String category, String imageThumbnail) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PostStorage that = (PostStorage) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(shortcode, that.shortcode) &&
                Objects.equals(takenAtDate, that.takenAtDate) &&
                Objects.equals(is_video, that.is_video) &&
                Objects.equals(imageUrlThumbnail, that.imageUrlThumbnail) &&
                Objects.equals(imageThumbnail, that.imageThumbnail) &&
                Objects.equals(is_sideCar, that.is_sideCar) &&
                Objects.equals(category, that.category);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, shortcode, takenAtDate, is_video, imageUrlThumbnail, imageThumbnail, is_sideCar, category);
    }
}
