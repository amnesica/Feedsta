package com.amnesica.feedsta.interfaces;

import com.amnesica.feedsta.models.Account;

/** Interface used to communicate between adapter and fragment to remove an account from storage */
public interface AccountsListCallback {
  void removeAccountFromStorage(Account account);
}
