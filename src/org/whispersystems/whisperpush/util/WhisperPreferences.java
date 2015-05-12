/**
 * Copyright (C) 2013 The CyanogenMod Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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

    private static final String PREF_VERIFYING              = "pref_verifying";
    private static final String PREF_REGISTRATION_COMPLETE  = "pref_registration_complete";
    private static final String PREF_LOCAL_NUMBER           = "pref_registered_number";
    private static final String PREF_PUSH_PASSWORD          = "pref_push_password";
    private static final String PREF_GCM_ID                 = "pref_gcm_id";
    private static final String PREF_GCM_REGISTRATION_TIME  = "pref_gcm_reg_time";
    private static final String PREF_GCM_VERSION            = "pref_gcm_version";
    private static final String PREF_IDENTITY_PUBLIC_KEY    = "pref_identity_public";
    private static final String PREF_IDENTITY_PRIVATE_KEY   = "pref_identity_private";
    private static final String PREF_MASTER_SECRET          = "pref_master_secret";
    private static final String PREF_SIGNALING_KEY          = "pref_signaling_key";
    private static final String PREF_DIRECTORY_REFRESH_TIME = "pref_directory_refresh";
    private static final String PREF_WAS_ACTIVE             = "pref_was_active";
    private static final String PREF_NEXT_STAT_TIME         = "pref_next_stat_time";
    private static final String PREF_INSTALL_ID             = "pref_install_id";

    public static long getDirectoryRefreshTime(Context context) {
        return getLongPreference(context, PREF_DIRECTORY_REFRESH_TIME, 0);
    }

    public static void setDirectoryRefreshTime(Context context, long value) {
        setLongPreference(context, PREF_DIRECTORY_REFRESH_TIME, value);
    }

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

    public static void setSignalingKey(Context context, String signalingKey) {
        setStringPreference(context, PREF_SIGNALING_KEY, signalingKey);
    }

    public static String getSignalingKey(Context context) {
        return getStringPreference(context, PREF_SIGNALING_KEY, null);
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

    public static long getGcmRegistrationTime(Context context) {
        return getLongPreference(context, PREF_GCM_REGISTRATION_TIME, -1);
    }

    public static void setGcmRegistrationTime(Context context, long timeSinceEpoch) {
        setLongPreference(context, PREF_GCM_REGISTRATION_TIME, timeSinceEpoch);
    }

    public static boolean getWasActive(Context context) {
        return getBooleanPreference(context, PREF_WAS_ACTIVE, false);
    }

    public static void setWasActive(Context context, boolean active) {
        setBooleanPreference(context, PREF_WAS_ACTIVE, active);
    }

    public static long getNextStatTime(Context context) {
        return getLongPreference(context, PREF_NEXT_STAT_TIME, -1);
    }

    public static void setNextStateTime(Context context, long timeSinceEpoch) {
        setLongPreference(context, PREF_NEXT_STAT_TIME, timeSinceEpoch);
    }

    public static void setInstallId(Context context, int installId) {
        setIntegerPreference(context, PREF_INSTALL_ID, installId);
    }

    public static int getInstallId(Context context) {
        return getIntegerPreference(context, PREF_INSTALL_ID, 0);
    }

    public static void resetPreferences(Context context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();
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

    private static void setIntegerPreference(Context context, String key, int value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(key, value).commit();
    }

    private static long getLongPreference(Context context, String key, long defaultValue) {
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(key, defaultValue);
    }

    private static void setLongPreference(Context context, String key, long value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putLong(key, value).commit();
    }
}