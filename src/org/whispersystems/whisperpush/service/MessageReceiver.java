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

import org.whispersystems.libaxolotl.DuplicateMessageException;
import org.whispersystems.libaxolotl.InvalidKeyException;
import org.whispersystems.libaxolotl.InvalidKeyIdException;
import org.whispersystems.libaxolotl.InvalidMessageException;
import org.whispersystems.libaxolotl.InvalidVersionException;
import org.whispersystems.libaxolotl.LegacyMessageException;
import org.whispersystems.libaxolotl.NoSessionException;
import org.whispersystems.libaxolotl.UntrustedIdentityException;
import org.whispersystems.libaxolotl.util.guava.Optional;
import org.whispersystems.textsecure.api.crypto.AttachmentCipherInputStream;
import org.whispersystems.textsecure.api.crypto.TextSecureCipher;
import org.whispersystems.textsecure.api.messages.TextSecureAttachment;
import org.whispersystems.textsecure.api.messages.TextSecureAttachmentStream;
import org.whispersystems.textsecure.api.messages.TextSecureEnvelope;
import org.whispersystems.textsecure.api.messages.TextSecureMessage;
import org.whispersystems.textsecure.api.push.ContactTokenDetails;
import org.whispersystems.textsecure.api.util.InvalidNumberException;
import org.whispersystems.textsecure.api.util.PhoneNumberFormatter;
import org.whispersystems.textsecure.internal.push.PushMessageProtos.PushMessageContent;
import org.whispersystems.textsecure.internal.push.PushMessageProtos.PushMessageContent.AttachmentPointer;
import org.whispersystems.textsecure.internal.push.PushServiceSocket;
import org.whispersystems.whisperpush.R;
import org.whispersystems.whisperpush.attachments.AttachmentManager;
import org.whispersystems.whisperpush.contacts.Contact;
import org.whispersystems.whisperpush.contacts.ContactsFactory;
import org.whispersystems.whisperpush.crypto.IdentityMismatchException;
import org.whispersystems.whisperpush.crypto.MasterSecretUtil;
import org.whispersystems.whisperpush.database.DatabaseFactory;
import org.whispersystems.whisperpush.database.WPAxolotlStore;
import org.whispersystems.whisperpush.db.CMDatabase;
import org.whispersystems.whisperpush.directory.Directory;
import org.whispersystems.whisperpush.directory.NotInDirectoryException;
import org.whispersystems.whisperpush.util.SmsServiceBridge;
import org.whispersystems.whisperpush.util.WhisperPreferences;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

public class MessageReceiver {

    private static final String TAG = MessageReceiver.class.getSimpleName();

    private final Context context;

    public MessageReceiver(Context context) {
        this.context = context;
    }
    
    public void handleReceiveMessage(String message) {
        try {
            String             sessionKey = WhisperPreferences.getSignalingKey(context);
            TextSecureEnvelope envelope   = new TextSecureEnvelope(message, sessionKey);

            handleEnvelope(envelope, true);
        } catch (IOException e) {
            Log.w(TAG, e);
            MessageNotifier.notifyProblemAndUnregister(context,
                context.getString(R.string.GcmReceiver_error),
                context.getString(R.string.GcmReceiver_invalid_push_message)
                + "\n" +
                context.getString(R.string.GcmReceiver_received_badly_formatted_push_message));
        } catch (InvalidVersionException e) {
            Log.w(TAG, e);
            MessageNotifier.notifyProblem(context,
                context.getString(R.string.GcmReceiver_error),
                context.getString(R.string.GcmReceiver_received_badly_formatted_push_message)
                + "\n" +
                context.getString(R.string.GcmReceiver_received_push_message_with_unknown_version));
        }
    }

