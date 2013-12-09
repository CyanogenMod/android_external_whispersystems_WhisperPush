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


import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Pair;

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

  public static String getRegistrationId(Context context) throws IOException {
    String registrationId = getCurrentRegistrationId(context);

    if (registrationId == null) {
      registrationId = GoogleCloudMessaging.getInstance(context).register(Release.GCM_SENDER_ID);
      setCurrentRegistrationId(context, registrationId);
    }

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
