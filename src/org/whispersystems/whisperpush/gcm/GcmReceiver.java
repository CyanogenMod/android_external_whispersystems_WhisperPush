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


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.thoughtcrimegson.Gson;
import com.google.thoughtcrimegson.JsonSyntaxException;
import org.whispersystems.textsecure.crypto.InvalidVersionException;
import org.whispersystems.textsecure.push.IncomingEncryptedPushMessage;
import org.whispersystems.textsecure.push.IncomingPushMessage;
import org.whispersystems.textsecure.util.Util;
import org.whispersystems.whisperpush.R;
import org.whispersystems.whisperpush.service.DirectoryRefreshListener;
import org.whispersystems.whisperpush.service.MessageNotifier;
import org.whispersystems.whisperpush.service.SendReceiveService;
import org.whispersystems.whisperpush.util.WhisperPreferences;

import java.io.IOException;

/**
 * The broadcast receiver that handles GCM events, such as incoming GCM messages.
 *
 * @author Moxie Marlinspike
 */
public class GcmReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    DirectoryRefreshListener.schedule(context);
    GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);

    if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(gcm.getMessageType(intent))) {
      try {
        String data = intent.getStringExtra("message");

        if (Util.isEmpty(data))
          return;

        String                       signalingKey     = WhisperPreferences.getSignalingKey(context);
        IncomingEncryptedPushMessage encryptedMessage = new IncomingEncryptedPushMessage(data, signalingKey);
        IncomingPushMessage          message          = encryptedMessage.getIncomingPushMessage();

        Intent serviceIntent = new Intent(context, SendReceiveService.class);
        serviceIntent.setAction(SendReceiveService.RECEIVE_SMS);
        serviceIntent.putExtra("message", message);
        context.startService(serviceIntent);
      } catch (IOException e) {
        Log.w("GcmReceiver", e);
        MessageNotifier.notifyProblemAndUnregister(context, context.getString(R.string.GcmReceiver_error),
                context.getString(R.string.GcmReceiver_invalid_push_message) + "\n" + context.getString(R.string.GcmReceiver_received_badly_formatted_push_message));
      } catch (InvalidVersionException e) {
        Log.w("GcmReceiver", e);
        MessageNotifier.notifyProblem(context, context.getString(R.string.GcmReceiver_error),
                context.getString(R.string.GcmReceiver_received_badly_formatted_push_message) + "\n" + context.getString(R.string.GcmReceiver_received_push_message_with_unknown_version));
      }
    }

    setResultCode(Activity.RESULT_OK);
  }
}
