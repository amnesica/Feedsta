package com.amnesica.feedsta.helper;

import static com.amnesica.feedsta.helper.StaticIdentifier.debugModeEnabled;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;

/** Helper for http and https connections */
public class NetworkHandler {

  private InputStream inputStream = null;
  private HttpURLConnection httpURLConnection = null;
  private PropertiesHelper propertiesHelper = null;

  public NetworkHandler() {
    // empty constructor
  }

  /**
   * Checks internet connection with a site with high availability
   *
   * @return boolean
   */
  public static boolean isInternetAvailable() {
    try {
      InetAddress ipAddress = InetAddress.getByName(StaticIdentifier.siteToCheckInternet);
      return !ipAddress.equals("");

    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Makes service call to requestUrl
   *
   * @param requestUrl String
   * @param className String
   * @return String
   */
  public String makeServiceCall(String requestUrl, String className) {
    String response = null;
    inputStream = null;
    httpURLConnection = null;

    try {
      if (requestUrl != null) {
        URL url;

        if (debugModeEnabled) {
          url = handleDebugMode(requestUrl);
        } else {
          url = new URL(requestUrl);
        }

        httpURLConnection = (HttpURLConnection) url.openConnection();
        httpURLConnection.setConnectTimeout(5000);
        httpURLConnection.setRequestMethod("GET");

        // read the response
        inputStream = new BufferedInputStream(httpURLConnection.getInputStream());
        response = convertStreamToString(inputStream);
      }
    } catch (Exception e) {
      if (className != null) {
        Log.d(
            NetworkHandler.class.getSimpleName(),
            "NetworkHandler by " + className + ": response was null");
      }
    } finally {
      closeConnectionsAndBuffers();
    }
    return response;
  }

  @NonNull
  private URL handleDebugMode(String requestUrl) throws Exception {
    URL url;

    if (propertiesHelper == null) {
      propertiesHelper = new PropertiesHelper();
    }

    // get server address for debugging
    final String debugServerAddressAndPort = propertiesHelper.getDebugServerAddressWithPort();
    if (debugServerAddressAndPort == null)
      throw new Exception("debugServerAddressAndPort is null in debug mode");

    // change request url with debug server address
    url =
        new URL(
            requestUrl.replace("https://www.instagram.com", "http://" + debugServerAddressAndPort));

    Log.d(NetworkHandler.class.getSimpleName(), "NetworkHandler: debug mode used: " + url);
    return url;
  }

  /** Closes the InputStream and the HttpURLConnection */
  public void closeConnectionsAndBuffers() {
    try {
      if (inputStream != null) {
        inputStream.close();
      }
      if (httpURLConnection != null) {
        httpURLConnection.disconnect();
      }
    } catch (Exception e) {
      Log.d(NetworkHandler.class.getSimpleName(), Log.getStackTraceString(e));
    }
  }

  /**
   * Converts InputStream to String
   *
   * @param inputStream InputStream
   * @return String
   * @throws IOException IOException
   */
  private String convertStreamToString(InputStream inputStream) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
    StringBuilder sb = new StringBuilder();
    String line;
    try {
      while ((line = reader.readLine()) != null) {
        sb.append(line).append('\n');
      }
    } catch (IOException e) {
      Log.d(NetworkHandler.class.getSimpleName(), Log.getStackTraceString(e));
    } finally {
      inputStream.close();
    }
    return sb.toString();
  }
}
