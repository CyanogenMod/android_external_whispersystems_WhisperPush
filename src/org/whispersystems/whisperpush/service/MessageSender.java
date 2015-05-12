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

import java.io.IOException;
import java.util.List;

import org.whispersystems.libaxolotl.util.guava.Optional;
import org.whispersystems.textsecure.api.TextSecureAccountManager;
import org.whispersystems.textsecure.api.TextSecureMessageSender;
import org.whispersystems.textsecure.api.crypto.UntrustedIdentityException;
import org.whispersystems.textsecure.api.messages.TextSecureMessage;
import org.whispersystems.textsecure.api.push.ContactTokenDetails;
import org.whispersystems.textsecure.api.push.TextSecureAddress;
import org.whispersystems.textsecure.api.util.InvalidNumberException;
import org.whispersystems.textsecure.api.util.PhoneNumberFormatter;
import org.whispersystems.whisperpush.directory.Directory;
import org.whispersystems.whisperpush.directory.NotInDirectoryException;
import org.whispersystems.whisperpush.sms.OutgoingSmsQueue.OutgoingMessageCandidate;
import org.whispersystems.whisperpush.util.WhisperPreferences;
import org.whispersystems.whisperpush.util.WhisperServiceFactory;
import org.whispersystems.whisperpush.util.StatsUtils;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver.PendingResult;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

public class MessageSender {

    private static final String PARTS        = "parts";
    private static final String SENT_INTENTS = "sentIntents";

    private final Context context;

    public MessageSender(Context context) {
        this.context = context.getApplicationContext();
    }

    public void handleSendMessage(OutgoingMessageCandidate candidate) {
        Log.d("MessageSender", "Got outgoing message candidate");

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
            List<String>            messageParts = sendIntent.getStringArrayListExtra(PARTS);
            // put destination in same format as was passed by isRegistered User above
            String                  localNumber  = WhisperPreferences.getLocalNumber(context);
            String                  e164number   = PhoneNumberFormatter.formatNumber(destination, localNumber);
            TextSecureAddress       address      = new TextSecureAddress(e164number);
            TextSecureMessageSender sender       = WhisperServiceFactory.createMessageSender(context);
            TextSecureMessage       body         = TextSecureMessage.newBuilder()
                                                                    .withBody(TextUtils.join("", messageParts))
                                                                    .build();
            sender.sendMessage(address, body);

            notifySendComplete(sendIntent);
            completeSendOperation(candidate);
        } catch (IOException e) {
            Log.w("MessageSender", e);
            abortSendOperation(candidate);
        } catch (InvalidNumberException e) {
            Log.w("MessageSender", e);
            abortSendOperation(candidate);
        } catch (UntrustedIdentityException e) {
            Log.w("MessageSender", e);
            abortSendOperation(candidate);
        }
    }

    private void completeSendOperation(OutgoingMessageCandidate candidate) {
        PendingResult pendingResult = candidate.getPendingResult();
        pendingResult.abortBroadcast();
        pendingResult.setResultCode(Activity.RESULT_CANCELED);
        pendingResult.finish();

        if (StatsUtils.isStatsActive(context)) {
            WhisperPreferences.setWasActive(context, true);
        }
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
                TextSecureAccountManager      manager = WhisperServiceFactory.createAccountManager(context);
                Log.w("MessageSender", "Getting contact token for: " + e164number);
                Optional<ContactTokenDetails> details = manager.getContact(e164number);

                if (details.isPresent()) {
                    directory.setNumber(details.get(), true);
                    return true;
                } else {
                    // FIXME: figure out what to do here
                    //contactTokenDetails = new ContactTokenDetails(contactToken);
                    //directory.setToken(contactTokenDetails, false);
                    return false;
                }
            } catch (IOException e1) {
                Log.w("MessageSender", e1);
                return false;
            }
        }
    }
}