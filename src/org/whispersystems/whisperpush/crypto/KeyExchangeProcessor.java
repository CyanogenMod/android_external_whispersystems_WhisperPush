package org.whispersystems.whisperpush.crypto;

import android.content.Context;
import android.util.Log;

import org.whispersystems.textsecure.crypto.IdentityKey;
import org.whispersystems.textsecure.crypto.KeyPair;
import org.whispersystems.textsecure.crypto.KeyUtil;
import org.whispersystems.textsecure.crypto.MasterSecret;
import org.whispersystems.textsecure.crypto.MessageCipher;
import org.whispersystems.textsecure.crypto.PublicKey;
import org.whispersystems.textsecure.crypto.protocol.PreKeyBundleMessage;
import org.whispersystems.textsecure.push.PreKeyEntity;
import org.whispersystems.textsecure.storage.CanonicalRecipientAddress;
import org.whispersystems.textsecure.storage.InvalidKeyIdException;
import org.whispersystems.textsecure.storage.LocalKeyRecord;
import org.whispersystems.textsecure.storage.PreKeyRecord;
import org.whispersystems.textsecure.storage.RemoteKeyRecord;
import org.whispersystems.textsecure.storage.SessionRecord;
import org.whispersystems.textsecure.util.Medium;
import org.whispersystems.whisperpush.database.DatabaseFactory;

public class KeyExchangeProcessor {

  private Context context;
  private CanonicalRecipientAddress address;
  private MasterSecret masterSecret;
  private LocalKeyRecord localKeyRecord;
  private RemoteKeyRecord remoteKeyRecord;
  private SessionRecord sessionRecord;

  public KeyExchangeProcessor(Context context, MasterSecret masterSecret,
                              CanonicalRecipientAddress address)
  {
    this.context      = context;
    this.address      = address;
    this.masterSecret = masterSecret;

    this.remoteKeyRecord = new RemoteKeyRecord(context, address);
    this.localKeyRecord  = new LocalKeyRecord(context, masterSecret, address);
    this.sessionRecord   = new SessionRecord(context, masterSecret, address);
  }

  public boolean isTrusted(PreKeyEntity message) {
    return isTrusted(message.getIdentityKey());
  }

  public boolean isTrusted(PreKeyBundleMessage message) {
    return isTrusted(message.getIdentityKey());
  }

  public boolean isTrusted(IdentityKey identityKey) {
    return DatabaseFactory.getIdentityDatabase(context).isValidIdentity(masterSecret,
                                                                        address,
                                                                        identityKey);
  }

  public void processKeyExchangeMessage(PreKeyBundleMessage message) throws InvalidKeyIdException {
    int preKeyId               = message.getPreKeyId();
    PublicKey remoteKey      = message.getPublicKey();
    IdentityKey remoteIdentity = message.getIdentityKey();

    Log.w("KeyExchangeProcessor", "Received pre-key with remote key ID: " + remoteKey.getId());
    Log.w("KeyExchangeProcessor", "Received pre-key with local key ID: " + preKeyId);

    if (!PreKeyRecord.hasRecord(context, preKeyId) && KeyUtil.isSessionFor(context, address)) {
      Log.w("KeyExchangeProcessor", "We've already processed the prekey part, letting bundled message fall through...");
      return;
    }

    if (!PreKeyRecord.hasRecord(context, preKeyId))
      throw new InvalidKeyIdException("No such prekey: " + preKeyId);

    PreKeyRecord preKeyRecord = new PreKeyRecord(context, masterSecret, preKeyId);
    KeyPair      preKeyPair   = new KeyPair(preKeyId, preKeyRecord.getKeyPair().getKeyPair(), masterSecret);

    localKeyRecord.setCurrentKeyPair(preKeyPair);
    localKeyRecord.setNextKeyPair(preKeyPair);

    remoteKeyRecord.setCurrentRemoteKey(remoteKey);
    remoteKeyRecord.setLastRemoteKey(remoteKey);

    sessionRecord.setSessionId(localKeyRecord.getCurrentKeyPair().getPublicKey().getFingerprintBytes(),
                               remoteKeyRecord.getCurrentRemoteKey().getFingerprintBytes());
    sessionRecord.setIdentityKey(remoteIdentity);
    sessionRecord.setSessionVersion(Math.min(message.getSupportedVersion(), MessageCipher.SUPPORTED_VERSION));

    localKeyRecord.save();
    remoteKeyRecord.save();
    sessionRecord.save();

    if (preKeyId != Medium.MAX_VALUE) {
      PreKeyRecord.delete(context, preKeyId);
    }

    DatabaseFactory.getIdentityDatabase(context)
                   .saveIdentity(masterSecret, address, remoteIdentity);
  }

  public void processKeyExchangeMessage(PreKeyEntity message) {
    PublicKey remoteKey = new PublicKey(message.getKeyId(), message.getPublicKey());

    remoteKeyRecord.setCurrentRemoteKey(remoteKey);
    remoteKeyRecord.setLastRemoteKey(remoteKey);
    remoteKeyRecord.save();

    localKeyRecord = KeyUtil.initializeRecordFor(address, context, masterSecret);
    localKeyRecord.setNextKeyPair(localKeyRecord.getCurrentKeyPair());
    localKeyRecord.save();

    sessionRecord.setSessionId(localKeyRecord.getCurrentKeyPair().getPublicKey().getFingerprintBytes(),
                               remoteKeyRecord.getCurrentRemoteKey().getFingerprintBytes());
    sessionRecord.setIdentityKey(message.getIdentityKey());
    sessionRecord.setSessionVersion(MessageCipher.SUPPORTED_VERSION);
    sessionRecord.setPrekeyBundleRequired(true);
    sessionRecord.save();

    DatabaseFactory.getIdentityDatabase(context)
                   .saveIdentity(masterSecret, address, message.getIdentityKey());
  }
}
