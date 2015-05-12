/**
 * Copyright (C) 2014 The CyanogenMod Project
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
package org.whispersystems.whisperpush;

import java.io.IOException;

import org.whispersystems.whisperpush.gcm.GcmHelper;
import org.whispersystems.whisperpush.util.WhisperPreferences;

import android.app.Application;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

public class WhisperPush extends Application {

    private static final String TAG = WhisperPush.class.getSimpleName();

    private static final long MILLIS_PER_HOUR = 60L * 60L * 1000L;
    private static final long MILLIS_PER_DAY = 24L * MILLIS_PER_HOUR;
    private static final long UPDATE_INTERVAL = 7L * MILLIS_PER_DAY;

    @Override
    public void onCreate() {
        long lastRegistered = WhisperPreferences.getGcmRegistrationTime(this);

        if(lastRegistered != -1
                && (lastRegistered + UPDATE_INTERVAL) < System.currentTimeMillis()) {
            //It has been a week, reregister
            launchGcmRegistration(this);
        }
    }

    private static void launchGcmRegistration(final Context context) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    GcmHelper.getRegistrationId(context);
                    WhisperPreferences.setGcmRegistrationTime(context, System.currentTimeMillis());
                    return true;
                } catch (IOException e) {
                    Log.e(TAG, "GcmRecurringRegistration", e);
                }
                return false;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result) {
                    Log.i(TAG, "GcmRecurringRegistration reregistered");
                }
            }
        }.execute();
    }

    private static boolean visible = false;

    public static void activityResumed() {
        visible = true;
    }

    public static void activityPaused() {
        visible = false;
    }

    public static boolean isActivityVisible() {
        return visible;
    }
}
