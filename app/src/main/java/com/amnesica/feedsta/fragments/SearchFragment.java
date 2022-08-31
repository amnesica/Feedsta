package com.amnesica.feedsta.fragments;

import static android.view.View.GONE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.amnesica.feedsta.R;
import com.amnesica.feedsta.adapter.ListAdapterSearch;
import com.amnesica.feedsta.helper.Error;
import com.amnesica.feedsta.helper.FragmentHelper;
import com.amnesica.feedsta.helper.NetworkHandler;
import com.amnesica.feedsta.models.Account;
import com.amnesica.feedsta.models.Hashtag;
import com.amnesica.feedsta.models.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/** Fragment for searching accounts */
@SuppressLint("ClickableViewAccessibility")
public class SearchFragment extends Fragment {

  // view stuff
  private ListView listView;
  private String inputString;
  private TextView textInputField;
  private ProgressBar progressBar;

  // list with accounts and hashtags
  private ArrayList<Object> listObjects;

  public SearchFragment() {
    // Required empty public constructor
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    // inflate the layout for this fragment
    return inflater.inflate(R.layout.fragment_search, container, false);
  }

  @Override
  public void onResume() {
    super.onResume();
    // listener to get inputString and start search from keyboard
    setListenerInputFieldToStartFromKeyboard();

    // listener to clear input string with clear icon
    setOnTouchListenerToUseClearIcon();

    // listener to get listView element and show profile page
    setItemClickListenerListView();

    // listener to show or hide nav bar
    setScrollListenerListView();
  }

