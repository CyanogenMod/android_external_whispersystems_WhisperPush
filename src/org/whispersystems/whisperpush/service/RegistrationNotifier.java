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

    // TODO Connect to upcoming settings UI, for now, open RegistrationCompletedActivity
    //builder.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, SettingsActivity.class), 0));

    builder.setWhen(System.currentTimeMillis());
    builder.setDefaults(Notification.DEFAULT_VIBRATE);
    builder.setAutoCancel(true);

    Notification notification = builder.getNotification();
    ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(31337, notification);
  }
}
