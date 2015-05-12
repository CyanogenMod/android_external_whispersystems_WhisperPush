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
import android.text.TextUtils;
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

      String messageData = intent.getStringExtra("message");
      String receiptData = intent.getStringExtra("receipt");

      if      (!TextUtils.isEmpty(messageData)) handleReceivedMessage(context, messageData);
      else if (!TextUtils.isEmpty(receiptData)) handleReceivedMessage(context, receiptData);
      else if (intent.hasExtra("notification")) handleReceivedNotification(context);
    }

    setResultCode(Activity.RESULT_OK);
  }

  private void handleReceivedNotification(Context context) {
    // FIXME: figure out the right thing to do here
  }

  private void handleReceivedMessage(Context context, String message) {
    Intent serviceIntent = new Intent(context, SendReceiveService.class);
    serviceIntent.setAction(SendReceiveService.RECEIVE_SMS);
    serviceIntent.putExtra("message", message);
    context.startService(serviceIntent);
  }
}