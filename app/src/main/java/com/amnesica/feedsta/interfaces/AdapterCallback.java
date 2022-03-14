package com.amnesica.feedsta.interfaces;

import com.amnesica.feedsta.models.Account;

/**
 * Interface used to communicate between adapter and fragment to remove an account from storage
 */
public interface AdapterCallback {
    /**
     * Removes an account from storage
     *
     * @param account Account
     */
    void removeAccountFromStorage(Account account);
}
