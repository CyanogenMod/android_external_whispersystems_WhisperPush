package org.whispersystems.whisperpush.database;

import android.content.Context;

import org.whispersystems.libaxolotl.IdentityKey;
import org.whispersystems.libaxolotl.IdentityKeyPair;
import org.whispersystems.libaxolotl.state.IdentityKeyStore;
import org.whispersystems.whisperpush.crypto.IdentityKeyUtil;
import org.whispersystems.whisperpush.crypto.MasterSecret;
import org.whispersystems.whisperpush.crypto.MessagePeer;
import org.whispersystems.whisperpush.util.WhisperPreferences;

public class WPIdentityKeyStore implements IdentityKeyStore {

  private final Context      context;
  private final MasterSecret masterSecret;

  public WPIdentityKeyStore(Context context, MasterSecret masterSecret) {
    this.context      = context;
    this.masterSecret = masterSecret;
  }

  @Override
  public IdentityKeyPair getIdentityKeyPair() {
    return IdentityKeyUtil.getIdentityKeyPair(context, masterSecret);
  }

  @Override
  public int getLocalRegistrationId() {
    return WhisperPreferences.getInstallId(context);
  }

  @Override
  public void saveIdentity(String number, IdentityKey identityKey) {
    MessagePeer recipient = new MessagePeer(context, number);
    DatabaseFactory.getIdentityDatabase(context).saveIdentity(masterSecret, recipient, identityKey);
  }

  @Override
  public boolean isTrustedIdentity(String number, IdentityKey identityKey) {
    MessagePeer recipient = new MessagePeer(context, number);
    return DatabaseFactory.getIdentityDatabase(context)
                          .isValidIdentity(masterSecret, recipient, identityKey);
  }
}