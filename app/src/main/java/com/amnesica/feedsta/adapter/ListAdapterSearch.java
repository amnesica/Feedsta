package com.amnesica.feedsta.adapter;

import android.annotation.SuppressLint;
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

import com.amnesica.feedsta.Account;
import com.amnesica.feedsta.R;
import com.amnesica.feedsta.fragments.FullscreenProfileImageFragment;
import com.amnesica.feedsta.helper.FragmentHelper;
import com.amnesica.feedsta.helper.StorageHelper;
import com.amnesica.feedsta.interfaces.AdapterCallback;
import com.amnesica.feedsta.models.Hashtag;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;

/**
 * Adapter for displaying a list of accounts
 */
@SuppressWarnings({"CanBeFinal", "deprecation"})
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
            this.adapterCallback = adapterCallback;
        }
    }

    @SuppressLint({"InflateParams", "UseCompatLoadingForDrawables"})
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            assert layoutInflater != null;
            convertView = layoutInflater.inflate(R.layout.list_item_search, null, true);
        }

        // if element is account
        if (getItem(position) instanceof Account) {

            // element in listObject is Account
            // get account at current position
            final Account account = (Account) getItem(position);

            // put account profile picture into CircularImageView
            final ImageView imageView = convertView.findViewById(R.id.accountOrHashtagProfilePic);
            assert account != null;

            // load base64 encoded image into view
            if (account.getImageThumbnail() != null) {
                // load image into view
                Glide.with(convertView)
                        .asBitmap()
                        .load(Base64.decode(account.getImageThumbnail(), Base64.DEFAULT))
                        .error(R.drawable.placeholder_image_post_error)
                        .dontAnimate()
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .into(imageView);
            } else {
                // load image with url into view
                Glide.with(convertView)
                        .load(account.getImageProfilePicUrl())
                        .error(R.drawable.placeholder_image_post_error)
                        .dontAnimate()
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .into(imageView);
            }

            // OnClickListener on profile picture for showing profile picture fullscreen
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!isImageFitToScreen) {

                        FullscreenProfileImageFragment fullscreenProfileImageFragment = null;

                        if (account.getImageThumbnail() != null) {
                            // new fullscreenImageFragment with image profile as string
                            fullscreenProfileImageFragment = FullscreenProfileImageFragment.newInstance(account.getImageThumbnail());
                        } else {
                            // new fullscreenImageFragment with image profile pic as url
                            fullscreenProfileImageFragment = FullscreenProfileImageFragment.newInstance(account.getImageProfilePicUrl());
                        }

                        // add fullscreenImageFragment to FragmentManager
                        FragmentHelper.addFragmentToContainer(fullscreenProfileImageFragment, ((AppCompatActivity) context).getSupportFragmentManager());
                    }
                }
            });

            // set username of account
            TextView username = convertView.findViewById(R.id.textUsernameOrHashtagName);
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

            TextView textFullName = convertView.findViewById(R.id.textFullNameOrResultSubtitle);
            RelativeLayout relLayoutSearchAccount = convertView.findViewById(R.id.relLayoutSearchAccount);

            // for private accounts
            if (account.getIs_private()) {

                // hint for private account
                username.setText(getContext().getResources().getString(R.string.full_name_private_account, account.getUsername()));

                // light grey background for private accounts
                TypedValue typedValue = new TypedValue();
                Resources.Theme theme = context.getTheme();
                theme.resolveAttribute(R.attr.colorVeryDarkGrey, typedValue, true);
                @ColorInt int color = typedValue.data;
                relLayoutSearchAccount.setBackgroundColor(color);

            } else { // public account
                // set fullName of account
                if (account.getFullName() != null) {
                    textFullName.setText(account.getFullName());
                }

                // set background color for public account
                TypedValue typedValue = new TypedValue();
                Resources.Theme theme = context.getTheme();
                theme.resolveAttribute(R.attr.colorPrimarySearchPublic, typedValue, true);
                @ColorInt int color = typedValue.data;
                relLayoutSearchAccount.setBackgroundColor(color);
            }

            // load popup menu if calling fragment is FollowingFragment
            ImageView imageRemoveAccount;
            if (isFollowingFragment) {
                imageRemoveAccount = convertView.findViewById(R.id.imageRemoveAccount);
                imageRemoveAccount.setVisibility(View.VISIBLE);
                imageRemoveAccount.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_more_vert_black_24dp));
                imageRemoveAccount.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        popupMenu = new PopupMenu(context, v);
                        popupMenu.inflate(R.menu.menu_remove_followed_account);
                        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                if (StorageHelper.checkIfAccountOrPostIsInFile(account, StorageHelper.filename_accounts, context)) {

                                    // final confirmation before removing
                                    adapterCallback.removeAccountFromStorage(account);
                                }
                                return false;
                            }
                        });
                        // show popup menu
                        popupMenu.show();
                    }
                });
            } else {
                // make ImageButton for removing account disappear (not needed in normal listView)
                imageRemoveAccount = convertView.findViewById(R.id.imageRemoveAccount);
                imageRemoveAccount.setVisibility(View.GONE);
            }
        } else if (getItem(position) instanceof Hashtag) {

            // get hashtag at current position
            Hashtag hashtag = (Hashtag) getItem(position);

            if (hashtag != null) {
                // set hashtag profile picture
                ImageView imageView = convertView.findViewById(R.id.accountOrHashtagProfilePic);
                Glide.with(convertView)
                        .load(hashtag.getProfile_pic_url())
                        .error(R.drawable.placeholder_image_post_error)
                        .dontAnimate()
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .into(imageView);

                // set hashtag name
                TextView hashtagName = convertView.findViewById(R.id.textUsernameOrHashtagName);
                hashtagName.setText(getContext().getResources().getString(R.string.name_hashtag_with_symbol, hashtag.getName()));

                // set amount of posts that are tagged with hashtag
                TextView textSearchResultSubtitle = convertView.findViewById(R.id.textFullNameOrResultSubtitle);
                textSearchResultSubtitle.setText(hashtag.getSearch_result_subtitle());

                // white background of hashtag (hashtags cannot be private)
                // fixing wrong background color when there are private accounts in list too
                RelativeLayout relLayoutSearchAccount = convertView.findViewById(R.id.relLayoutSearchAccount);
                TypedValue typedValue = new TypedValue();
                Resources.Theme theme = context.getTheme();
                theme.resolveAttribute(R.attr.colorPrimarySearchPublic, typedValue, true);
                @ColorInt int color = typedValue.data;
                relLayoutSearchAccount.setBackgroundColor(color);

                // fixing wrong appearance of verified icon (hashtags cannot be verified)
                ImageView imageViewVerified = convertView.findViewById(R.id.imageVerifiedAccount);
                imageViewVerified.setVisibility(View.INVISIBLE);
            }
        }
        return convertView;
    }
}