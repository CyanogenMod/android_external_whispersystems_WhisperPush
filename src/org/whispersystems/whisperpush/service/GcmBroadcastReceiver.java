package org.whispersystems.whisperpush.service;

import android.content.Context;

public class GcmBroadcastReceiver extends com.google.android.gcm.GCMBroadcastReceiver {

  @Override
  protected String getGCMIntentServiceClassName(Context context) {
    return "org.whispersystems.whisperpush.service.GcmIntentService";
  }

}