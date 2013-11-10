package org.whispersystems.whisperpush.crypto;

import android.content.Context;
import android.util.Log;

import com.google.protobuf.InvalidProtocolBufferException;

import org.whispersystems.textsecure.crypto.IdentityKey;
import org.whispersystems.textsecure.crypto.IdentityKeyPair;
import org.whispersystems.textsecure.crypto.InvalidKeyException;
import org.whispersystems.textsecure.crypto.InvalidMessageException;
import org.whispersystems.textsecure.crypto.InvalidVersionException;
import org.whispersystems.textsecure.crypto.KeyUtil;
import org.whispersystems.textsecure.crypto.MasterSecret;
import org.whispersystems.textsecure.crypto.MessageCipher;
import org.whispersystems.textsecure.crypto.protocol.CiphertextMessage;
import org.whispersystems.textsecure.crypto.protocol.PreKeyBundleMessage;
import org.whispersystems.textsecure.push.IncomingPushMessage;
import org.whispersystems.textsecure.push.PreKeyEntity;
import org.whispersystems.textsecure.push.PushBody;
import org.whispersystems.textsecure.push.PushDestination;
import org.whispersystems.textsecure.push.PushMessage;
import org.whispersystems.textsecure.push.PushMessageProtos.PushMessageContent;
import org.whispersystems.textsecure.push.PushServiceSocket;
import org.whispersystems.textsecure.storage.CanonicalRecipientAddress;
import org.whispersystems.textsecure.storage.InvalidKeyIdException;

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
        case PushMessage.TYPE_MESSAGE_PREKEY_BUNDLE: plaintext = getDecryptedMessageForNewSession(ciphertext);      break;
        case PushMessage.TYPE_MESSAGE_CIPHERTEXT:    plaintext = getDecryptedMessageForExistingSession(ciphertext); break;
        case PushMessage.TYPE_MESSAGE_PLAINTEXT:     plaintext = ciphertext;                                        break;
        default:                                     throw new InvalidVersionException("Unknown type: " + message.getType());
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

  public PushBody getEncryptedMessage(PushServiceSocket socket,
                                                   byte[] plaintext)
      throws IOException
  {
    if (KeyUtil.isNonPrekeySessionFor(context, masterSecret, address)) {
      Log.w("WhisperCipher", "Encrypting standard ciphertext message...");
      byte[] ciphertext = getEncryptedMessageForExistingSession(address, plaintext);
      return new PushBody(PushMessage.TYPE_MESSAGE_CIPHERTEXT, ciphertext);
    } else if (KeyUtil.isSessionFor(context, address)) {
      Log.w("WhisperCipher", "Encrypting prekeybundle ciphertext message for existing session...");
      byte[] ciphertext = getEncryptedPrekeyBundleMessageForExistingSession(address, plaintext);
      return new PushBody(PushMessage.TYPE_MESSAGE_PREKEY_BUNDLE, ciphertext);
    } else {
      Log.w("WhisperCipher", "Encrypting prekeybundle ciphertext message for new session...");
      byte[] ciphertext = getEncryptedPrekeyBundleMessageForNewSession(socket, address, pushDestination, plaintext);
      return new PushBody(PushMessage.TYPE_MESSAGE_PREKEY_BUNDLE, ciphertext);
    }
  }

  private byte[] getDecryptedMessageForNewSession(byte[] ciphertext)
      throws InvalidVersionException, InvalidKeyException,
      InvalidKeyIdException, IdentityMismatchException, InvalidMessageException
  {
    KeyExchangeProcessor processor     = new KeyExchangeProcessor(context, masterSecret, address);
    PreKeyBundleMessage  bundleMessage = new PreKeyBundleMessage(ciphertext);

    if (processor.isTrusted(bundleMessage)) {
      Log.w("WhisperCipher", "Trusted, processing...");
      processor.processKeyExchangeMessage(bundleMessage);
      return getDecryptedMessageForExistingSession(bundleMessage.getBundledMessage().serialize());
    }

    throw new IdentityMismatchException("Bad identity key!");
  }

  private byte[] getDecryptedMessageForExistingSession(byte[] ciphertext)
      throws InvalidMessageException
  {
    IdentityKeyPair identityKeyPair = IdentityKeyUtil.getIdentityKeyPair(context, masterSecret);
    MessageCipher   messageCipher   = new MessageCipher(context, masterSecret, identityKeyPair);
    return messageCipher.decrypt(address, ciphertext);
  }

  private byte[] getEncryptedPrekeyBundleMessageForExistingSession(CanonicalRecipientAddress address,
                                                                   byte[] plaintext)
  {
    IdentityKeyPair   identityKeyPair = IdentityKeyUtil.getIdentityKeyPair(context, masterSecret);
    IdentityKey       identityKey     = identityKeyPair.getPublicKey();

    MessageCipher     message         = new MessageCipher(context, masterSecret, identityKeyPair);
    CiphertextMessage bundledMessage  = message.encrypt(address, plaintext);

    PreKeyBundleMessage preKeyBundleMessage = new PreKeyBundleMessage(bundledMessage, identityKey);
    return preKeyBundleMessage.serialize();
  }

  private byte[] getEncryptedPrekeyBundleMessageForNewSession(PushServiceSocket socket,
                                                              CanonicalRecipientAddress address,
                                                              PushDestination pushDestination,
                                                              byte[] plaintext)
      throws IOException
  {
    IdentityKeyPair      identityKeyPair = IdentityKeyUtil.getIdentityKeyPair(context, masterSecret);
    IdentityKey          identityKey     = identityKeyPair.getPublicKey();
    PreKeyEntity         preKey          = socket.getPreKey(pushDestination);
    KeyExchangeProcessor processor       = new KeyExchangeProcessor(context, masterSecret, address);

    if (processor.isTrusted(preKey)) {
      processor.processKeyExchangeMessage(preKey);
    } else {
      throw new IdentityMismatchException("Retrieved identity is untrusted!");
    }

    MessageCipher       messageCipher       = new MessageCipher(context, masterSecret, identityKeyPair);
    CiphertextMessage   bundledMessage      = messageCipher.encrypt(address, plaintext);
    PreKeyBundleMessage preKeyBundleMessage = new PreKeyBundleMessage(bundledMessage, identityKey);

    return preKeyBundleMessage.serialize();
  }

  private byte[] getEncryptedMessageForExistingSession(CanonicalRecipientAddress address, byte[] plaintext)
      throws IOException
  {
    IdentityKeyPair   identityKeyPair = IdentityKeyUtil.getIdentityKeyPair(context, masterSecret);
    MessageCipher     messageCipher   = new MessageCipher(context, masterSecret, identityKeyPair);
    CiphertextMessage message         = messageCipher.encrypt(address, plaintext);

    return message.serialize();
  }

}
