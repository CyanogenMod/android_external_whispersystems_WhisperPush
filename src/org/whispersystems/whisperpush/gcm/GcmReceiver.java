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
