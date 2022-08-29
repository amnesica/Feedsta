package com.amnesica.feedsta.helper;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

/** Helper class for properties */
public class PropertiesHelper {

  /**
   * Return properties from file debug.properties
   *
   * @return Properties
   */
  private Properties getPropertiesFile() {
    try (InputStream input =
        Objects.requireNonNull(this.getClass().getClassLoader())
            .getResourceAsStream("assets/debug.properties")) {

      Properties properties = new Properties();

      // load properties file
      properties.load(input);

      return properties;
    } catch (IOException e) {
      Log.d("PropertiesHelper", Log.getStackTraceString(e));
    }
    return null;
  }

  /**
   * Returns address with port of debug server from properties file debug.properties
   *
   * @return String
   */
  String getDebugServerAddressWithPort() {
    Properties properties = getPropertiesFile();

    if (properties == null) return null;

    return properties.getProperty("debugServerAddressAndPort");
  }
}
