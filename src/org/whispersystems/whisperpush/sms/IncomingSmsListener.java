/**
 * Copyright (C) 2015 The CyanogenMod Project
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
package org.whispersystems.whisperpush.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import org.whispersystems.whisperpush.service.DirectoryRefreshListener;
import org.whispersystems.whisperpush.service.RegistrationService;
import org.whispersystems.whisperpush.util.WhisperPreferences;

/**
 * A BroadcastReceiver that listens for incoming SMS events.
 *
 * If the incoming SMS is a registration challenge, it'll abort the broadcast and
 * send a notification to the RegistrationService. *
 */
public class IncomingSmsListener extends BroadcastReceiver {

    private static final String PROTECTED_SMS_RECEIVED_ACTION =
            "android.provider.Telephony.ACTION_PROTECTED_SMS_RECEIVED";
    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";

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

    private String parseChallenge(Intent intent) {
        String messageBody    = getSmsMessageBodyFromIntent(intent);
        String[] messageParts = messageBody.split(":");
        String[] codeParts    = messageParts[1].trim().split("-");

        return codeParts[0] + codeParts[1];
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        DirectoryRefreshListener.schedule(context);

        String action        = intent.getAction();
        boolean incomingSms  = SMS_RECEIVED.equals(action);
        boolean protectedSms = PROTECTED_SMS_RECEIVED_ACTION.equals(action);

        if ((incomingSms || protectedSms) && isIncomingChallenge(context, intent)) {
            Intent challengeIntent = new Intent(RegistrationService.CHALLENGE_EVENT);
            challengeIntent.putExtra(RegistrationService.CHALLENGE_EXTRA, parseChallenge(intent));
            context.sendBroadcast(challengeIntent);

            // if we got this as a protected SMS, we are the only ones who need to see it
            if(protectedSms) { abortBroadcast(); }
        }
    }
}