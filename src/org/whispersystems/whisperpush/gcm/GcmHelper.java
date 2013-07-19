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
package org.whispersystems.whisperpush.gcm;


import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Pair;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.whispersystems.whisperpush.util.WhisperPreferences;

import java.io.IOException;

/**
 * A helper that manages synchronous retrieval of GCM registration IDs.
 *
 * @author Moxie Marlinspike
 */
public class GcmHelper {

  private static final String GCM_SENDER_ID = "312334754206";

  public static String getRegistrationId(Context context) throws IOException {
    String registrationId = getCurrentRegistrationId(context);

    if (registrationId == null) {
      registrationId = GoogleCloudMessaging.getInstance(context).register(GCM_SENDER_ID);
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
