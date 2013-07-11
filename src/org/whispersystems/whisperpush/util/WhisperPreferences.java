package org.whispersystems.whisperpush.util;


import android.content.Context;
import android.preference.PreferenceManager;

/**
 * A layer of indirection in front of the app's SharedPreferences.
 */
public class WhisperPreferences {

  private static final String PREF_VERIFYING             = "pref_verifying";
  private static final String PREF_REGISTRATION_COMPLETE = "pref_registration_complete";
  private static final String PREF_LOCAL_NUMBER          = "pref_registered_number";
  private static final String PREF_PUSH_PASSWORD         = "pref_push_password";

  public static boolean isRegistered(Context context) {
    return getBooleanPreference(context, PREF_REGISTRATION_COMPLETE, false);
  }

  public static void setRegistered(Context context, boolean registered) {
    setBooleanPreference(context, PREF_REGISTRATION_COMPLETE, registered);
  }

  public static boolean isVerifying(Context context) {
    return getBooleanPreference(context, PREF_VERIFYING, false);
  }

  public static void setVerifying(Context context, boolean verifying) {
    setBooleanPreference(context, PREF_VERIFYING, verifying);
  }

  public static String getLocalNumber(Context context) {
    return getStringPreference(context, PREF_LOCAL_NUMBER, null);
  }

  public static void setLocalNumber(Context context, String localNumber) {
    setStringPreference(context, PREF_LOCAL_NUMBER, localNumber);
  }

  public static String getPushServerPassword(Context context) {
    return getStringPreference(context, PREF_PUSH_PASSWORD, null);
  }

  public static void setPushServerPassword(Context context, String password) {
    setStringPreference(context, PREF_PUSH_PASSWORD, password);
  }

  private static String getStringPreference(Context context, String key, String defaultValue) {
    return PreferenceManager.getDefaultSharedPreferences(context).getString(key, defaultValue);
  }

  private static void setStringPreference(Context context, String key, String value) {
    PreferenceManager.getDefaultSharedPreferences(context).edit().putString(key, value).commit();
  }

  private static boolean getBooleanPreference(Context context, String key, boolean defaultValue) {
    return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key, defaultValue);
  }

  private static void setBooleanPreference(Context context, String key, boolean value) {
    PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(key, value).commit();
  }

}
