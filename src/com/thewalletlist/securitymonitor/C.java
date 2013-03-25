package com.thewalletlist.securitymonitor;

public class C {
  public static final String LOG = "com.thewalletlist.securitymonitor";

  private static final String KEY_EMAIL = "pref_email";

  // the user-sanctioned result & last time we got it
  private static final String KEY_SAVED_RESULT = "pref_saved_result";
  private static final String KEY_SAVED_DATE = "pref_saved_date";

  // the last result we got & time we got it
  private static final String KEY_LAST_RESULT = "pref_last_result";
  private static final String KEY_LAST_DATE = "pref_last_date";

  public static String getEmailKey(int widgetId) {
    return KEY_EMAIL + widgetId;
  }

  public static String getSavedResultKey(int widgetId) {
    return KEY_SAVED_RESULT + widgetId;
  }

  public static String getSavedDateKey(int widgetId) {
    return KEY_SAVED_DATE + widgetId;
  }

  public static String getLastResultKey(int widgetId) {
    return KEY_LAST_RESULT + widgetId;
  }

  public static String getLastDateKey(int widgetId) {
    return KEY_LAST_DATE + widgetId;
  }
}
