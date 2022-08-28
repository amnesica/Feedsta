package com.amnesica.feedsta.helper;

import android.content.Context;
import android.util.Base64;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.amnesica.feedsta.fragments.FullscreenImagePostFragment;
import com.amnesica.feedsta.fragments.FullscreenProfileImageFragment;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

public class ImageHelper {
  /* Loads image with glide (note: error retries loading the image from the url) */
  public static void loadImageWithGlide(
      final ImageView imageView, final String url, int idErrorImage, final Context context) {
    Glide.with(context)
        .load(url)
        .error(ContextCompat.getDrawable(context, idErrorImage))
        .fallback(ContextCompat.getDrawable(context, idErrorImage)) // if load was null
        .diskCacheStrategy(DiskCacheStrategy.NONE)
        .skipMemoryCache(true)
        .dontAnimate()
        .into(imageView);
  }

  public static void loadBase64ImageWithGlide(
      final ImageView imageView,
      final String base64Image,
      int idErrorImage,
      final Context context) {
    Glide.with(context)
        .asBitmap()
        .load(Base64.decode(base64Image, Base64.DEFAULT))
        .error(ContextCompat.getDrawable(context, idErrorImage))
        .dontAnimate()
        .diskCacheStrategy(DiskCacheStrategy.NONE)
        .skipMemoryCache(true)
        .into(imageView);
  }

  public static void setupListenerToShowFullscreenProfileImage(
      final AppCompatActivity context,
      final String imageUrlOrBase64String,
      final ImageView imageView) {
    imageView.setOnClickListener(
        view -> {
          FullscreenProfileImageFragment fullscreenProfileImageFragment;
          fullscreenProfileImageFragment =
              FullscreenProfileImageFragment.newInstance(imageUrlOrBase64String);

          FragmentHelper.addFragmentToContainer(
              fullscreenProfileImageFragment, context.getSupportFragmentManager());
        });
  }

  public static void setupListenerToShowFullscreenPostImage(
      final AppCompatActivity context,
      final String imageUrlOrBase64String,
      final ImageView imageView) {
    imageView.setOnClickListener(
        view -> {
          FullscreenImagePostFragment fullscreenImagePostFragment;
          fullscreenImagePostFragment =
              FullscreenImagePostFragment.newInstance(imageUrlOrBase64String);

          FragmentHelper.addFragmentToContainer(
              fullscreenImagePostFragment, context.getSupportFragmentManager());
        });
  }
}
