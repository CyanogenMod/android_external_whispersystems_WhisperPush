package org.whispersystems.whisperpush.service;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;
import org.whispersystems.textsecure.push.PushServiceSocket;
import org.whispersystems.textsecure.push.RateLimitException;
import org.whispersystems.whisperpush.util.WhisperPreferences;

import java.io.IOException;

public class GcmIntentService extends GCMBaseIntentService {

  public static final String GCM_SENDER_ID = "312334754206";

  @Override
  protected void onRegistered(Context context, String registrationId) {
    if (!WhisperPreferences.isRegistered(context)) {
      Intent intent = new Intent(RegistrationService.GCM_REGISTRATION_EVENT);
      intent.putExtra(RegistrationService.GCM_REGISTRATION_ID, registrationId);
      sendBroadcast(intent);
    } else {
      try {
        getPushServiceSocket(context).registerGcmId(registrationId);
      } catch (IOException e) {
        Log.w("GcmIntentService", e);
      } catch (RateLimitException e) {
        Log.w("GcmIntentService", e);
      }
    }
  }

  @Override
  protected void onUnregistered(Context context, String registrationId) {
    try {
      getPushServiceSocket(context).unregisterGcmId(registrationId);
    } catch (IOException ioe) {
      Log.w("GcmIntentService", ioe);
    } catch (RateLimitException e) {
      Log.w("GcmIntentService", e);
    }
  }


  @Override
  protected void onMessage(Context context, Intent intent) {
    // TODO
  }

  @Override
  protected void onError(Context context, String s) {
    Log.w("GcmIntentService", "GCM Error: " + s);
  }

  private PushServiceSocket getPushServiceSocket(Context context) {
    String localNumber = WhisperPreferences.getLocalNumber(context);
    String password    = WhisperPreferences.getPushServerPassword(context);
    return new PushServiceSocket(context, localNumber, password);
  }
}
