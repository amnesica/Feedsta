package com.amnesica.feedsta.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.amnesica.feedsta.R;
import com.amnesica.feedsta.helper.FragmentHelper;

import java.lang.ref.WeakReference;

/**
 * Fragment is the main container for the settings fragment and holds the toolbar. Hint: otherwise toolbar
 * cannot be shown!
 */
public class SettingsHolderFragment extends Fragment {

    public SettingsHolderFragment() {
        // empty constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings_holder, container, false);

        setupToolbar(view);

        // set prefs content as the main content
        assert getFragmentManager() != null;
        getFragmentManager().beginTransaction().replace(R.id.pref_content, new SettingsFragment()).commit();

        return view;
    }

    /**
     * Sets up the toolbar with menu
     *
     * @param view View
     */
    private void setupToolbar(View view) {
        if (view == null) return;

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setTitle(getResources().getString(R.string.toolbar_title_settings));
        FragmentHelper.setupToolbarWithBackButton(toolbar, new WeakReference<>(SettingsHolderFragment.this));
        toolbar.inflateMenu(R.menu.menu_main);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.menu_action_followed_accounts) {
                    FragmentHelper.loadAndShowFragment(requireActivity().getSupportFragmentManager()
                                                               .findFragmentByTag(FollowingFragment.class
                                                                                          .getSimpleName()),
                                                       requireActivity().getSupportFragmentManager());
                } else if (item.getItemId() == R.id.menu_action_statistics_dialog) {
                    FragmentHelper.showStatisticsDialog(SettingsHolderFragment.this);
                } else if (item.getItemId() == R.id.menu_info) {
                    FragmentHelper.loadAndShowFragment(requireActivity().getSupportFragmentManager()
                                                               .findFragmentByTag(
                                                                       AboutFragment.class.getSimpleName()),
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
            // Slide down bottom navigation view if necessary
            FragmentHelper.makeBottomNavigationBarInvisible(getActivity());
        }
    }
}
