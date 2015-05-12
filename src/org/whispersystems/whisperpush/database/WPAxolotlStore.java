package org.whispersystems.whisperpush.database;

import java.util.List;

import org.whispersystems.libaxolotl.AxolotlAddress;
import org.whispersystems.libaxolotl.IdentityKey;
import org.whispersystems.libaxolotl.IdentityKeyPair;
import org.whispersystems.libaxolotl.InvalidKeyIdException;
import org.whispersystems.libaxolotl.state.AxolotlStore;
import org.whispersystems.libaxolotl.state.PreKeyRecord;
import org.whispersystems.libaxolotl.state.SessionRecord;
import org.whispersystems.libaxolotl.state.SignedPreKeyRecord;
import org.whispersystems.libaxolotl.state.SignedPreKeyStore;
import org.whispersystems.whisperpush.crypto.MasterSecret;
import org.whispersystems.whisperpush.crypto.MasterSecretUtil;

import android.content.Context;

public class WPAxolotlStore implements AxolotlStore {
    private static WPAxolotlStore instance;

    public synchronized static WPAxolotlStore getInstance(Context context) {
        if (instance == null) {
            instance = new WPAxolotlStore(context, MasterSecretUtil.getMasterSecret(context));
        }

        return instance;
    }

    private final WPPreKeyStore       preKeyStore;
    private final SignedPreKeyStore   signedPreKeyStore;
    private final WPIdentityKeyStore  identityKeyStore;
    private final WPSessionStore      sessionStore;

    public WPAxolotlStore(Context context, MasterSecret masterSecret) {
        this.preKeyStore = new WPPreKeyStore(context, masterSecret);
        this.signedPreKeyStore = new WPPreKeyStore(context, masterSecret);
        this.identityKeyStore = new WPIdentityKeyStore(context, masterSecret);
        this.sessionStore = new WPSessionStore(context, masterSecret);
    }

    @Override
    public IdentityKeyPair getIdentityKeyPair() {
        return identityKeyStore.getIdentityKeyPair();
    }

    @Override
    public int getLocalRegistrationId() {
        return identityKeyStore.getLocalRegistrationId();
    }

    @Override
    public void saveIdentity(String number, IdentityKey identityKey) {
        identityKeyStore.saveIdentity(number, identityKey);
    }

    @Override
    public boolean isTrustedIdentity(String number, IdentityKey identityKey) {
        return identityKeyStore.isTrustedIdentity(number, identityKey);
    }

    @Override
    public PreKeyRecord loadPreKey(int preKeyId) throws InvalidKeyIdException {
        return preKeyStore.loadPreKey(preKeyId);
    }

    @Override
    public void storePreKey(int preKeyId, PreKeyRecord record) {
        preKeyStore.storePreKey(preKeyId, record);
    }

    @Override
    public boolean containsPreKey(int preKeyId) {
        return preKeyStore.containsPreKey(preKeyId);
    }

    @Override
    public void removePreKey(int preKeyId) {
        preKeyStore.removePreKey(preKeyId);
    }

    @Override
    public SessionRecord loadSession(AxolotlAddress axolotlAddress) {
        return sessionStore.loadSession(axolotlAddress);
    }

    @Override
    public List<Integer> getSubDeviceSessions(String number) {
        return sessionStore.getSubDeviceSessions(number);
    }

    @Override
    public void storeSession(AxolotlAddress axolotlAddress, SessionRecord record) {
        sessionStore.storeSession(axolotlAddress, record);
    }

    @Override
    public boolean containsSession(AxolotlAddress axolotlAddress) {
        return sessionStore.containsSession(axolotlAddress);
    }

    @Override
    public void deleteSession(AxolotlAddress axolotlAddress) {
        sessionStore.deleteSession(axolotlAddress);
    }

    @Override
    public void deleteAllSessions(String number) {
        sessionStore.deleteAllSessions(number);
    }

    @Override
    public SignedPreKeyRecord loadSignedPreKey(int signedPreKeyId)
            throws InvalidKeyIdException {
        return signedPreKeyStore.loadSignedPreKey(signedPreKeyId);
    }

    @Override
    public List<SignedPreKeyRecord> loadSignedPreKeys() {
        return signedPreKeyStore.loadSignedPreKeys();
    }

    @Override
    public void storeSignedPreKey(int signedPreKeyId, SignedPreKeyRecord record) {
        signedPreKeyStore.storeSignedPreKey(signedPreKeyId, record);
    }

    @Override
    public boolean containsSignedPreKey(int signedPreKeyId) {
        return signedPreKeyStore.containsSignedPreKey(signedPreKeyId);
    }

    @Override
    public void removeSignedPreKey(int signedPreKeyId) {
        signedPreKeyStore.removeSignedPreKey(signedPreKeyId);
    }
}