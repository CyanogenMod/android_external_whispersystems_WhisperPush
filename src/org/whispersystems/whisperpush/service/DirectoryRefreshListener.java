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
package org.whispersystems.whisperpush.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.whispersystems.whisperpush.util.WhisperPreferences;

public class DirectoryRefreshListener extends BroadcastReceiver {

    private static final String REFRESH_EVENT = "org.whispersystems.whisperpush.DIRECTORY_REFRESH";
    private static final String BOOT_EVENT    = Intent.ACTION_BOOT_COMPLETED;

    private static final long   INTERVAL      = 24 * 60 * 60 * 1000; // 24 hours.

    @Override
    public void onReceive(Context context, Intent intent) {
        if      (REFRESH_EVENT.equals(intent.getAction())) handleRefreshAction(context);
        else if (BOOT_EVENT.equals(intent.getAction()))    handleBootEvent(context);
    }

    private void handleBootEvent(Context context) {
        schedule(context);
    }

    private void handleRefreshAction(Context context) {
        schedule(context);
    }

    public static void schedule(Context context) {
        if (!WhisperPreferences.isRegistered(context)) return;

        AlarmManager      alarmManager  = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent            intent        = new Intent(DirectoryRefreshListener.REFRESH_EVENT);
        PendingIntent     pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        long              time          = WhisperPreferences.getDirectoryRefreshTime(context);

        if (time <= System.currentTimeMillis()) {
            if (time != 0) {
                Intent serviceIntent = new Intent(context, DirectoryRefreshService.class);
                serviceIntent.setAction(DirectoryRefreshService.REFRESH_ACTION);
                context.startService(serviceIntent);
            }

            time = System.currentTimeMillis() + INTERVAL;
        }

        Log.w("DirectoryRefreshService", "Scheduling for: " + time);

        alarmManager.cancel(pendingIntent);
        alarmManager.set(AlarmManager.RTC_WAKEUP, time, pendingIntent);

        WhisperPreferences.setDirectoryRefreshTime(context, time);
    }

}
