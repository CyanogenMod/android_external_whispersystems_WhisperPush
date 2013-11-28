package org.whispersystems.whisperpush.crypto;

import android.content.Context;
import android.util.Log;

import org.whispersystems.textsecure.crypto.IdentityKey;
import org.whispersystems.textsecure.crypto.IdentityKeyPair;
import org.whispersystems.textsecure.crypto.InvalidKeyException;
import org.whispersystems.textsecure.crypto.MasterSecret;
import org.whispersystems.textsecure.crypto.ecc.Curve;
import org.whispersystems.textsecure.crypto.ecc.ECKeyPair;
import org.whispersystems.textsecure.crypto.ecc.ECPublicKey;
import org.whispersystems.textsecure.crypto.protocol.PreKeyWhisperMessage;
import org.whispersystems.textsecure.crypto.ratchet.RatchetingSession;
import org.whispersystems.textsecure.push.PreKeyEntity;
import org.whispersystems.textsecure.storage.CanonicalRecipientAddress;
import org.whispersystems.textsecure.storage.InvalidKeyIdException;
import org.whispersystems.textsecure.storage.PreKeyRecord;
import org.whispersystems.textsecure.storage.Session;
import org.whispersystems.textsecure.storage.SessionRecordV2;
import org.whispersystems.textsecure.util.Medium;
import org.whispersystems.whisperpush.database.DatabaseFactory;

public class KeyExchangeProcessor {

  private Context                   context;
  private CanonicalRecipientAddress address;
  private MasterSecret              masterSecret;
  private SessionRecordV2           sessionRecord;

  public KeyExchangeProcessor(Context context, MasterSecret masterSecret,
                              CanonicalRecipientAddress address)
  {
    this.context       = context;
    this.address       = address;
    this.masterSecret  = masterSecret;
    this.sessionRecord = new SessionRecordV2(context, masterSecret, address);
  }

  public boolean isTrusted(PreKeyEntity message) {
    return isTrusted(message.getIdentityKey());
  }

  public boolean isTrusted(PreKeyWhisperMessage message) {
    return isTrusted(message.getIdentityKey());
  }

  public boolean isTrusted(IdentityKey identityKey) {
    return DatabaseFactory.getIdentityDatabase(context).isValidIdentity(masterSecret,
                                                                        address,
                                                                        identityKey);
  }

  public void processKeyExchangeMessage(PreKeyWhisperMessage message)
      throws InvalidKeyIdException, InvalidKeyException
  {
    int         preKeyId          = message.getPreKeyId();
    ECPublicKey theirBaseKey      = message.getBaseKey();
    ECPublicKey theirEphemeralKey = message.getWhisperMessage().getSenderEphemeral();
    IdentityKey theirIdentityKey  = message.getIdentityKey();

    Log.w("KeyExchangeProcessor", "Received pre-key with local key ID: " + preKeyId);

    if (!PreKeyRecord.hasRecord(context, preKeyId) && Session.hasSession(context, masterSecret, address)) {
      Log.w("KeyExchangeProcessor", "We've already processed the prekey part, letting bundled message fall through...");
      return;
    }

    if (!PreKeyRecord.hasRecord(context, preKeyId))
      throw new InvalidKeyIdException("No such prekey: " + preKeyId);

    PreKeyRecord    preKeyRecord    = new PreKeyRecord(context, masterSecret, preKeyId);
    ECKeyPair       ourBaseKey      = preKeyRecord.getKeyPair();
    ECKeyPair       ourEphemeralKey = ourBaseKey;
    IdentityKeyPair ourIdentityKey  = IdentityKeyUtil.getIdentityKeyPair(context, masterSecret);

    sessionRecord.clear();

    RatchetingSession.initializeSession(sessionRecord, ourBaseKey, theirBaseKey, ourEphemeralKey,
                                        theirEphemeralKey, ourIdentityKey, theirIdentityKey);

    sessionRecord.save();

    if (preKeyId != Medium.MAX_VALUE) {
      PreKeyRecord.delete(context, preKeyId);
    }

    DatabaseFactory.getIdentityDatabase(context)
                   .saveIdentity(masterSecret, address, theirIdentityKey);
  }

  public void processKeyExchangeMessage(PreKeyEntity message)
      throws InvalidKeyException
  {
    ECKeyPair       ourBaseKey        = Curve.generateKeyPairForSession(2);
    ECKeyPair       ourEphemeralKey   = Curve.generateKeyPairForSession(2);
    ECPublicKey     theirBaseKey      = message.getPublicKey();
    ECPublicKey     theirEphemeralKey = theirBaseKey;
    IdentityKey     theirIdentityKey  = message.getIdentityKey();
    IdentityKeyPair ourIdentityKey    = IdentityKeyUtil.getIdentityKeyPair(context, masterSecret);

    sessionRecord.clear();

    RatchetingSession.initializeSession(sessionRecord, ourBaseKey, theirBaseKey, ourEphemeralKey,
                                        theirEphemeralKey, ourIdentityKey, theirIdentityKey);

    sessionRecord.setPendingPreKey(message.getKeyId(), ourBaseKey.getPublicKey());
    sessionRecord.save();

    DatabaseFactory.getIdentityDatabase(context)
                   .saveIdentity(masterSecret, address, message.getIdentityKey());
  }

}