  /** Sets up listener to get inputString and start search from keyboard search icon */
  private void setListenerInputFieldToStartFromKeyboard() {
    // when clicking on search key in keyboard start CheckConnectionAndStartSearch async task
    textInputField.setOnEditorActionListener(
        (view, actionId, event) -> {
          if (actionId == EditorInfo.IME_ACTION_SEARCH) {

            // clear listView to show new list items
            clearListsForRefresh();

            // get text from inputField
            inputString = textInputField.getText().toString();

            // manipulate input
            if (inputString.contains(" ")) {
              // replace whitespaces with "+"
              makeValidInputWithSpace();
            }

            progressBar = requireView().findViewById(R.id.progressBarSearch);
            progressBar.setProgress(0);

            // validate string and start getAccountsAndHashtags
            if (inputStringIsValid(inputString)) {
              new CheckConnectionAndStartSearch(SearchFragment.this).execute();

              // hide keyboard
              InputMethodManager imm =
                  (InputMethodManager)
                      view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
              assert imm != null;
              imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
            return true;
          }
          return false;
        });
  }

  /** Set up clear icon to remove input string */
  private void setOnTouchListenerToUseClearIcon() {
    String inputString = "";

    textInputField.setText(inputString);
    final Drawable clearDrawable =
        ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_clear_24dp);

    if (clearDrawable == null) return;
    clearDrawable.setBounds(
        0, 0, clearDrawable.getIntrinsicWidth(), clearDrawable.getIntrinsicHeight());

    textInputField.setCompoundDrawables(
        null, null, inputString.equals("") ? null : clearDrawable, null);

    textInputField.setOnTouchListener(
        (view, event) -> {
          if (textInputField.getCompoundDrawables()[2] == null) {
            return false;
          }
          if (event.getAction() != MotionEvent.ACTION_UP) {
            return false;
          }
          if (event.getX()
              > textInputField.getWidth()
                  - textInputField.getPaddingRight()
                  - clearDrawable.getIntrinsicWidth()) {
            textInputField.setText("");
            textInputField.setCompoundDrawables(null, null, null, null);
          }
          return false;
        });

    textInputField.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {
            textInputField.setCompoundDrawables(
                null,
                null,
                textInputField.getText().toString().equals("") ? null : clearDrawable,
                null);
          }

          @Override
          public void afterTextChanged(Editable arg0) {}

          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        });
  }

  /** Sets up listener to get listView element and show profile page */
  private void setItemClickListenerListView() {
    // get list item in list view
    listView.setOnItemClickListener(
        (parent, view, position, id) -> {
          if (listObjects.get(position) instanceof Account) {
            // only clickable if account is public
            if (!((Account) listObjects.get(position)).getIs_private()) {
              // go to profile page of account
              goToProfilePageOfAccount((Account) listObjects.get(position));
            }
          } else if (listObjects.get(position) instanceof Hashtag) {
            // go to hashtag page with posts
            goToHashtagPageOfHashtag((Hashtag) listObjects.get(position));
          }
        });
  }

  /** Sets up listener to hide and show nav bar */
  private void setScrollListenerListView() {
    listView.setOnScrollListener(
        new AbsListView.OnScrollListener() {
          private int mLastFirstVisibleItem;

          @Override
          public void onScrollStateChanged(AbsListView view, int scrollState) {}

          @Override
          public void onScroll(
              AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            if (mLastFirstVisibleItem < firstVisibleItem) {
              FragmentHelper.slideDownBottomNavigationBar(requireActivity());
            }
            if (mLastFirstVisibleItem > firstVisibleItem) {
              FragmentHelper.slideUpBottomNavigationBar(requireActivity());
            }
            mLastFirstVisibleItem = firstVisibleItem;
          }
        });
  }

  private void goToHashtagPageOfHashtag(Hashtag hashtag) {
    // new hashtagFragment
    HashtagFragment hashtagFragment = HashtagFragment.newInstance(hashtag);

    // add fragment to container
    FragmentHelper.addFragmentToContainer(
        hashtagFragment, requireActivity().getSupportFragmentManager());
  }

  private void goToProfilePageOfAccount(Account account) {
    // new profileFragment
    ProfileFragment profileFragment = ProfileFragment.newInstance(account);

    // add fragment to container
    FragmentHelper.addFragmentToContainer(
        profileFragment, requireActivity().getSupportFragmentManager());
  }

  /** Hides the progressBar */
  private void hideProgressBar() {
    if (this.getActivity() != null) {
      requireActivity().runOnUiThread(() -> progressBar.setVisibility(GONE));
    }
  }

  @Override
  public void onHiddenChanged(boolean hidden) {
    super.onHiddenChanged(hidden);

    // set highlighted item on nav bar to "search"
    if (!hidden) {
      FragmentHelper.setBottomNavViewSelectElem(getActivity(), R.id.navigation_search);
    }
  }

  /** Eliminates whitespaces in inputString */
  private void makeValidInputWithSpace() {
    inputString = inputString.replaceAll("\\s+", "+");
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    // find views
    listView = view.findViewById(R.id.listAccountsAndHashtags);
    textInputField = requireActivity().findViewById(R.id.inputTag);
  }

  /** Clear objects list to show new list items */
  private void clearListsForRefresh() {
    if (listObjects != null) {
      listObjects.clear();
      listObjects = null;
    }
  }

  private boolean inputStringIsValid(String inputString) {
    return inputString != null && !inputString.isEmpty();
  }

  /** Async task to checks internet connection and notify user if there is no connection */
  private static class CheckConnectionAndStartSearch extends AsyncTask<Void, Void, Void> {

    private final WeakReference<SearchFragment> fragmentReference;
    boolean isInternetAvailable = false;

    // constructor
    CheckConnectionAndStartSearch(SearchFragment context) {
      fragmentReference = new WeakReference<>(context);
    }

    @Override
    protected Void doInBackground(Void... voids) {
      if (isCancelled()) return null;
      isInternetAvailable = NetworkHandler.isInternetAvailable();
      return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
      super.onPostExecute(aVoid);
      if (isCancelled()) return;

      final SearchFragment fragment = fragmentReference.get();
      if (fragment == null) return;

      if (isInternetAvailable) {
        new GetAccountsAndHashtags(fragment).execute();
      } else {
        FragmentHelper.notifyUserOfProblem(fragment, Error.NO_INTERNET_CONNECTION);
      }
    }
  }

  /** Async task to fetch accounts and hashtags */
  private static class GetAccountsAndHashtags extends AsyncTask<Void, Void, Void> {

    private final WeakReference<SearchFragment> fragmentReference;

    // helper stuff
    private NetworkHandler sh;
    private URL url;

    // only retain a weak reference to the activity
    GetAccountsAndHashtags(SearchFragment context) {
      fragmentReference = new WeakReference<>(context);
    }

    @Override
    protected void onPreExecute() {
      super.onPreExecute();
      if (isCancelled()) return;

      final SearchFragment fragment = fragmentReference.get();
      if (fragment == null) return;

      // make progressBar visible
      fragment.progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    protected Void doInBackground(Void... arg0) {
      if (isCancelled()) return null;

      SearchFragment fragment = fragmentReference.get();
      if (fragment == null) return null;

      sh = new NetworkHandler();

      // make valid URL from inputString
      makeValidUrls(fragment.inputString);

      // fetch data of url
      fetchDataOfUrl(url);

      return null;
    }

    /**
     * Makes valid urls from input
     *
     * @param inputString String
     */
    private void makeValidUrls(String inputString) {
      if (isCancelled()) return;

      final String urlAddress =
          "https://www.instagram.com/web/search/topsearch/?context=blended&query=" + inputString;
      url = new URL(urlAddress, inputString, null);
    }

    /**
     * Fetches data from url
     *
     * @param url URL
     */
    void fetchDataOfUrl(URL url) {
      if (isCancelled()) return;

      final SearchFragment fragment = fragmentReference.get();
      if (fragment == null) return;

      String jsonStr = sh.makeServiceCall(url.url, SearchFragment.class.getSimpleName());

      if (jsonStr == null) {
        FragmentHelper.showNetworkOrSomethingWrongErrorToUser(fragment);
      }

      if (!FragmentHelper.checkIfJsonStrIsValid(jsonStr, fragment)) {
        // stop fetching
        return;
      }

      JSONObject jsonObj = null;
      try {
        jsonObj = new JSONObject(jsonStr);
      } catch (JSONException e) {
        Log.d("SearchFragment", Log.getStackTraceString(e));
      }

      if (jsonObj != null) {
        fetchDataOfUrlForHashtag(jsonObj);
        fetchDataOfUrlForAccount(jsonObj);
      }
    }

    /**
     * Fetch account data from JSONObject
     *
     * @param jsonObj JSONObject
     */
    private void fetchDataOfUrlForAccount(JSONObject jsonObj) {
      if (isCancelled()) return;

      final SearchFragment fragment = fragmentReference.get();
      if (fragment == null) return;

      try {
        // getting through overall structure
        JSONArray users = jsonObj.getJSONArray("users");

        // fetch and create user and add to accounts list
        fetchUserOfUrl(users);
      } catch (JSONException e) {
        FragmentHelper.showNetworkOrSomethingWrongErrorToUser(fragment);
      }
    }

    /**
     * Fetch hashtag data from JSONObject
     *
     * @param jsonObj JSONObject
     */
    private void fetchDataOfUrlForHashtag(JSONObject jsonObj) {
      if (isCancelled()) return;

      final SearchFragment fragment = fragmentReference.get();
      if (fragment == null) return;

      try {
        // getting through overall structure
        JSONArray hashtagsJSONArray = jsonObj.getJSONArray("hashtags");

        // fetch and create hashtag and add to hashtag list
        fetchHashtagOfUrl(hashtagsJSONArray);
      } catch (final JSONException e) {
        FragmentHelper.showNetworkOrSomethingWrongErrorToUser(fragment);
      }
    }

    /**
     * Fetches user of url and creates new account
     *
     * @param users JSONArray
     */
    private void fetchUserOfUrl(JSONArray users) {
      if (isCancelled()) return;

      final SearchFragment fragment = fragmentReference.get();
      if (fragment == null) return;

      if (fragment.listObjects == null) {
        fragment.listObjects = new ArrayList<>();
      }

      for (int i = 0; i < users.length(); i++) {
        try {
          // get user
          JSONObject userWithPosition = users.getJSONObject(i);
          JSONObject user = userWithPosition.getJSONObject("user");

          // create account
          Account account =
              new Account(
                  user.getString("profile_pic_url"),
                  user.getString("username"),
                  user.getString("full_name"),
                  user.getBoolean("is_private"),
                  user.getString("pk"),
                  // pk is id
                  null);

          // add verified status here
          account.setIs_verified(user.getBoolean("is_verified"));

          // add account to list
          fragment.listObjects.add(account);
        } catch (JSONException e) {
          Log.d("SearchFragment", Log.getStackTraceString(e));

          FragmentHelper.showNetworkOrSomethingWrongErrorToUser(fragment);
        }
      }
    }

    /**
     * Fetches hashtag of url and creates new hashtag
     *
     * @param hashtagsJSONArray JSONArray
     */
    private void fetchHashtagOfUrl(JSONArray hashtagsJSONArray) {
      if (isCancelled()) return;

      final SearchFragment fragment = fragmentReference.get();
      if (fragment == null) return;

      if (fragment.listObjects == null) {
        fragment.listObjects = new ArrayList<>();
      }

      for (int i = 0; i < hashtagsJSONArray.length(); i++) {
        try {
          // get hashtag
          JSONObject hashtagWithPosition = hashtagsJSONArray.getJSONObject(i);
          JSONObject hashtagObj = hashtagWithPosition.getJSONObject("hashtag");

          // create hashtag
          Hashtag hashtag =
              new Hashtag(
                  hashtagObj.getString("name"),
                  hashtagObj.getInt("id"),
                  hashtagObj.getInt("media_count"),
                  hashtagObj.getString("profile_pic_url"),
                  hashtagObj.getString("search_result_subtitle"));

          // add hashtag to list
          fragment.listObjects.add(hashtag);
        } catch (JSONException e) {
          Log.d("SearchFragment", Log.getStackTraceString(e));

          FragmentHelper.showNetworkOrSomethingWrongErrorToUser(fragment);
        }
      }
    }

    @Override
    protected void onPostExecute(Void result) {
      super.onPostExecute(result);
      if (isCancelled()) return;

      final SearchFragment fragment = fragmentReference.get();
      if (fragment == null) return;

      fragment.hideProgressBar();

      try {
        if (fragment.listObjects != null && !fragment.listObjects.isEmpty()) {
          // make no results text disappear
          fragment.requireView().findViewById(R.id.relLayTextNoResults).setVisibility(View.GONE);

          // make listView appear
          fragment
              .requireView()
              .findViewById(R.id.listAccountsAndHashtags)
              .setVisibility(View.VISIBLE);

          setListAdapter();
        } else {
          // make listView disappear
          fragment
              .requireView()
              .findViewById(R.id.listAccountsAndHashtags)
              .setVisibility(View.GONE);

          // set no results text
          setNoResultsText();
        }
      } catch (Exception e) {
        Log.d("SearchFragment", Log.getStackTraceString(e));
      }
    }

    /** Sets the adapter for listAccounts (for CustomListAdapterAccount) */
    private void setListAdapter() {
      if (isCancelled()) return;

      final SearchFragment fragment = fragmentReference.get();
      if (fragment == null) return;

      try {
        ListAdapterSearch adapter =
            new ListAdapterSearch(
                fragment.getActivity(),
                R.layout.list_item_search,
                fragment.listObjects,
                false,
                null);
        fragment.listView.setAdapter(adapter);
      } catch (NullPointerException e) {
        Log.d("SearchFragment", Log.getStackTraceString(e));
      }
    }

    /** Sets the text "no results" for search */
    private void setNoResultsText() {
      if (isCancelled()) return;

      final SearchFragment fragment = fragmentReference.get();
      if (fragment == null) return;

      RelativeLayout relativeLayout = fragment.requireView().findViewById(R.id.relLayTextNoResults);
      relativeLayout.setVisibility(View.VISIBLE);
      TextView textNoAccounts = fragment.requireView().findViewById(R.id.textNoResults);
      textNoAccounts.setText(fragment.getResources().getString(R.string.no_search_results));
      textNoAccounts.setVisibility(View.VISIBLE);
    }
  }
}
