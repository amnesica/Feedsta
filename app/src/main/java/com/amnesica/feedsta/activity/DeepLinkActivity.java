package com.amnesica.feedsta.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.amnesica.feedsta.R;

/**
 * When a link with prefix "www.instagram.com" is opened this class serves as an entry point to give
 * MainActivity the intent data. Class is needed to have only one Feedsta instance existing,
 * otherwise every deep link would open a new Feedsta instance. When app is used as usual this class
 * is not called
 */
public class DeepLinkActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_deep_link);

    Intent intent = getIntent();
    Uri intentUri = intent.getData();
    Intent newIntent = new Intent(this, MainActivity.class);
    newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
    newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    newIntent.setData(intentUri);
    newIntent.setAction(Long.toString(System.currentTimeMillis()));

    startActivity(newIntent);
    finish();
  }
}
