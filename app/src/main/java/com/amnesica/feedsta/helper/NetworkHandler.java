package com.amnesica.feedsta.helper;

import android.annotation.SuppressLint;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;

/**
 * Helper for http and https connections
 */
public class NetworkHandler {

    InputStream in = null;
    HttpURLConnection conn = null;

    public NetworkHandler() {
        // empty constructor
    }

    /**
     * Checks internet connection with a site with high availability
     *
     * @return true, if connected to internet
     */
    public static boolean isInternetAvailable() {
        try {
            InetAddress ipAddress = InetAddress.getByName(StaticIdentifier.siteToCheckInternet);
            // You can replace it with your name
            // noinspection EqualsBetweenInconvertibleTypes
            return !ipAddress.equals("");

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Makes service call to requestUrl
     *
     * @param requestUrl url
     * @param className  class which called the method
     * @return String
     */
    @SuppressLint("LongLogTag")
    public String makeServiceCall(String requestUrl, String className) {
        String response = null;
        in = null;
        conn = null;
        try {
            if (requestUrl != null) {
                URL url = new URL(requestUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                // read the response
                in = new BufferedInputStream(conn.getInputStream());
                response = convertStreamToString(in);
            }
        } catch (Exception e) {
            if (className != null) {
                Log.d("NetworkHandler by " + className + ": response was " + response, Log.getStackTraceString(e));
            }
        } finally {
            closeConnectionsAndBuffers();
        }
        return response;
    }

    /**
     * Closes the InputStream and the HttpURLConnection
     */
    public void closeConnectionsAndBuffers() {
        try {
            if (in != null) {
                in.close();
            }
            if (conn != null) {
                conn.disconnect();
            }
        } catch (Exception e) {
            Log.d("NetworkHandler", Log.getStackTraceString(e));
        }
    }

    /**
     * Converts InputStream to String
     *
     * @param is InputStream
     * @return String
     * @throws IOException IOException
     */
    private String convertStreamToString(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (IOException e) {
            Log.d("NetworkHandler", Log.getStackTraceString(e));
        } finally {
            is.close();
        }
        return sb.toString();
    }
}

