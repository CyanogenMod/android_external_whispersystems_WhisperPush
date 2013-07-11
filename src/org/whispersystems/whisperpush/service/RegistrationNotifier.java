package org.whispersystems.whisperpush.service;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.whispersystems.whisperpush.R;

/**
 * Catch registration complete broadcasts and display a notification if there is no visible
 * view handling them.
 */
public class RegistrationNotifier extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    Notification.Builder builder = new Notification.Builder(context);
    builder.setSmallIcon(R.drawable.icon);
    builder.setContentTitle(intent.getStringExtra(RegistrationService.NOTIFICATION_TITLE));
    builder.setContentText(intent.getStringExtra(RegistrationService.NOTIFICATION_TEXT));

    // TODO Connect to upcoming settings UI
    //builder.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, SettingsActivity.class), 0));

    builder.setWhen(System.currentTimeMillis());
    builder.setDefaults(Notification.DEFAULT_VIBRATE);
    builder.setAutoCancel(true);

    Notification notification = builder.getNotification();
    ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(31337, notification);
  }
}
