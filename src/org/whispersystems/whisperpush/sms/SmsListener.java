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
package org.whispersystems.whisperpush.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import org.whispersystems.textsecure.directory.Directory;
import org.whispersystems.textsecure.directory.NotInDirectoryException;
import org.whispersystems.textsecure.util.InvalidNumberException;
import org.whispersystems.textsecure.util.PhoneNumberFormatter;
import org.whispersystems.whisperpush.service.DirectoryRefreshListener;
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

    private static final String PROTECTED_SMS_RECEIVED_ACTION =
            "android.provider.Telephony.ACTION_PROTECTED_SMS_RECEIVED";
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

        try {
            String number = PhoneNumberFormatter.formatNumber(destination, localNumber);
            return Directory.getInstance(context).isActiveNumber(number);
        } catch (NotInDirectoryException e) {
            return true;
        } catch (InvalidNumberException e) {
            Log.w("SmsListener", e);
            return false;
        }
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

        if (PROTECTED_SMS_RECEIVED_ACTION.equals(intent.getAction())
                && isIncomingChallenge(context, intent)) {
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
