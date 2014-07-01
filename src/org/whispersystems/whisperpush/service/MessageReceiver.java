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

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import com.android.internal.telephony.util.BlacklistUtils;
import org.whispersystems.textsecure.crypto.AttachmentCipherInputStream;
import org.whispersystems.textsecure.crypto.InvalidMessageException;
import org.whispersystems.textsecure.crypto.MasterSecret;
import org.whispersystems.textsecure.directory.Directory;
import org.whispersystems.textsecure.directory.NotInDirectoryException;
import org.whispersystems.textsecure.push.ContactTokenDetails;
import org.whispersystems.textsecure.push.IncomingPushMessage;
import org.whispersystems.textsecure.push.PushDestination;
import org.whispersystems.textsecure.push.PushMessageProtos.PushMessageContent;
import org.whispersystems.textsecure.push.PushMessageProtos.PushMessageContent.AttachmentPointer;
import org.whispersystems.textsecure.push.PushServiceSocket;
import org.whispersystems.textsecure.util.InvalidNumberException;
import org.whispersystems.textsecure.util.PhoneNumberFormatter;
import org.whispersystems.whisperpush.R;
import org.whispersystems.whisperpush.attachments.AttachmentManager;
import org.whispersystems.whisperpush.contacts.Contact;
import org.whispersystems.whisperpush.contacts.ContactsFactory;
import org.whispersystems.whisperpush.crypto.IdentityMismatchException;
import org.whispersystems.whisperpush.crypto.MasterSecretUtil;
import org.whispersystems.whisperpush.crypto.WhisperCipher;
import org.whispersystems.whisperpush.database.DatabaseFactory;
import org.whispersystems.whisperpush.db.CMDatabase;
import org.whispersystems.whisperpush.util.PushServiceSocketFactory;
import org.whispersystems.whisperpush.util.SmsServiceBridge;
import org.whispersystems.whisperpush.util.WhisperPreferences;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class MessageReceiver {

    private static final String TAG = MessageReceiver.class.getSimpleName();

    private final Context context;

    public MessageReceiver(Context context) {
        this.context = context;
    }

    public void handleReceiveMessage(IncomingPushMessage message) {
        if (message == null)
            return;

        if (isNumberBlackListed(message.getSource())) {
            MessageNotifier.notifyBlacklisted(context, message.getSource());
            return;
        }

        if (!hasActiveSession(message.getSource())) {
            Log.d(TAG, "New session detected for " + message.getSource());
            setActiveSession(message.getSource());
            MessageNotifier.notifyNewSessionIncoming(context, message);
        }
        updateDirectoryIfNecessary(message);

        try {
            PushMessageContent         content     = getPlaintext(message);
            List<Pair<String, String>> attachments = new LinkedList<Pair<String, String>>();

            if (content.getAttachmentsCount() > 0) {
                try {
                    attachments = retrieveAttachments(message.getRelay(), content.getAttachmentsList());
                } catch (IOException e) {
                    Log.w("MessageReceiver", e);
                    Contact contact = ContactsFactory.getContactFromNumber(context, message.getSource(), false);
                    MessageNotifier.notifyProblem(context, contact,
                            context.getString(R.string.MessageReceiver_unable_to_retrieve_encrypted_attachment_for_incoming_message));
                }
            }

            SmsServiceBridge.receivedPushMessage(context, message.getSource(), message.getDestinations(),
                    content.getBody(), attachments,
                    message.getTimestampMillis());
        } catch (IdentityMismatchException e) {
            Log.w("MessageReceiver", e);
            DatabaseFactory.getPendingApprovalDatabase(context).insert(message);
            MessageNotifier.updateNotifications(context);
        } catch (InvalidMessageException e) {
            Log.w("MessageReceiver", e);
            Contact contact = ContactsFactory.getContactFromNumber(context, message.getSource(), false);
            MessageNotifier.notifyProblem(context, contact,
                    context.getString(R.string.MessageReceiver_received_badly_encrypted_message));
        }
    }

    private PushMessageContent getPlaintext(IncomingPushMessage message)
            throws IdentityMismatchException, InvalidMessageException
    {
        try {
            MasterSecret    masterSecret    = MasterSecretUtil.getMasterSecret(context);
            String          localNumber     = WhisperPreferences.getLocalNumber(context);
            PushDestination pushDestination = PushDestination.create(context, localNumber, message.getSource());
            WhisperCipher   whisperCipher   = new WhisperCipher(context, masterSecret, pushDestination);

            return whisperCipher.getDecryptedMessage(message);
        } catch (InvalidNumberException e) {
            throw new InvalidMessageException(e);
        }
    }

    private List<Pair<String, String>> retrieveAttachments(String relay, List<AttachmentPointer> attachments)
            throws IOException, InvalidMessageException
    {
        AttachmentManager          attachmentManager = AttachmentManager.getInstance(context);
        PushServiceSocket          socket            = PushServiceSocketFactory.create(context);
        List<Pair<String, String>> results           = new LinkedList<Pair<String, String>>();

        for (AttachmentPointer attachment : attachments) {
            byte[]                      key              = attachment.getKey().toByteArray();
            File                        file             = socket.retrieveAttachment(relay, attachment.getId());
            AttachmentCipherInputStream attachmentStream = new AttachmentCipherInputStream(file, key);
            String                      storedToken      = attachmentManager.store(attachmentStream);

            file.delete();
            results.add(new Pair<String, String>(storedToken, attachment.getContentType()));
        }

        return results;
    }

    private void updateDirectoryIfNecessary(IncomingPushMessage message) {
        if (!isActiveNumber(message.getSource())) {
            Directory           directory           = Directory.getInstance(context);
            String              contactToken        = directory.getToken(message.getSource());
            String              relay               = message.getRelay();
            ContactTokenDetails contactTokenDetails = new ContactTokenDetails(contactToken, relay);

            directory.setToken(contactTokenDetails, true);
        }
    }

    private boolean isActiveNumber(String e164number) {
        try {
            return Directory.getInstance(context).isActiveNumber(e164number);
        } catch (NotInDirectoryException e) {
            return false;
        }
    }

    private boolean hasActiveSession(String e164number) {
        String token = Directory.getInstance(context).getToken(e164number);
        return CMDatabase.getInstance(context).hasActiveSession(token);
    }

    private void setActiveSession(String e164number) {
        String token = Directory.getInstance(context).getToken(e164number);
        CMDatabase.getInstance(context).setActiveSession(token);
    }

    private boolean isNumberBlackListed(String number) {
        int type = BlacklistUtils.isListed(context,
                PhoneNumberFormatter.formatNumberNational(number) , BlacklistUtils.BLOCK_MESSAGES);
        return type != BlacklistUtils.MATCH_NONE;
    }
}
