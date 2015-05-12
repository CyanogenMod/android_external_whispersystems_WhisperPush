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


import org.whispersystems.whisperpush.service.DirectoryRefreshListener;
import org.whispersystems.whisperpush.service.SendReceiveService;
import org.whispersystems.whisperpush.util.WhisperPreferences;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

/**
 * The broadcast receiver that handles GCM events, such as incoming GCM messages.
 *
 * @author Moxie Marlinspike
 */
public class GcmReceiver extends BroadcastReceiver {
  private static final String TAG = "GcmReceiver";

  @Override
  public void onReceive(Context context, Intent intent) {
    DirectoryRefreshListener.schedule(context);
    GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);

    if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(gcm.getMessageType(intent))) {
      Log.d(TAG, "GCM message...");

      if (!WhisperPreferences.isRegistered(context)) {
        Log.w(TAG, "Not push registered!");
        return;
      }

      boolean isMessage = intent.hasExtra("message");
      boolean isReceipt = intent.hasExtra("receipt");
      boolean isNotification = intent.hasExtra("notification");

      // According to Moxie, no message bodies should be delivered
      // over GCM anymore, only notifications to fetch messages.
      // but just in case we see something different, do some
      // logging here - WF
      if (isMessage) {
          Log.w(TAG, "received unexpected message via GCM");
      }
      if (isReceipt) {
          Log.w(TAG, "received unexpected receipt via GCM");
      }
      if (isNotification || isMessage || isReceipt) {
          handleReceivedNotification(context);
      }
    }

    setResultCode(Activity.RESULT_OK);
  }

  private void handleReceivedNotification(Context context) {
    Intent serviceIntent = new Intent(context, SendReceiveService.class);
    serviceIntent.setAction(SendReceiveService.RCV_NOTIFICATION);
    context.startService(serviceIntent);
  }
}