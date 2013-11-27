package org.whispersystems.whisperpush.util;

import android.content.Context;

import org.whispersystems.textsecure.push.PushServiceSocket;
import org.whispersystems.whisperpush.R;

import java.io.InputStream;

public class WhisperPushTrustStore implements PushServiceSocket.TrustStore {

  private final Context context;

  public WhisperPushTrustStore(Context context) {
    this.context = context.getApplicationContext();
  }

  @Override
  public InputStream getKeyStoreInputStream() {
    return context.getResources().openRawResource(R.raw.whisper);
  }

  @Override
  public String getKeyStorePassword() {
    return "whisper";
  }
}
