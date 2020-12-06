package com.amnesica.feedsta.helper;

import com.amnesica.feedsta.Post;

import java.util.Comparator;

/**
 * Helper to compare dates for sorting posts
 */
public class CustomComparatorNewestFirst implements Comparator<Post> {
    @Override
    public int compare(Post p1, Post p2) throws NullPointerException {
        return p1.getTakenAtDate().compareTo(p2.getTakenAtDate()); // p1 first, then p2
    }
}
