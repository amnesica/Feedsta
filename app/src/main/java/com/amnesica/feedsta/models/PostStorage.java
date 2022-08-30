package com.amnesica.feedsta.models;

import com.amnesica.feedsta.models.sidecar.Sidecar;

import java.io.Serializable;
import java.util.Date;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Minimal representation of post to store in internal storage. Hint: do changes very carefully! */
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode
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
  private Sidecar sidecar;
  private String imageUrl;
  private String videoUrl;
}
