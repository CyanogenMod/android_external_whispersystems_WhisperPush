package org.whispersystems.whisperpush.crypto;

import android.content.Context;

import org.whispersystems.textsecure.storage.CanonicalRecipientAddress;
import org.whispersystems.whisperpush.database.DatabaseFactory;

public class MessagePeer implements CanonicalRecipientAddress {

  private final long canonicalAddress;

  public MessagePeer(Context context, String canonicalPeerNumber) {
    this.canonicalAddress = DatabaseFactory.getAddressDatabase(context)
                                           .getCanonicalAddressFromNumber(canonicalPeerNumber);
  }

  @Override
  public long getCanonicalAddress(Context context) {
    return canonicalAddress;
  }
}
