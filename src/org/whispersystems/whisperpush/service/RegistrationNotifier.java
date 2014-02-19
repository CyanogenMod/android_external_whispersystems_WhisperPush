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


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.whispersystems.whisperpush.R;
import org.whispersystems.whisperpush.ui.RegistrationCompletedActivity;

/**
 * Catch registration complete broadcasts and display a notification if there is no visible
 * view handling them.
 *
 * @author Moxie Marlinspike
 */
public class RegistrationNotifier extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Notification.Builder builder = new Notification.Builder(context);
        builder.setSmallIcon(R.drawable.icon);
        builder.setContentTitle(intent.getStringExtra(RegistrationService.NOTIFICATION_TITLE));
        builder.setContentText(intent.getStringExtra(RegistrationService.NOTIFICATION_TEXT));

        builder.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, RegistrationCompletedActivity.class), 0));
        builder.setWhen(System.currentTimeMillis());
        builder.setDefaults(Notification.DEFAULT_VIBRATE);
        builder.setAutoCancel(true);

        Notification notification = builder.getNotification();
        ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(31337, notification);
    }
}
