package com.amnesica.feedsta.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.textview.MaterialTextView;

/**
 * Custom TextView which is expandable/collapsable like the Instagram caption. It shows a clickable
 * "... more" string at the end of the text when it has more than maxLines lines of text. If
 * fulltext is already a SpannableString this string will not get changed in any way.
 *
 * <p>How to use? Just call textView.makeExpandable(text, maxLines); in your code.
 *
 * <p>Code is based on sources: https://gist.github.com/vedant1811/21f536dcc94dd7f00801,
 * https://stackoverflow.com/a/22751079
 */
public class ExpandableTextView extends MaterialTextView {

  private static final String ELLIPSIZE = "... ";
  private static final String MORE = "more";
  private static final String LESS = "less";

  private SpannableStringBuilder fullText;
  private int maxLines;
  private boolean textHasAlreadyBeenChanged = false;

  public ExpandableTextView(Context context) {
    super(context);
  }

  public ExpandableTextView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public ExpandableTextView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public void makeExpandable(@NonNull final SpannableStringBuilder fullText, final int maxLines) {
    this.fullText = fullText;
    this.maxLines = maxLines;

    // reinitialize boolean when view gets reattached in recyclerview so that changes can be made
    // again
    textHasAlreadyBeenChanged = false;

    // set text here so method onTextChanged is called and getLayout does not return null
    setText(fullText, BufferType.SPANNABLE);

    // call method below so texts with less than maxLines lines are clickable
    setMovementMethod(LinkMovementMethod.getInstance());
  }

  @Override
  protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
    // set boolean to stop undoing changes when text is changed in methods below
    if (textHasAlreadyBeenChanged) return;

    post(
        () -> {
          if (getLineCount() > maxLines) {
            setMovementMethod(LinkMovementMethod.getInstance());
            truncateText();
          }
        });
  }

  /** Truncates text to maxLines lines and appends a clickable "... more" string at the end */
  private void truncateText() {
    // check needed so NullPointerException is not thrown
    if (getLayout() == null) return;

    // get insert position for "... more" text
    int lineEndIndex = getLayout().getLineEnd(maxLines - 1);
    final int insertIndex = lineEndIndex - (ELLIPSIZE.length() + MORE.length() + 1);

    // build clickable "... more" text
    CharSequence moreText = ELLIPSIZE + MORE;
    SpannableStringBuilder builderMoreText = new SpannableStringBuilder(moreText);
    builderMoreText.setSpan(
        new ClickableSpan() {
          @SuppressLint("ResourceType")
          @Override
          public void updateDrawState(@NonNull TextPaint ds) {
            super.updateDrawState(ds);
            // set color to textColorSecondary
            int secondaryColor =
                MaterialColors.getColor(
                    getContext(), android.R.attr.textColorSecondary, Color.BLUE);
            ds.setColor(secondaryColor);

            // remove underline and set alpha
            ds.setUnderlineText(false);
            ds.setAlpha(180);
          }

          @Override
          public void onClick(View widget) {
            expandText();
          }
        },
        ELLIPSIZE.length(),
        moreText.length(),
        0);

    // append "... more" text to truncated fulltext
    CharSequence newText = fullText.subSequence(0, insertIndex);
    SpannableStringBuilder builderNewText = new SpannableStringBuilder(newText);
    builderNewText.append(builderMoreText);
    setText(builderNewText, BufferType.SPANNABLE);

    // set boolean to true so that changes are not made again
    textHasAlreadyBeenChanged = true;
  }

  /** Shows full text and appends a clickable "... less" string at the end */
  private void expandText() {
    // build clickable "less" text
    CharSequence lessText = LESS;
    SpannableStringBuilder builderLessText = new SpannableStringBuilder(lessText);
    builderLessText.setSpan(
        new ClickableSpan() {
          @SuppressLint("ResourceType")
          @Override
          public void updateDrawState(@NonNull TextPaint ds) {
            super.updateDrawState(ds);
            // set color to textColorSecondary
            int secondaryColor =
                MaterialColors.getColor(
                    getContext(), android.R.attr.textColorSecondary, Color.BLUE);
            ds.setColor(secondaryColor);

            // remove underline and set alpha
            ds.setUnderlineText(false);
            ds.setAlpha(180);
          }

          @Override
          public void onClick(View widget) {
            truncateText();
          }
        },
        0,
        lessText.length(),
        0);

    // append "less" text to expanded fulltext
    SpannableStringBuilder builderNewText = new SpannableStringBuilder(fullText);
    builderNewText.append(builderLessText);
    setText(builderNewText, BufferType.SPANNABLE);
  }
}
