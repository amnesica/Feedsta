package com.amnesica.feedsta.asynctasks;

import static androidx.core.content.ContextCompat.getSystemService;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.widget.ImageButton;

import androidx.fragment.app.Fragment;

import com.amnesica.feedsta.R;
import com.amnesica.feedsta.helper.FragmentHelper;
import com.amnesica.feedsta.models.Post;

import java.lang.ref.WeakReference;

public class CopyLink {
  private final Post post;
  private final Fragment callingFragment;
  private final ImageButton buttonCopyLink;

  public CopyLink(Post post, Fragment callingFragment, ImageButton buttonCopyLink) {
    this.post = post;
    this.callingFragment = callingFragment;
    this.buttonCopyLink = buttonCopyLink;
  }

  public void copyLink() {
    String urlToCopy;

    final Fragment fragment = new WeakReference<>(callingFragment).get();
    if (fragment == null) return;

    if (post != null) {
      urlToCopy = createCopyUrlForPost(fragment);

      // copy urlToCopy to clipboard
      ClipboardManager clipboard =
          getSystemService(fragment.requireContext(), ClipboardManager.class);
      ClipData clip = ClipData.newPlainText("urlToCopy", urlToCopy);

      if (clipboard != null) {
        clipboard.setPrimaryClip(clip);

        // make toast that link has been copied
        FragmentHelper.showToast(
            fragment.getResources().getString(R.string.link_copied),
            fragment.requireActivity(),
            fragment.requireContext());

        changeDrawableButtonCopy(fragment);
      }
    }
  }

  private String createCopyUrlForPost(Fragment fragment) {
    if (fragment == null || post.getShortcode() == null) return "";

    if (FragmentHelper.addAdvertisingStringToClipboard(fragment)) {
      return "https://www.instagram.com/p/"
          + post.getShortcode()
          + fragment.getResources().getString(R.string.copy_post_second_part);
    } else {
      return "https://www.instagram.com/p/" + post.getShortcode();
    }
  }

  private void changeDrawableButtonCopy(Fragment fragment) {
    if (fragment == null) return;

    fragment
        .requireActivity()
        .runOnUiThread(
            () -> {
              // change icon to copied
              buttonCopyLink.setBackgroundResource(R.drawable.ic_content_copy_pressed_24dp);
            });
  }
}
