/*
 * Copyright (C) 2015 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.whispersystems.whisperpush.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

public class StatsUtils {
    private static final String STATS_PACKAGE = "com.cyngn.stats";
    private static final String ANALYTIC_INTENT = "com.cyngn.stats.action.SEND_ANALYTIC_EVENT";
    private static final String ANALYTIC_PERMISSION = "com.cyngn.stats.SEND_ANALYTICS";
    private static final String TRACKING_ID = "tracking_id";

    public static boolean isStatsActive(Context context) {
        return isStatsPackageInstalled(context) && isStatsCollectionEnabled(context);
    }

    public static boolean isStatsCollectionEnabled(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(),
                Settings.Secure.STATS_COLLECTION, 1) != 0;
    }

    public static boolean isStatsPackageInstalled(Context context) {
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(STATS_PACKAGE, 0);
            return pi.applicationInfo.enabled
                    && ((pi.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static void sendWeeklyActiveUserEvent(Context context) {
        if (!isStatsActive(context)) { return; }

        // Create new intent
        Intent intent = new Intent();
        intent.setAction(ANALYTIC_INTENT);

        // add tracking id
        intent.putExtra(TRACKING_ID, context.getPackageName());
        // metric
        intent.putExtra("category", "usage");
        intent.putExtra("action", "active");
        intent.putExtra("label", "weekly");
        if(WhisperPreferences.getWasActive(context)) {
            intent.putExtra("value", "active");
        } else {
            intent.putExtra("value", "inactive");
        }

        // reset activity marker
        WhisperPreferences.setWasActive(context, false);

        try {
            // broadcast to cmstats
            context.sendBroadcast(intent, ANALYTIC_PERMISSION);
            Log.d("StatsUtils", "sent wau metric");
        } catch (RuntimeException e) {
            Log.e("StatsUtils", "FAILED to send wau metric", e);
        }
    }
}