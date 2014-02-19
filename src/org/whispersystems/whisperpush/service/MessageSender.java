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
package org.whispersystems.whisperpush.service;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver.PendingResult;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.whispersystems.textsecure.crypto.MasterSecret;
import org.whispersystems.textsecure.directory.Directory;
import org.whispersystems.textsecure.directory.NotInDirectoryException;
import org.whispersystems.textsecure.push.ContactTokenDetails;
import org.whispersystems.textsecure.push.PushBody;
import org.whispersystems.textsecure.push.PushDestination;
import org.whispersystems.textsecure.push.PushMessageProtos.PushMessageContent;
import org.whispersystems.textsecure.push.PushServiceSocket;
import org.whispersystems.textsecure.util.InvalidNumberException;
import org.whispersystems.textsecure.util.PhoneNumberFormatter;
import org.whispersystems.textsecure.util.Util;
import org.whispersystems.whisperpush.crypto.MasterSecretUtil;
import org.whispersystems.whisperpush.crypto.WhisperCipher;
import org.whispersystems.whisperpush.sms.OutgoingSmsQueue.OutgoingMessageCandidate;
import org.whispersystems.whisperpush.util.PushServiceSocketFactory;
import org.whispersystems.whisperpush.util.WhisperPreferences;

import java.io.IOException;
import java.util.List;

public class MessageSender {

    private static final String PARTS        = "parts";
    private static final String SENT_INTENTS = "sentIntents";

    private final Context context;

    public MessageSender(Context context) {
        this.context = context.getApplicationContext();
    }

    public void handleSendMessage(OutgoingMessageCandidate candidate) {
        Log.w("MessageSender", "Got outgoing message candidate: " + candidate);

        if (candidate == null)
            return;

        Intent sendIntent  = candidate.getIntent();
        String destination = sendIntent.getStringExtra(SendReceiveService.DESTINATION);

        if (!isRegisteredUser(destination)) {
            Log.w("MessageSender", "Not a registered user...");
            abortSendOperation(candidate);
            return;
        }

        try {
            List<String>      messageParts    = sendIntent.getStringArrayListExtra(PARTS);
            String            localNumber     = WhisperPreferences.getLocalNumber(context);
            PushDestination   pushDestination = PushDestination.create(context, localNumber, destination);
            PushServiceSocket socket          = PushServiceSocketFactory.create(context);
            PushBody          body            = getEncryptedMessage(pushDestination, messageParts);

            socket.sendMessage(pushDestination, body);

            notifySendComplete(sendIntent);
            completeSendOperation(candidate);
        } catch (IOException e) {
            Log.w("MessageSender", e);
            abortSendOperation(candidate);
        } catch (InvalidNumberException e) {
            Log.w("MessageSender", e);
            abortSendOperation(candidate);
        }
    }

    private void completeSendOperation(OutgoingMessageCandidate candidate) {
        PendingResult pendingResult = candidate.getPendingResult();
        pendingResult.abortBroadcast();
        pendingResult.setResultCode(Activity.RESULT_CANCELED);
        pendingResult.finish();
    }

    private void abortSendOperation(OutgoingMessageCandidate candidate) {
        PendingResult pendingResult = candidate.getPendingResult();
        pendingResult.finish();
    }

    private void notifySendComplete(Intent sendIntent) {
        List<PendingIntent> sentIntents = sendIntent.getParcelableArrayListExtra(SENT_INTENTS);

        if (sentIntents == null) {
            Log.w("MessageSender", "Warning, no sent intents available!");
            return;
        }

        for (PendingIntent sentIntent : sentIntents) {
            try {
                sentIntent.send(Activity.RESULT_OK);
            } catch (PendingIntent.CanceledException e) {
                Log.w("MessageSender", e);
            }
        }
    }

    private PushBody getEncryptedMessage(PushDestination pushDestination,
                                         List<String> messageParts)
            throws IOException
    {
        PushServiceSocket socket        = PushServiceSocketFactory.create(context);
        String            message       = Util.join(messageParts, "");
        byte[]            plaintext     = PushMessageContent.newBuilder().setBody(message).build().toByteArray();
        MasterSecret      masterSecret  = MasterSecretUtil.getMasterSecret(context);
        WhisperCipher     whisperCipher = new WhisperCipher(context, masterSecret, pushDestination);

        return whisperCipher.getEncryptedMessage(socket, plaintext);
    }

    private boolean isRegisteredUser(String number) {
        Log.w("MessageSender", "Number to canonicalize: " + number);
        String    localNumber = WhisperPreferences.getLocalNumber(context);
        Directory directory   = Directory.getInstance(context);

        String e164number;

        try {
            e164number  = PhoneNumberFormatter.formatNumber(number, localNumber);
        } catch (InvalidNumberException e) {
            Log.w("MessageSender", e);
            return false;
        }

        if (e164number.equals(localNumber)) {
            return false;
        }

        try {
            return directory.isActiveNumber(e164number);
        } catch (NotInDirectoryException e) {
            try {
                PushServiceSocket   socket              = PushServiceSocketFactory.create(context);
                Log.w("MessageSender", "Getting contact token for: " + e164number);
                String              contactToken        = directory.getToken(e164number);
                ContactTokenDetails contactTokenDetails = socket.getContactTokenDetails(contactToken);

                if (contactTokenDetails != null) {
                    directory.setToken(contactTokenDetails, true);
                    return true;
                } else {
                    contactTokenDetails = new ContactTokenDetails(contactToken);
                    directory.setToken(contactTokenDetails, false);
                    return false;
                }
            } catch (IOException e1) {
                Log.w("MessageSender", e1);
                return false;
            }
        }
    }


}
