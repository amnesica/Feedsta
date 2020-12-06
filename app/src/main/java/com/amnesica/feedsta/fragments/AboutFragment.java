package com.amnesica.feedsta.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.amnesica.feedsta.BuildConfig;
import com.amnesica.feedsta.R;
import com.amnesica.feedsta.helper.FragmentHelper;

import java.lang.ref.WeakReference;

/**
 * Fragment for displaying info about the application
 */
@SuppressWarnings("deprecation")
public class AboutFragment extends Fragment {

    public AboutFragment() {
        //  Required empty public constructor
    }

    /**
     * Returns the name of the application
     *
     * @param context context
     * @return name of application
     */
    private static String getApplicationName(Context context) {
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        int stringId = applicationInfo.labelRes;
        return stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() : context.getString(stringId);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_about, container, false);

        // setup toolbar
        Toolbar toolbar = v.findViewById(R.id.toolbar);
        setupToolbar(toolbar);

        // set app icon image
        ImageView imgAppInfo = v.findViewById(R.id.imageAppInfo);
        imgAppInfo.setImageDrawable(getResources().getDrawable(R.mipmap.ic_launcher_feedsta_round));

        // set text 'made with love'
        TextView textMadeWithLove = v.findViewById(R.id.textMadeWithLove);
        textMadeWithLove.setText(getResources().getString(R.string.made_with_love_from_hh));

        // set text to rate the app
        TextView textRatingInfo = v.findViewById(R.id.textAppInfoRating);
        String stringToUnderline = "Click here to rate the app";
        SpannableString content = new SpannableString(stringToUnderline);
        content.setSpan(new UnderlineSpan(), 0, stringToUnderline.length(), 0);
        textRatingInfo.setText(content);
        textRatingInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentHelper.rateApp(requireContext());
            }
        });


        // set app name
        if (getContext() != null) {
            String applicationName = getApplicationName(requireContext());
            TextView textAppName = v.findViewById(R.id.textAppName);
            textAppName.setText(applicationName);

            // set app info text
            TextView textAppInfo = v.findViewById(R.id.textAppInfo);
            textAppInfo.setText(requireContext().getResources().getString(R.string.no_relationship_to_instagram, applicationName));
        }

        // set app version text
        TextView textAppVersion = v.findViewById(R.id.textAppVersion);
        textAppVersion.setText(getAppVersion());

        return v;
    }

    /**
     * Sets up the toolbar with NavigationOnClickListener and MenuItemClickListener
     *
     * @param toolbar toolbar
     */
    private void setupToolbar(Toolbar toolbar) {
        toolbar.setTitle(getResources().getString(R.string.toolbar_title_about));
        FragmentHelper.setupToolbarWithBackButton(toolbar, new WeakReference<>(AboutFragment.this));
        toolbar.inflateMenu(R.menu.menu_main);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.menu_action_followed_accounts) {
                    FragmentHelper.loadAndShowFragment(requireActivity().getSupportFragmentManager().findFragmentByTag(FollowingFragment.class.getSimpleName()),
                            requireActivity().getSupportFragmentManager());
                } else if (item.getItemId() == R.id.menu_action_statistics_dialog) {
                    FragmentHelper.showStatisticsDialog(AboutFragment.this);
                } else if (item.getItemId() == R.id.menu_settings) {
                    FragmentHelper.loadAndShowFragment(requireActivity().getSupportFragmentManager().findFragmentByTag(SettingsHolderFragment.class.getSimpleName()),
                            requireActivity().getSupportFragmentManager());
                } else if (item.getItemId() == R.id.menu_exit) {
                    requireActivity().finish();
                }
                return false;
            }
        });
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        // set highlighted item on nav bar to "feed"
        if (!hidden) {
            FragmentHelper.setBottomNavViewSelectElem(getActivity(), R.id.navigation_feed);
        }
    }

    /**
     * Returns the version name of the app
     *
     * @return String
     */
    private String getAppVersion() {
        return BuildConfig.VERSION_NAME;
    }
}
