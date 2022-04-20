package com.amnesica.feedsta.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.util.Base64;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.amnesica.feedsta.R;
import com.amnesica.feedsta.fragments.FullscreenProfileImageFragment;
import com.amnesica.feedsta.helper.FragmentHelper;
import com.amnesica.feedsta.helper.StorageHelper;
import com.amnesica.feedsta.interfaces.AdapterCallback;
import com.amnesica.feedsta.models.Account;
import com.amnesica.feedsta.models.Hashtag;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;

/**
 * Adapter for displaying a list of accounts (used in FollowingFragment and SearchFragment)
 */
public class ListAdapterSearch extends ArrayAdapter<Object> {

    private final Context context;
    private final Boolean isFollowingFragment;
    private final boolean isImageFitToScreen = false;
    private PopupMenu popupMenu;
    private AdapterCallback adapterCallback;

    public ListAdapterSearch(Context context, int resource, ArrayList<Object> listObject,
                             Boolean isFollowingFragment, AdapterCallback adapterCallback) {
        super(context, resource, listObject);
        this.context = context;

        this.isFollowingFragment = isFollowingFragment;
        if (this.isFollowingFragment) {
            // adapter callback to remove account from storage
            this.adapterCallback = adapterCallback;
        }
    }

    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(
                    Activity.LAYOUT_INFLATER_SERVICE);
            assert layoutInflater != null;
            convertView = layoutInflater.inflate(R.layout.list_item_search, null, false);
        }

        // if element is account
        if (getItem(position) instanceof Account) {
            setupAccountListItem(convertView, context, position);

        } else if (getItem(position) instanceof Hashtag) {
            setupHashtagListItem(convertView, position);
        }
        return convertView;
    }

    /**
     * Sets up the list item with account information like profile photo and name. If view is called in
     * FollowingFragment also the ImageButton to remove an account has to be initialized
     *
     * @param convertView convertView
     * @param context     Context
     * @param position    int
     */
    private void setupAccountListItem(View convertView, final Context context, final int position) {
        // element in listObject is Account
        // get account at current position
        final Account account = (Account) getItem(position);
        assert account != null;

        // put account profile picture into CircularImageView
        final ImageView imageView = convertView.findViewById(R.id.accountOrHashtagProfilePic);

        // load base64 encoded image into view
        if (account.getImageThumbnail() != null) {
            // load image into view
            Glide.with(convertView).asBitmap().load(
                    Base64.decode(account.getImageThumbnail(), Base64.DEFAULT)).error(
                    ContextCompat.getDrawable(context, R.drawable.placeholder_image_error)).dontAnimate()
                    .diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).into(imageView);
        } else {
            // load image with url into view
            Glide.with(convertView).load(account.getImageProfilePicUrl()).error(
                    ContextCompat.getDrawable(context, R.drawable.placeholder_image_error)).dontAnimate()
                    .diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).into(imageView);
        }

        // set up OnClickListener on profile picture for showing profile picture fullscreen
        setupProfilePictureToFullscreenImage((AppCompatActivity) context, account, imageView);

        // set username of account
        final TextView username = convertView.findViewById(R.id.textUsernameOrHashtagName);
        if (account.getUsername() != null) {
            username.setText(account.getUsername());
        }

        // set verified account badge
        ImageView verifiedImage = convertView.findViewById(R.id.imageVerifiedAccount);
        if (account.getIs_verified()) {
            verifiedImage.setVisibility(View.VISIBLE);
        } else {
            verifiedImage.setVisibility(View.INVISIBLE);
        }

        final TextView textFullName = convertView.findViewById(R.id.textFullNameOrResultSubtitle);
        final RelativeLayout relLayoutSearchAccount = convertView.findViewById(R.id.relLayoutSearchAccount);

        // for private accounts set hint that account is private and
        // set background color to dark grey
        if (account.getIs_private()) {

            // hint for private account
            username.setText(getContext().getResources()
                                     .getString(R.string.full_name_private_account, account.getUsername()));

            // light grey background for private accounts
            relLayoutSearchAccount.setBackgroundColor(getColor(context, R.attr.colorSurfaceVariant));

        } else { // public account
            // set fullName of account
            if (account.getFullName() != null) {
                textFullName.setText(account.getFullName());
            }

            // set background color for public account;
            relLayoutSearchAccount.setBackgroundColor(getColor(context, R.attr.colorPrimary));
        }

        // load popup menu to remove account if calling fragment is FollowingFragment
        ImageView imageRemoveAccount;
        if (isFollowingFragment) {
            setupPopupMenuToRemoveAccount(convertView, context, account);
        } else {
            // make ImageButton for removing account disappear (not needed in normal listView
            // (SearchFragment))
            imageRemoveAccount = convertView.findViewById(R.id.imageRemoveAccount);
            imageRemoveAccount.setVisibility(View.GONE);
        }
    }

    /**
     * Sets up the list item with hashtag information like hashtag name and static profile photo
     *
     * @param position    position
     * @param convertView convertView
     */
    private void setupHashtagListItem(View convertView, final int position) {
        // get hashtag at current position
        Hashtag hashtag = (Hashtag) getItem(position);

        if (hashtag != null) {
            // set hashtag profile picture
            ImageView imageView = convertView.findViewById(R.id.accountOrHashtagProfilePic);
            Glide.with(convertView).load(hashtag.getProfile_pic_url()).error(
                    ContextCompat.getDrawable(context, R.drawable.placeholder_image_error)).dontAnimate()
                    .diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).into(imageView);

            // set hashtag name
            TextView hashtagName = convertView.findViewById(R.id.textUsernameOrHashtagName);
            hashtagName.setText(getContext().getResources()
                                        .getString(R.string.name_hashtag_with_symbol, hashtag.getName()));

            // set amount of posts that are tagged with hashtag
            TextView textSearchResultSubtitle = convertView.findViewById(R.id.textFullNameOrResultSubtitle);
            textSearchResultSubtitle.setText(hashtag.getSearch_result_subtitle());

            // white background of hashtag (hashtags cannot be private)
            // fixing wrong background color when there are private accounts in list too
            RelativeLayout relLayoutSearchAccount = convertView.findViewById(R.id.relLayoutSearchAccount);
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = context.getTheme();
            theme.resolveAttribute(R.attr.colorPrimary, typedValue, true);
            @ColorInt int color = typedValue.data;
            relLayoutSearchAccount.setBackgroundColor(color);

            // fixing wrong appearance of verified icon (hashtags cannot be verified)
            ImageView imageViewVerified = convertView.findViewById(R.id.imageVerifiedAccount);
            imageViewVerified.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Sets up the OnClickListener to view the profile picture of an account in fullscreen
     *
     * @param context   AppCompatActivity
     * @param account   Account
     * @param imageView ImageView
     */
    private void setupProfilePictureToFullscreenImage(final AppCompatActivity context, final Account account,
                                                      ImageView imageView) {
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isImageFitToScreen) {
                    FullscreenProfileImageFragment fullscreenProfileImageFragment;

                    if (account.getImageThumbnail() != null) {
                        // new fullscreenImageFragment with image profile as string
                        fullscreenProfileImageFragment = FullscreenProfileImageFragment.newInstance(
                                account.getImageThumbnail());
                    } else {
                        // new fullscreenImageFragment with image profile pic as url
                        fullscreenProfileImageFragment = FullscreenProfileImageFragment.newInstance(
                                account.getImageProfilePicUrl());
                    }

                    // add fullscreenImageFragment to FragmentManager
                    FragmentHelper.addFragmentToContainer(fullscreenProfileImageFragment,
                                                          context.getSupportFragmentManager());
                }
            }
        });
    }

    /**
     * Sets up the popup menu to remove an account from the storage (only needed when view is called in
     * FollowingFragment)
     *
     * @param convertView View
     * @param context     Context
     * @param account     Account
     */
    private void setupPopupMenuToRemoveAccount(View convertView, final Context context,
                                               final Account account) {
        ImageView imageRemoveAccount;
        imageRemoveAccount = convertView.findViewById(R.id.imageRemoveAccount);
        imageRemoveAccount.setVisibility(View.VISIBLE);
        imageRemoveAccount.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_more_vert_24dp));
        imageRemoveAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupMenu = new PopupMenu(context, v);
                popupMenu.inflate(R.menu.menu_remove_followed_account);
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (StorageHelper.checkIfAccountOrPostIsInFile(account,
                                                                       StorageHelper.filename_accounts,
                                                                       context)) {

                            // final confirmation before removing with dialog
                            adapterCallback.removeAccountFromStorage(account);
                        }
                        return false;
                    }
                });
                // show popup menu
                popupMenu.show();
            }
        });
    }

    /**
     * Returns the color for a resourceId with respect to the theme
     *
     * @param context    Context
     * @param resourceId resourceId
     * @return int
     */
    private int getColor(final Context context, final int resourceId) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(resourceId, typedValue, true);
        return typedValue.data;
    }
}