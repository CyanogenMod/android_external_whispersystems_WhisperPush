package org.whispersystems.whisperpush.util;

import android.content.Context;

import org.whispersystems.textsecure.push.PushServiceSocket;
import org.whispersystems.whisperpush.Release;

public class PushServiceSocketFactory {
  public static PushServiceSocket create(Context context, String number, String password) {
    return new PushServiceSocket(context, Release.PUSH_URL, new WhisperPushTrustStore(context),
                                 number, password);
  }

  public static PushServiceSocket create(Context context) {
    return create(context,
                  WhisperPreferences.getLocalNumber(context),
                  WhisperPreferences.getPushServerPassword(context));
  }
}
