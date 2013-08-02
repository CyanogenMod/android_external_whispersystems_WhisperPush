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


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.thoughtcrimegson.Gson;
import com.google.thoughtcrimegson.JsonSyntaxException;
import org.whispersystems.textsecure.push.IncomingPushMessage;
import org.whispersystems.textsecure.util.Util;
import org.whispersystems.whisperpush.service.SendReceiveService;

/**
 * The broadcast receiver that handles GCM events, such as incoming GCM messages.
 *
 * @author Moxie Marlinspike
 */
public class GcmReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);

    if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(gcm.getMessageType(intent))) {
      try {
        String data = intent.getStringExtra("message");

        Log.w("GcmReceiver", "GCM message: " + data);

        if (Util.isEmpty(data))
          return;

        IncomingPushMessage message = new Gson().fromJson(data, IncomingPushMessage.class);

        Intent serviceIntent = new Intent(context, SendReceiveService.class);
        serviceIntent.setAction(SendReceiveService.RECEIVE_SMS);
        serviceIntent.putExtra("message", message);
        context.startService(serviceIntent);
      } catch (JsonSyntaxException e) {
        Log.w("GcmReceiver", e);
      }
    }

    setResultCode(Activity.RESULT_OK);
  }
}
