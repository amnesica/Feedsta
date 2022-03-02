package com.amnesica.feedsta.interfaces;

import com.amnesica.feedsta.models.Account;

/**
 * Interface used to communicate between adapter and fragment to remove an account from storage
 */
public interface AdapterCallback {
    // removes an account from storage
    void removeAccountFromStorage(Account account);
}
