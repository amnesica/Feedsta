package com.amnesica.feedsta.helper;

/**
 * enum for distinguishing errors
 */
public enum Error {
    NO_INTERNET_CONNECTION,                     // no internet connection
    SOMETHINGS_WRONG,                           // unspecified error, somethings wrong (or <!DOCTYPE> or request is null)
    UPDATING_BOOKMARKS_CATEGORY_FAILED,         // something went wrong when trying to update category of bookmarked post
    POST_NOT_AVAILABLE_ANYMORE,                 // post is not available anymore, invoked when post is null and is set to adapter
    NOT_ALL_ACCOUNTS_COULD_BE_QUERIED,          // when updating the feed and not all accounts could be queried
}
