package com.amnesica.feedsta.helper;

import android.Manifest;

/** Helper class for static identifiers */
public class StaticIdentifier {
  // static query id
  public static final String query_id = "17888483320059182";

  // static query hash
  public static final String query_hash = "33ba35852cb50da46f5b5e889df7d159";

  // permissions stuff
  public static final String[] permsWriteOnly = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
  public static final int permsRequestCode = 200;

  // site to send get request to check internet availability
  public static final String siteToCheckInternet = "duckduckgo.com";

  // boolean for enabling debug mode
  public static boolean debugModeEnabled = false;
}
