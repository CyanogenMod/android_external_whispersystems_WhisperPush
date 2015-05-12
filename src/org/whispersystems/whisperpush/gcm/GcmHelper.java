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
package org.whispersystems.whisperpush.gcm;


import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Pair;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.whispersystems.whisperpush.Release;
import org.whispersystems.whisperpush.util.WhisperPreferences;

import java.io.IOException;

/**
 * A helper that manages synchronous retrieval of GCM registration IDs.
 *
 * @author Moxie Marlinspike
 */
public class GcmHelper {

    public final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    public final static int RESULT_SUPPORTED = 1;
    public final static int RESULT_UNSUPPORTED = 2;
    public final static int RESULT_USER_RECOVERABLE = 3;

    public static int checkPlayServices(Context context) {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                return RESULT_USER_RECOVERABLE;
            }
            return RESULT_UNSUPPORTED;
        } else {
            return RESULT_SUPPORTED;
        }
    }

    public static void showPlayServicesDialog(Activity activity) {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);
        GooglePlayServicesUtil.getErrorDialog(resultCode, activity, PLAY_SERVICES_RESOLUTION_REQUEST).show();
    }

    public static String getRegistrationId(Context context) throws IOException {
        // FIMXE: should update to use the new style for handling GCM messages
        String registrationId = GoogleCloudMessaging.getInstance(context).register(Release.GCM_SENDER_ID);
        setCurrentRegistrationId(context, registrationId);
        return registrationId;
    }

    private static void setCurrentRegistrationId(Context context, String registrationId) {
        int currentVersion = getCurrentAppVersion(context);
        WhisperPreferences.setGcmRegistrationId(context, registrationId, currentVersion);
    }

    private static String getCurrentRegistrationId(Context context) {
        int                   currentVersion          = getCurrentAppVersion(context);
        Pair<String, Integer> currentRegistrationInfo = WhisperPreferences.getGcmRegistrationId(context);

        if (currentVersion != currentRegistrationInfo.second) {
            return null;
        }

        return currentRegistrationInfo.first;
    }

    private static int getCurrentAppVersion(Context context) {
        try {
            PackageManager manager     = context.getPackageManager();
            PackageInfo    packageInfo = manager.getPackageInfo(context.getPackageName(), 0);

            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            throw new AssertionError(e);
        }
    }

}
