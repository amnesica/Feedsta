package com.amnesica.feedsta.models;

import com.amnesica.feedsta.models.sidecar.Sidecar;

import java.io.Serializable;
import java.util.Date;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Represents a single post */
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode
public class Post implements Serializable {

  // serialVersionUID
  private static final long serialVersionUID = 6519669658345894950L;

  private String id;
  private String imageUrl;
  private String videoUrl;
  private Sidecar sidecar;
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
  private boolean isChecked;
  private String category;
  private int height;
  private String imageThumbnail;

  public void toggleChecked() {
    isChecked = !isChecked;
  }
}
