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
package org.whispersystems.whisperpush.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import org.whispersystems.textsecure.directory.NumberFilter;
import org.whispersystems.textsecure.util.PhoneNumberFormatter;
import org.whispersystems.whisperpush.service.RegistrationService;
import org.whispersystems.whisperpush.service.SendReceiveService;
import org.whispersystems.whisperpush.sms.OutgoingSmsQueue.OutgoingMessageCandidate;
import org.whispersystems.whisperpush.util.WhisperPreferences;

/**
 * A BroadcastReceiver that listens for incoming and outgoing SMS events.
 *
 * If the incoming SMS is a registration challenge, it'll abort the broadcast and
 * send a notification to the RegistrationService.
 *
 * If the outgoing SMS is to a participating destination, it'll attempt to deliver
 * the message via the push channel and abort the broadcast if successful.
 *
 * @author Moxie Marlinspike
 */
public class SmsListener extends BroadcastReceiver {

  private static final String SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED";
  private static final String SMS_OUTGOING_ACTION = "android.intent.action.NEW_OUTGOING_SMS";

  private String getSmsMessageBodyFromIntent(Intent intent) {
    Bundle bundle             = intent.getExtras();
    Object[] pdus             = (Object[])bundle.get("pdus");
    StringBuilder bodyBuilder = new StringBuilder();

    if (pdus == null)
      return null;

    for (Object pdu : pdus)
      bodyBuilder.append(SmsMessage.createFromPdu((byte[]) pdu).getDisplayMessageBody());

    return bodyBuilder.toString();
  }

  private boolean isIncomingChallenge(Context context, Intent intent) {
    String messageBody = getSmsMessageBodyFromIntent(intent);

    if (messageBody == null)
      return false;

    return messageBody.matches("Your TextSecure verification code: [0-9]{3,4}-[0-9]{3,4}") &&
           WhisperPreferences.isVerifying(context);
  }

  private boolean isRelevantOutgoingMessage(Context context, Intent intent) {
    String destination = intent.getStringExtra(SendReceiveService.DESTINATION);

    if (destination == null)
      return false;

    if (!WhisperPreferences.isRegistered(context))
      return false;

    String localNumber = WhisperPreferences.getLocalNumber(context);

    if (localNumber == null)
      return false;

    String number = PhoneNumberFormatter.formatNumber(destination, localNumber);

    return NumberFilter.getInstance(context).containsNumber(number);
  }

  private String parseChallenge(Intent intent) {
    String messageBody    = getSmsMessageBodyFromIntent(intent);
    String[] messageParts = messageBody.split(":");
    String[] codeParts    = messageParts[1].trim().split("-");

    return codeParts[0] + codeParts[1];
  }

  @Override
  public void onReceive(Context context, Intent intent) {

    if (SMS_RECEIVED_ACTION.equals(intent.getAction()) && isIncomingChallenge(context, intent)) {
      Intent challengeIntent = new Intent(RegistrationService.CHALLENGE_EVENT);
      challengeIntent.putExtra(RegistrationService.CHALLENGE_EXTRA, parseChallenge(intent));
      context.sendBroadcast(challengeIntent);

      abortBroadcast();
    } else if (SMS_OUTGOING_ACTION.equals(intent.getAction()) && isRelevantOutgoingMessage(context, intent)) {
      PendingResult            pendingResult = goAsync();
      OutgoingMessageCandidate candidate     = new OutgoingMessageCandidate(intent, pendingResult);

      OutgoingSmsQueue.getInstance().put(candidate);

      Intent sendIntent = new Intent(context, SendReceiveService.class);
      sendIntent.setAction(SendReceiveService.SEND_SMS);
      context.startService(sendIntent);
    }
  }
}
