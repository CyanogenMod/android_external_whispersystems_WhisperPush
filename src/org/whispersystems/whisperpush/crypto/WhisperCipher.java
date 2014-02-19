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
package org.whispersystems.whisperpush.crypto;

import android.content.Context;
import android.util.Log;

import com.google.protobuf.InvalidProtocolBufferException;

import org.whispersystems.textsecure.crypto.InvalidKeyException;
import org.whispersystems.textsecure.crypto.InvalidMessageException;
import org.whispersystems.textsecure.crypto.InvalidVersionException;
import org.whispersystems.textsecure.crypto.MasterSecret;
import org.whispersystems.textsecure.crypto.SessionCipher;
import org.whispersystems.textsecure.crypto.protocol.CiphertextMessage;
import org.whispersystems.textsecure.crypto.protocol.PreKeyWhisperMessage;
import org.whispersystems.textsecure.push.IncomingPushMessage;
import org.whispersystems.textsecure.push.PreKeyEntity;
import org.whispersystems.textsecure.push.PushBody;
import org.whispersystems.textsecure.push.PushDestination;
import org.whispersystems.textsecure.push.PushMessage;
import org.whispersystems.textsecure.push.PushMessageProtos.PushMessageContent;
import org.whispersystems.textsecure.push.PushServiceSocket;
import org.whispersystems.textsecure.storage.CanonicalRecipientAddress;
import org.whispersystems.textsecure.storage.InvalidKeyIdException;
import org.whispersystems.textsecure.storage.Session;

import java.io.IOException;

public class WhisperCipher {

    private final Context context;
    private final MasterSecret masterSecret;
    private final CanonicalRecipientAddress address;
    private final PushDestination pushDestination;

    public WhisperCipher(Context context, MasterSecret masterSecret, PushDestination pushDestination) {
        this.context         = context.getApplicationContext();
        this.masterSecret    = masterSecret;
        this.address         = new MessagePeer(context, pushDestination.getNumber());
        this.pushDestination = pushDestination;
    }

    public PushMessageContent getDecryptedMessage(IncomingPushMessage message)
            throws IdentityMismatchException, InvalidMessageException
    {
        try {
            Log.w("WhisperCipher", "Message type: " + message.getType());

            byte[] ciphertext = message.getBody();
            byte[] plaintext;

            switch (message.getType()) {
                case PushMessage.TYPE_MESSAGE_PREKEY_BUNDLE:
                    plaintext = getDecryptedMessageForNewSession(ciphertext);      break;
                case PushMessage.TYPE_MESSAGE_CIPHERTEXT:
                    plaintext = getDecryptedMessageForExistingSession(ciphertext); break;
                case PushMessage.TYPE_MESSAGE_PLAINTEXT:
                    plaintext = ciphertext;                                        break;
                default:
                    throw new InvalidVersionException("Unknown type: " + message.getType());
            }

            return PushMessageContent.parseFrom(plaintext);
        } catch (InvalidKeyException e) {
            throw new InvalidMessageException(e);
        } catch (InvalidVersionException e) {
            throw new InvalidMessageException(e);
        } catch (InvalidKeyIdException e) {
            throw new InvalidMessageException(e);
        } catch (InvalidProtocolBufferException e) {
            throw new InvalidMessageException(e);
        }
    }

    public PushBody getEncryptedMessage(PushServiceSocket socket, byte[] plaintext)
            throws IOException
    {
        if (Session.hasSession(context, masterSecret, address)) {
            Log.w("WhisperCipher", "Encrypting ciphertext message for existing session...");
            return getEncryptedMessageForExistingSession(address, plaintext);
        } else {
            Log.w("WhisperCipher", "Encrypting prekeybundle ciphertext message for new session...");
            return getEncryptedMessageForNewSession(socket, address, pushDestination, plaintext);
        }
    }

    private byte[] getDecryptedMessageForNewSession(byte[] ciphertext)
            throws InvalidVersionException, InvalidKeyException,
            InvalidKeyIdException, IdentityMismatchException, InvalidMessageException
    {
        KeyExchangeProcessor processor     = new KeyExchangeProcessor(context, masterSecret, address);
        PreKeyWhisperMessage bundleMessage = new PreKeyWhisperMessage(ciphertext);

        if (processor.isTrusted(bundleMessage)) {
            Log.w("WhisperCipher", "Trusted, processing...");
            processor.processKeyExchangeMessage(bundleMessage);
            return getDecryptedMessageForExistingSession(bundleMessage.getWhisperMessage().serialize());
        }

        throw new IdentityMismatchException("Bad identity key!");
    }

    private byte[] getDecryptedMessageForExistingSession(byte[] ciphertext)
            throws InvalidMessageException
    {
        SessionCipher sessionCipher = SessionCipher.createFor(context, masterSecret, address);
        return sessionCipher.decrypt(ciphertext);
    }
    private PushBody getEncryptedMessageForNewSession(PushServiceSocket socket,
                                                      CanonicalRecipientAddress address,
                                                      PushDestination pushDestination,
                                                      byte[] plaintext)
            throws IOException
    {
        try {
            PreKeyEntity         preKey    = socket.getPreKey(pushDestination);
            KeyExchangeProcessor processor = new KeyExchangeProcessor(context, masterSecret, address);

            if (processor.isTrusted(preKey)) {
                processor.processKeyExchangeMessage(preKey);
            } else {
                throw new IdentityMismatchException("Retrieved identity is untrusted!");
            }

            return getEncryptedMessageForExistingSession(address, plaintext);
        } catch (InvalidKeyException e) {
            throw new IOException(e);
        }
    }

    private PushBody getEncryptedMessageForExistingSession(CanonicalRecipientAddress address,
                                                           byte[] plaintext)
            throws IOException
    {
        SessionCipher     sessionCipher = SessionCipher.createFor(context, masterSecret, address);
        CiphertextMessage message       = sessionCipher.encrypt(plaintext);

        if (message.getType() == CiphertextMessage.PREKEY_WHISPER_TYPE) {
            return new PushBody(PushMessage.TYPE_MESSAGE_PREKEY_BUNDLE, message.serialize());
        } else if (message.getType() == CiphertextMessage.CURRENT_WHISPER_TYPE) {
            return new PushBody(PushMessage.TYPE_MESSAGE_CIPHERTEXT, message.serialize());
        } else {
            throw new AssertionError("Unknown ciphertext type: " + message.getType());
        }

    }

}
