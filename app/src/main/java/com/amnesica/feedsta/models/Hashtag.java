package com.amnesica.feedsta.models;

import java.io.Serializable;

/** Represents a hashtag */
public class Hashtag implements Serializable {

  // serialVersionUID (hint: same as computed! - use the one from the exception message)
  private static final long serialVersionUID = 6519669658345894950L;

  private String name;
  private int id;
  private int media_count;
  private String profile_pic_url;
  private String search_result_subtitle;

  // constructor
  public Hashtag(
      String name, int id, int media_count, String profile_pic_url, String search_result_subtitle) {
    this.name = name;
    this.id = id;
    this.media_count = media_count;
    this.profile_pic_url = profile_pic_url;
    this.search_result_subtitle = search_result_subtitle;
  }

  public static long getSerialVersionUID() {
    return serialVersionUID;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getMedia_count() {
    return media_count;
  }

  public void setMedia_count(int media_count) {
    this.media_count = media_count;
  }

  public String getProfile_pic_url() {
    return profile_pic_url;
  }

  public void setProfile_pic_url(String profile_pic_url) {
    this.profile_pic_url = profile_pic_url;
  }

  public String getSearch_result_subtitle() {
    return search_result_subtitle;
  }

  public void setSearch_result_subtitle(String search_result_subtitle) {
    this.search_result_subtitle = search_result_subtitle;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }
}
