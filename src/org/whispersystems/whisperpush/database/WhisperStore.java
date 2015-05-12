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

import android.content.Context;

public class WhisperStore implements AxolotlStore {
    private static WhisperStore instance;

    public synchronized static WhisperStore getInstance(Context context) {
        if (instance == null) {
            instance = new WhisperStore(context);
        }

        return instance;
    }

    public WhisperStore(Context context) {
        // TODO Auto-generated constructor stub
    }
    
    // FIXME: much to implement!
    @Override
    public IdentityKeyPair getIdentityKeyPair() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getLocalRegistrationId() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean isTrustedIdentity(String arg0, IdentityKey arg1) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void saveIdentity(String arg0, IdentityKey arg1) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean containsPreKey(int arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public PreKeyRecord loadPreKey(int arg0) throws InvalidKeyIdException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void removePreKey(int arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void storePreKey(int arg0, PreKeyRecord arg1) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean containsSession(AxolotlAddress arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void deleteAllSessions(String arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteSession(AxolotlAddress arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public List<Integer> getSubDeviceSessions(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SessionRecord loadSession(AxolotlAddress arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void storeSession(AxolotlAddress arg0, SessionRecord arg1) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean containsSignedPreKey(int arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public SignedPreKeyRecord loadSignedPreKey(int arg0)
            throws InvalidKeyIdException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<SignedPreKeyRecord> loadSignedPreKeys() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void removeSignedPreKey(int arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void storeSignedPreKey(int arg0, SignedPreKeyRecord arg1) {
        // TODO Auto-generated method stub

    }
}