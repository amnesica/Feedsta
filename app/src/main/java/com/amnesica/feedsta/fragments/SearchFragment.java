package com.amnesica.feedsta.fragments;

import static android.view.View.GONE;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.amnesica.feedsta.Account;
import com.amnesica.feedsta.R;
import com.amnesica.feedsta.adapter.ListAdapterSearch;
import com.amnesica.feedsta.helper.Error;
import com.amnesica.feedsta.helper.FragmentHelper;
import com.amnesica.feedsta.helper.NetworkHandler;
import com.amnesica.feedsta.models.Hashtag;
import com.amnesica.feedsta.models.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Fragment for searching accounts
 */
@SuppressWarnings("deprecation")
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        // listener to get inputString and start search
        setOnEditorActionListenerInputField();

        // listener to get listView element and show profile page
        setItemClickListenerListView();
    }

    /**
     * Sets up listener to get inputString and start search
     */
    private void setOnEditorActionListenerInputField() {
        // when clicking on search key in keyboard start getAccountsAndHashtags search
        textInputField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
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

                    // set progressBar
                    progressBar = requireView().findViewById(R.id.progressBarSearch);
                    progressBar.setProgress(0);

                    // validate string and start getAccountsAndHashtags
                    if (inputStringIsValid(inputString)) {
                        new CheckConnectionAndStartSearch(SearchFragment.this).execute();
                        // clear textField
                        textInputField.setText(null);

                        // hide keyboard
                        InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        assert imm != null;
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }

                    return true;
                }
                return false;
            }
        });
    }

    /**
     * Sets up listener to get listView element and show profile page
     */
    private void setItemClickListenerListView() {
        // get list item in list view
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
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
            }
        });
    }

    /**
     * Starts new hashtagFragment
     *
     * @param hashtag hashtag
     */
    private void goToHashtagPageOfHashtag(Hashtag hashtag) {
        // new HashtagFragment
        HashtagFragment hashtagfragment = HashtagFragment.newInstance(hashtag);

        // add fragment to container
        FragmentHelper.addFragmentToContainer(hashtagfragment, requireActivity().getSupportFragmentManager());
    }

    /**
     * Starts new ProfileFragment
     *
     * @param account account
     */
    private void goToProfilePageOfAccount(Account account) {
        // new profileFragment
        ProfileFragment profileFragment = ProfileFragment.newInstance(account);

        // add fragment to container
        FragmentHelper.addFragmentToContainer(profileFragment, requireActivity().getSupportFragmentManager());
    }

    /**
     * Hides the progressBar
     */
    private void hideProgressBar() {
        try {
            requireActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressBar.setVisibility(GONE);
                }
            });
        } catch (IllegalStateException e) {
            Log.d("SearchFragment", Log.getStackTraceString(e));
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

    /**
     * Eliminates whitespaces in inputString
     */
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

    /**
     * Clear objects list to show new list items
     */
    private void clearListsForRefresh() {
        if (listObjects != null) {
            listObjects.clear();
            listObjects = null;
        }
    }

    /**
     * Validates input string of inputField
     *
     * @param inputString name of account
     * @return string is valid
     */
    private boolean inputStringIsValid(String inputString) {
        return inputString != null && !inputString.isEmpty();
    }

    /**
     * Checks internet connection and notifies user if there is no connection
     */
    @SuppressWarnings("CanBeFinal")
    private static class CheckConnectionAndStartSearch extends AsyncTask<Void, Void, Void> {

        private final WeakReference<SearchFragment> fragmentReference;
        boolean isInternetAvailable = false;

        // constructor
        CheckConnectionAndStartSearch(SearchFragment context) {
            fragmentReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if (!isCancelled()) {
                isInternetAvailable = NetworkHandler.isInternetAvailable();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (!isCancelled()) {
                //get reference from fragment
                final SearchFragment fragment = fragmentReference.get();
                if (fragment != null) {
                    if (isInternetAvailable) {
                        new GetAccountsAndHashtags(fragment).execute();
                    } else {
                        FragmentHelper.notifyUserOfProblem(fragment, Error.NO_INTERNET_CONNECTION);
                    }
                }
            }
        }
    }

    /**
     * Class to get accounts from Instagram
     */
    @SuppressWarnings({"CanBeFinal", "deprecation"})
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
            if (!isCancelled()) {
                // get reference from fragment
                final SearchFragment fragment = fragmentReference.get();
                if (fragment != null) {
                    // make progressBar visible
                    fragment.progressBar.setVisibility(View.VISIBLE);
                }
            }
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            if (!isCancelled()) {
                sh = new NetworkHandler();
                // get reference from fragment
                SearchFragment fragment = fragmentReference.get();
                if (fragment != null) {
                    // make valid URL from inputAccount
                    makeValidURLs(fragment.inputString);

                    // fetch data of url
                    fetchDataOfUrl(url);
                }
            }
            return null;
        }

        /**
         * Makes valid URLs from input
         *
         * @param inputString String
         */
        private void makeValidURLs(String inputString) {
            if (!isCancelled()) {
                String urlAddress = "https://www.instagram.com/web/search/topsearch/?context=blended&query=" + inputString;
                url = new URL(urlAddress, inputString, null);
            }
        }

        /**
         * Fetches data from URL
         *
         * @param url url to fetch from
         */
        void fetchDataOfUrl(URL url) {
            if (!isCancelled()) {
                //get reference from fragment
                final SearchFragment fragment = fragmentReference.get();
                if (fragment != null) {
                    //get json string from url
                    String jsonStr = sh.makeServiceCall(url.url, SearchFragment.class.getSimpleName());

                    //fetch site data
                    if (jsonStr != null) {

                        //validate jsonStr
                        if (!FragmentHelper.checkIfJsonStrIsValid(jsonStr, fragment)) {
                            //stop fetching
                            return;
                        }

                        //file overall as json object
                        JSONObject jsonObj = null;
                        try {
                            jsonObj = new JSONObject(jsonStr);
                        } catch (JSONException e) {
                            Log.d("SearchFragment", Log.getStackTraceString(e));
                        }

                        if (jsonObj != null) {
                            //fetch site data for hashtags
                            fetchDataOfUrlForHashtag(jsonObj);

                            //fetch site data for accounts
                            fetchDataOfUrlForAccount(jsonObj);
                        }
                    } else {
                        if (!NetworkHandler.isInternetAvailable()) {
                            FragmentHelper.notifyUserOfProblem(fragment, Error.NO_INTERNET_CONNECTION);
                        } else { //connected with internet -> something else is problem
                            FragmentHelper.notifyUserOfProblem(fragment, Error.SOMETHINGS_WRONG);
                        }
                    }
                }
            }
        }

        /**
         * Fetch account data from JSONObject
         *
         * @param jsonObj JSONObject
         */
        private void fetchDataOfUrlForAccount(JSONObject jsonObj) {
            if (!isCancelled()) {
                //get reference from fragment
                final SearchFragment fragment = fragmentReference.get();
                if (fragment != null) {
                    try {
                        //getting through overall structure
                        JSONArray users = jsonObj.getJSONArray("users");

                        //Fetch and create user and add to accounts list
                        fetchUserOfUrl(users);
                    } catch (final JSONException e) {
                        if (!NetworkHandler.isInternetAvailable()) {
                            FragmentHelper.notifyUserOfProblem(fragment, Error.NO_INTERNET_CONNECTION);
                        } else { //connected with internet -> something else is problem
                            FragmentHelper.notifyUserOfProblem(fragment, Error.SOMETHINGS_WRONG);
                        }
                    }
                }
            }
        }

        /**
         * Fetch hashtag data from JSONObject
         *
         * @param jsonObj JSONObject
         */
        private void fetchDataOfUrlForHashtag(JSONObject jsonObj) {
            if (!isCancelled()) {
                //get reference from fragment
                final SearchFragment fragment = fragmentReference.get();
                if (fragment != null) {
                    try {
                        //getting through overall structure
                        JSONArray hashtagsJSONArray = jsonObj.getJSONArray("hashtags");

                        //Fetch and create hashtag and add to hashtag list
                        fetchHashtagOfUrl(hashtagsJSONArray);
                    } catch (final JSONException e) {
                        if (!NetworkHandler.isInternetAvailable()) {
                            FragmentHelper.notifyUserOfProblem(fragment, Error.NO_INTERNET_CONNECTION);
                        } else { //connected with internet -> something else is problem
                            FragmentHelper.notifyUserOfProblem(fragment, Error.SOMETHINGS_WRONG);
                        }
                    }
                }
            }
        }

        /**
         * Fetches user of url and creates new account
         *
         * @param users JSONArray
         */
        private void fetchUserOfUrl(JSONArray users) {
            if (!isCancelled()) {
                //get reference from fragment
                final SearchFragment fragment = fragmentReference.get();
                if (fragment != null) {
                    if (fragment.listObjects == null) {
                        fragment.listObjects = new ArrayList<>();
                    }

                    for (int i = 0; i < users.length(); i++) {
                        try {
                            //get user
                            JSONObject userWithPosition = users.getJSONObject(i);
                            JSONObject user = userWithPosition.getJSONObject("user");

                            //create account
                            Account account = new Account(
                                    user.getString("profile_pic_url"),
                                    user.getString("username"),
                                    user.getString("full_name"),
                                    user.getBoolean("is_private"),
                                    user.getString("pk"), //pk is id
                                    null);

                            //add verified status here
                            account.setIs_verified(user.getBoolean("is_verified"));

                            //Add account to list
                            fragment.listObjects.add(account);
                        } catch (JSONException e) {
                            Log.d("SearchFragment", Log.getStackTraceString(e));
                            if (!NetworkHandler.isInternetAvailable()) {
                                FragmentHelper.notifyUserOfProblem(fragment, Error.NO_INTERNET_CONNECTION);
                            } else { //connected with internet -> something else is problem
                                FragmentHelper.notifyUserOfProblem(fragment, Error.SOMETHINGS_WRONG);
                            }
                        }
                    }
                }
            }
        }

        /**
         * Fetches hashtag of url and creates new hashtag
         *
         * @param hashtagsJSONArray JSONArray
         */
        private void fetchHashtagOfUrl(JSONArray hashtagsJSONArray) {

            if (!isCancelled()) {
                //get reference from fragment
                final SearchFragment fragment = fragmentReference.get();
                if (fragment != null) {
                    if (fragment.listObjects == null) {
                        fragment.listObjects = new ArrayList<>();
                    }

                    for (int i = 0; i < hashtagsJSONArray.length(); i++) {
                        try {
                            //get hashtag
                            JSONObject hashtagWithPosition = hashtagsJSONArray.getJSONObject(i);
                            JSONObject hashtagObj = hashtagWithPosition.getJSONObject("hashtag");

                            //create hashtag
                            Hashtag hashtag = new Hashtag(
                                    hashtagObj.getString("name"),
                                    hashtagObj.getInt("id"),
                                    hashtagObj.getInt("media_count"),
                                    hashtagObj.getString("profile_pic_url"),
                                    hashtagObj.getString("search_result_subtitle"));

                            //Add hashtag to list
                            fragment.listObjects.add(hashtag);
                        } catch (JSONException e) {
                            Log.d("SearchFragment", Log.getStackTraceString(e));
                            if (!NetworkHandler.isInternetAvailable()) {
                                FragmentHelper.notifyUserOfProblem(fragment, Error.NO_INTERNET_CONNECTION);
                            } else { //connected with internet -> something else is problem
                                FragmentHelper.notifyUserOfProblem(fragment, Error.SOMETHINGS_WRONG);
                            }
                        }
                    }
                }
            }
        }


        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if (!isCancelled()) {
                //get reference from fragment
                final SearchFragment fragment = fragmentReference.get();
                if (fragment != null) {

                    //hide progressBar
                    fragment.hideProgressBar();

                    try {
                        if (fragment.listObjects != null && !fragment.listObjects.isEmpty()) {
                            //make no results text disappear
                            fragment.requireView().findViewById(R.id.relLayTextNoResults).setVisibility(View.GONE);

                            //make listView appear
                            fragment.requireView().findViewById(R.id.listAccountsAndHashtags).setVisibility(View.VISIBLE);

                            //set adapter etc.
                            setListAdapter();
                        } else {
                            //make listView disappear
                            fragment.requireView().findViewById(R.id.listAccountsAndHashtags).setVisibility(View.GONE);

                            //set no results text
                            setNoResultsText();
                        }
                    } catch (Exception e) {
                        Log.d("SearchFragment", Log.getStackTraceString(e));
                    }
                }
            }
        }

        /**
         * Sets the adapter for listAccounts (for CustomListAdapterAccount)
         */
        private void setListAdapter() {
            if (!isCancelled()) {
                final SearchFragment fragment = fragmentReference.get();
                if (fragment != null) {
                    try {
                        ListAdapterSearch adapter = new ListAdapterSearch(
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
            }
        }

        /**
         * Sets the text "No Results"
         */
        private void setNoResultsText() {
            if (!isCancelled()) {
                final SearchFragment fragment = fragmentReference.get();
                if (fragment != null) {
                    RelativeLayout relativeLayout = fragment.requireView().findViewById(R.id.relLayTextNoResults);
                    relativeLayout.setVisibility(View.VISIBLE);
                    TextView textNoAccounts = fragment.requireView().findViewById(R.id.textNoResults);
                    textNoAccounts.setText(fragment.getResources().getString(R.string.no_search_results));
                    textNoAccounts.setVisibility(View.VISIBLE);
                }
            }
        }
    }
}