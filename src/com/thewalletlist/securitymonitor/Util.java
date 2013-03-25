package com.thewalletlist.securitymonitor;

import java.io.*;
import java.util.Random;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import android.util.Log;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class Util {

  public static String convertStreamToString(InputStream is)
      throws java.io.IOException {
    BufferedReader reader =
      new BufferedReader(new InputStreamReader(is));
    StringBuilder sb = new StringBuilder();
    String line = null;

    while ((line = reader.readLine()) != null) {
      sb.append(line);
    }

    is.close();

    return sb.toString();
  }

  static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
  static Random rnd = new Random();

  public static String randomString(int length) {
    int ablen = AB.length();
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      sb.append(AB.charAt(rnd.nextInt(ablen)));
    }
    return sb.toString();
  }

  public final static boolean isValidEmail(CharSequence target) {
    if (target == null) {
      return false;
    } else {
      return android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }
  }

  public static String downloadReq(String email) {
    InputStream in = null;
    OutputStream out = null;
    HttpURLConnection conn = null;

    try {
      URL url = new URL("https://thewalletlist.com/api/lookup/" + email);
      conn = (HttpURLConnection) url.openConnection();

      conn.setReadTimeout(10000);
      conn.setConnectTimeout(15000);
      conn.setRequestMethod("GET");
      conn.setDoInput(true);

      conn.connect();

      int response = conn.getResponseCode();
      if (response == 404) {
        Log.d(C.LOG, "404 for " + email);
        return null;
      } else if (response == 403) {
        Log.d(C.LOG, "403 for " + email);
        return "";
      } else if (response == 200) {
        Log.d(C.LOG, "200 for " + email);
        in = new BufferedInputStream(conn.getInputStream());
        String res = convertStreamToString(in);
        return res;
      } else {
        Log.d(C.LOG, "unrecognized http response: " + response + " for " + email);
        return null;
      }
    } catch (java.net.ConnectException e) {
      Log.e(C.LOG, e.toString() + ":" + e.getMessage());
      return null;
    } catch (java.io.IOException e) {
      Log.e(C.LOG, e.toString() + ":" + e.getMessage());
      return null;
    } finally {
        try {
          if (conn != null) { conn.disconnect(); };
          if (in != null) { in.close(); };
          if (out != null) { out.close(); };
        } catch (java.io.IOException e) {
          Log.e(C.LOG, "while closing: " + e.toString());
        }
      }
    }

  // return values:
  public static final int EMAIL_BLANK = 0;
  public static final int NO_CHANGE = 1;
  public static final int CHANGE = 2;
  public static int update(SharedPreferences prefs, int widgetId) {
    String email = prefs.getString(C.getEmailKey(widgetId),"");
    if (email.equals("")) { return EMAIL_BLANK; }

    String savedResult = prefs.getString(C.getSavedResultKey(widgetId),null);
    if (savedResult == null) {
      return NO_CHANGE;
    }
    String res = downloadReq(email);

    Log.w(C.LOG, "email: " + email);
    Log.w(C.LOG, "saved: " + savedResult);
    Log.w(C.LOG, "actual res: " + res);

    SharedPreferences.Editor e = prefs.edit();
    Date now = new Date();
    e.putString(C.getLastResultKey(widgetId),res);
    e.putLong(C.getLastDateKey(widgetId),now.getTime());

    int retval = 0;
    if (res.equals(savedResult)) {
      // all is well
      Log.w(C.LOG, "all is well");
      e.putLong(C.getSavedDateKey(widgetId),now.getTime());
      retval = NO_CHANGE;
    } else {
      Log.w(C.LOG, "something changed!");
      retval = CHANGE;
    }
    e.apply();

    return retval;
  }

  public static String displayResult(String result) {
    if (result == null)
      return "(email not found)";
    else if (result.equals(""))
      return "(empty)";
    else
      return result;
  }

}
