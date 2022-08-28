package com.amnesica.feedsta.helper;

import com.amnesica.feedsta.models.Account;

import java.util.Comparator;

/** Helper to compare the usernames of accounts for sorting accounts in FollowingFragment */
public class CustomComparatorAccount implements Comparator<Object> {
  @Override
  public int compare(Object o1, Object o2) {
    // convert o1 to o1A (A for Account) to do comparison
    Account o1A = (Account) o1;
    Account o2A = (Account) o2;

    // o1A first, then o2A
    return o1A.getUsername().compareTo(o2A.getUsername());
  }
}
