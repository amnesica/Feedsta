package com.amnesica.feedsta.models;

import java.io.Serializable;

/** Represents an account or profile */
public class Account implements Serializable {

  // serialVersionUID
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
  private String imageThumbnail;

  // constructor
  public Account(
      String imageProfilePicUrl,
      String username,
      String fullName,
      Boolean is_private,
      String id,
      String imageThumbnail) {
    this.imageProfilePicUrl = imageProfilePicUrl;
    this.username = username;
    this.fullName = fullName;
    this.is_private = is_private;
    this.id = id;
    this.imageThumbnail = imageThumbnail;
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

  public String getImageThumbnail() {
    return imageThumbnail;
  }

  public void setImageThumbnail(String imageThumbnail) {
    this.imageThumbnail = imageThumbnail;
  }
}
