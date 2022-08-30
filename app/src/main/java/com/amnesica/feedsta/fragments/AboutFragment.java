package com.amnesica.feedsta.fragments;

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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.amnesica.feedsta.BuildConfig;
import com.amnesica.feedsta.R;
import com.amnesica.feedsta.fragments.settings.SettingsHolderFragment;
import com.amnesica.feedsta.helper.FragmentHelper;
import com.amnesica.feedsta.helper.StorageHelper;

import java.lang.ref.WeakReference;

/** Fragment for displaying info about the application */
public class AboutFragment extends Fragment {

  public AboutFragment() {
    //  Required empty public constructor
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    // inflate the layout for this fragment
    View view = inflater.inflate(R.layout.fragment_about, container, false);

    setupToolbar(view);

    // set app icon image
    ImageView imgAppInfo = view.findViewById(R.id.imageAppInfo);
    imgAppInfo.setImageDrawable(
        ContextCompat.getDrawable(requireActivity(), R.mipmap.ic_launcher_feedsta_round));

    // set text "made with love"
    TextView textMadeWithLove = view.findViewById(R.id.textMadeWithLove);
    if (FragmentHelper.getThemeIsDarkTheme(requireContext())) {
      textMadeWithLove.setText(
          getResources().getString(R.string.made_with_love_from_hh_dark_theme));
    } else {
      textMadeWithLove.setText(
          getResources().getString(R.string.made_with_love_from_hh_light_theme));
    }

    // set text to visit Github
    TextView textRatingInfo = view.findViewById(R.id.textAppGoToGithub);
    String stringToUnderline = getString(R.string.click_here_to_go_to_github);
    SpannableString content = new SpannableString(stringToUnderline);
    content.setSpan(new UnderlineSpan(), 0, stringToUnderline.length(), 0);
    textRatingInfo.setText(content);
    textRatingInfo.setOnClickListener(
        view1 -> FragmentHelper.openProjectOnGithub(requireContext()));

    // set app name
    if (getContext() != null) {
      String applicationName = StorageHelper.getApplicationName(requireContext());
      TextView textAppName = view.findViewById(R.id.textAppName);
      textAppName.setText(applicationName);

      // set app info text
      TextView textAppInfo = view.findViewById(R.id.textAppInfo);
      textAppInfo.setText(
          requireContext()
              .getResources()
              .getString(R.string.no_relationship_to_instagram, applicationName));
    }

    // set app version text
    TextView textAppVersion = view.findViewById(R.id.textAppVersion);
    textAppVersion.setText(BuildConfig.VERSION_NAME);

    return view;
  }

  /**
   * Sets up the toolbar with MenuItemClickListener for menu
   *
   * @param view View
   */
  private void setupToolbar(final View view) {
    if (view == null) return;

    Toolbar toolbar = view.findViewById(R.id.toolbar);
    toolbar.setTitle(getResources().getString(R.string.toolbar_title_about));
    FragmentHelper.setupToolbarWithBackButton(toolbar, new WeakReference<>(AboutFragment.this));
    toolbar.inflateMenu(R.menu.menu_main);
    toolbar.setOnMenuItemClickListener(
        new Toolbar.OnMenuItemClickListener() {
          @Override
          public boolean onMenuItemClick(MenuItem item) {
            if (item.getItemId() == R.id.menu_action_followed_accounts) {
              FragmentHelper.loadAndShowFragment(
                  requireActivity()
                      .getSupportFragmentManager()
                      .findFragmentByTag(FollowingFragment.class.getSimpleName()),
                  requireActivity().getSupportFragmentManager());
            } else if (item.getItemId() == R.id.menu_action_statistics_dialog) {
              FragmentHelper.showStatisticsDialog(AboutFragment.this);
            } else if (item.getItemId() == R.id.menu_settings) {
              FragmentHelper.loadAndShowFragment(
                  requireActivity()
                      .getSupportFragmentManager()
                      .findFragmentByTag(SettingsHolderFragment.class.getSimpleName()),
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

    if (!hidden) {
      // set highlighted item on nav bar to "feed"
      FragmentHelper.setBottomNavViewSelectElem(getActivity(), R.id.navigation_feed);

      // Slide down bottom navigation view if necessary
      FragmentHelper.slideDownBottomNavigationBar(getActivity());
    }
  }
}
