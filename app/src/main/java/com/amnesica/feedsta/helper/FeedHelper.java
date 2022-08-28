package com.amnesica.feedsta.helper;

/** Helper for setting important values for feed */
public class FeedHelper {
  // amount of posts to be fetched from a single account in feed
  // hint: 12 is the common size of a page
  public static final int counterPostBorder = 48;

  // amount of posts to be fetched on a single page
  // hint: 12 is the common size of a page
  public static final int fetchBorderPerPage = 48;

  // overall amount of posts in feed
  // hint: to prevent OutOfMemoryException this border might be lower
  public static final int counterFeedBorder = 400;
}
