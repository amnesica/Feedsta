package com.amnesica.feedsta.helper;

/**
 * Helper class for static identifiers
 */
public class StaticIdentifier {
    // static query id
    public static final String query_id = "17888483320059182";

    // static query hash
    public static final String query_hash = "33ba35852cb50da46f5b5e889df7d159";

    // permissions stuff
    public static final String[] permsWriteOnly = {"android.permission.WRITE_EXTERNAL_STORAGE"};
    public static final String[] permsReadAndWrite = {"android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_EXTERNAL_STORAGE"};
    public static final int permsRequestCode = 200;

    // site to send get request to check internet availability
    public static final String siteToCheckInternet = "duckduckgo.com";
}
