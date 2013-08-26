/*
 * Copyright (C) 2013 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.whispersystems.whisperpush.util;


import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Pair;

/**
 * A layer of indirection in front of the app's SharedPreferences.
 *
 * @author Moxie Marlinspike
 */
public class WhisperPreferences {

  private static final String PREF_VERIFYING             = "pref_verifying";
  private static final String PREF_REGISTRATION_COMPLETE = "pref_registration_complete";
  private static final String PREF_LOCAL_NUMBER          = "pref_registered_number";
  private static final String PREF_PUSH_PASSWORD         = "pref_push_password";
  private static final String PREF_GCM_ID                = "pref_gcm_id";
  private static final String PREF_GCM_VERSION           = "pref_gcm_version";
  private static final String PREF_IDENTITY_PUBLIC_KEY   = "pref_identity_public";
  private static final String PREF_IDENTITY_PRIVATE_KEY  = "pref_identity_private";
  private static final String PREF_MASTER_SECRET         = "pref_master_secret";

  public static void setMasterSecret(Context context, String value) {
    setStringPreference(context, PREF_MASTER_SECRET, value);
  }

  public static String getMasterSecret(Context context) {
    return getStringPreference(context, PREF_MASTER_SECRET, null);
  }

  public static String getIdentityKeyPublic(Context context) {
    return getStringPreference(context, PREF_IDENTITY_PUBLIC_KEY, null);
  }

  public static void setIdentityKeyPublic(Context context, String value) {
    setStringPreference(context, PREF_IDENTITY_PUBLIC_KEY, value);
  }

  public static String getIdentityKeyPrivate(Context context) {
    return getStringPreference(context, PREF_IDENTITY_PRIVATE_KEY, null);
  }

  public static void setIdentityKeyPrivate(Context context, String value) {
    setStringPreference(context, PREF_IDENTITY_PRIVATE_KEY, value);
  }

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

  public static Pair<String, Integer> getGcmRegistrationId(Context context) {
    return new Pair<String, Integer>(getStringPreference(context, PREF_GCM_ID, null),
                                     getIntegerPreference(context, PREF_GCM_VERSION, 0));
  }

  public static void setGcmRegistrationId(Context context, String gcmId, int version) {
    PreferenceManager.getDefaultSharedPreferences(context).edit()
        .putString(PREF_GCM_ID, gcmId)
        .putInt(PREF_GCM_VERSION, version)
        .commit();
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

  private static int getIntegerPreference(Context context, String key, int defaultValue) {
    return PreferenceManager.getDefaultSharedPreferences(context).getInt(key, defaultValue);
  }

}