    public void handleEnvelope(TextSecureEnvelope envelope, boolean sendExplicitReceipt) {
        if (!isActiveNumber(context, envelope.getSource())) {
          Directory directory                     = Directory.getInstance(context);
          ContactTokenDetails contactTokenDetails = new ContactTokenDetails();
          contactTokenDetails.setNumber(envelope.getSource());

          directory.setToken(contactTokenDetails, true);
        }

        if (envelope.isReceipt()) handleReceipt(envelope);
        else                      handleMessage(envelope, sendExplicitReceipt);
    }

    private boolean isActiveNumber(Context context, String e164number) {
        try {
            return Directory.getInstance(context).isActiveNumber(e164number);
        } catch (NotInDirectoryException e) {
            return false;
        }
    }

    private void handleReceipt(TextSecureEnvelope envelope) {
        Log.w(TAG, String.format("Received receipt: (XXXXX, %d)", envelope.getTimestamp()));
        // FIXME: don't know what to do with receipt
        //DatabaseFactory.getMmsSmsDatabase(context).incrementDeliveryReceiptCount(envelope.getSource(),
        //                                                                         envelope.getTimestamp());
    }

    public void handleMessage(TextSecureEnvelope message, boolean sendExplicitReceipt) {
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
            TextSecureMessage          content     = getPlaintext(message);
            List<Pair<String, String>> attachments = new LinkedList<Pair<String, String>>();
            
            Optional<List<TextSecureAttachment>> attach = content.getAttachments();

            if (attach.isPresent()) {
                try {
                    attachments = retrieveAttachments(message.getRelay(), attach.get());
                } catch (IOException e) {
                    Log.w("MessageReceiver", e);
                    Contact contact = ContactsFactory.getContactFromNumber(context, message.getSource(), false);
                    MessageNotifier.notifyProblem(context, contact,
                            context.getString(R.string.MessageReceiver_unable_to_retrieve_encrypted_attachment_for_incoming_message));
                }
            }

            SmsServiceBridge.receivedPushMessage(context, message.getSource(), content.getBody(),
                                                 attachments, message.getTimestamp());
        } catch (IdentityMismatchException e) {
            Log.w(TAG, e);
            DatabaseFactory.getPendingApprovalDatabase(context).insert(message);
            MessageNotifier.updateNotifications(context);
        } catch (InvalidMessageException e) {
            Log.w(TAG, e);
            Contact contact = ContactsFactory.getContactFromNumber(context, message.getSource(), false);
            MessageNotifier.notifyProblem(context, contact,
                    context.getString(R.string.MessageReceiver_received_badly_encrypted_message));
        }
    }

    private TextSecureMessage getPlaintext(TextSecureEnvelope envelope)
            throws IdentityMismatchException, InvalidMessageException
    {
        try {
            WPAxolotlStore store = WPAxolotlStore.getInstance(context);
            TextSecureCipher cipher = new TextSecureCipher(store);
            return cipher.decrypt(envelope);
        } catch (Exception e) {
            if (e instanceof IdentityMismatchException) {
                throw (IdentityMismatchException)e;
            } else {
                // FIXME: not the best error handling approach?
                throw new InvalidMessageException(e.getMessage(), e);
            }
        }
    }

    private List<Pair<String, String>> retrieveAttachments(String relay, List<TextSecureAttachment> list)
            throws IOException, InvalidMessageException
    {
        AttachmentManager          attachmentManager = AttachmentManager.getInstance(context);
        List<Pair<String, String>> results           = new LinkedList<Pair<String, String>>();

        for (TextSecureAttachment attachment : list) {
            InputStream stream      = attachment.asStream().getInputStream();
            String      storedToken = attachmentManager.store(stream);
            results.add(Pair.create(storedToken, attachment.getContentType()));
        }

        return results;
    }

    private void updateDirectoryIfNecessary(TextSecureEnvelope message) {
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
                PhoneNumberFormatter.formatNumberNational(number), BlacklistUtils.BLOCK_MESSAGES);
        return type != BlacklistUtils.MATCH_NONE;
    }
}
