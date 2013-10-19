package org.whispersystems.whisperpush.util;

import android.content.Context;

import org.whispersystems.textsecure.push.PushServiceSocket;

public class WhisperPushCredentials implements PushServiceSocket.PushCredentials {

  private static final WhisperPushCredentials instance = new WhisperPushCredentials();

  public static WhisperPushCredentials getInstance() {
    return instance;
  }

  @Override
  public String getLocalNumber(Context context) {
    return WhisperPreferences.getLocalNumber(context);
  }

  @Override
  public String getPassword(Context context) {
    return WhisperPreferences.getPushServerPassword(context);
  }

}
