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

import org.whispersystems.textsecure.crypto.IdentityKey;
import org.whispersystems.textsecure.crypto.IdentityKeyPair;
import org.whispersystems.textsecure.crypto.InvalidKeyException;
import org.whispersystems.textsecure.crypto.MasterCipher;
import org.whispersystems.textsecure.crypto.MasterSecret;
import org.whispersystems.textsecure.crypto.ecc.Curve;
import org.whispersystems.textsecure.crypto.ecc.ECKeyPair;
import org.whispersystems.textsecure.crypto.ecc.ECPrivateKey;
import org.whispersystems.textsecure.util.Base64;
import org.whispersystems.whisperpush.util.WhisperPreferences;

import java.io.IOException;

/**
 * Responsible for generating, loading, and storing the identity
 * key for the user.
 */
public class IdentityKeyUtil {

    public static boolean hasIdentityKey(Context context) {
        return
                WhisperPreferences.getIdentityKeyPrivate(context) != null &&
                        WhisperPreferences.getIdentityKeyPublic(context) != null;
    }

    public static IdentityKey getIdentityKey(Context context) {
        if (!hasIdentityKey(context))
            return null;

        try {
            byte[] publicKeyBytes = Base64.decode(WhisperPreferences.getIdentityKeyPublic(context));
            return new IdentityKey(publicKeyBytes, 0);
        } catch (IOException ioe) {
            Log.w("IdentityKeyUtil", ioe);
            return null;
        } catch (InvalidKeyException e) {
            Log.w("IdentityKeyUtil", e);
            return null;
        }
    }

    public static IdentityKeyPair getIdentityKeyPair(Context context, MasterSecret masterSecret) {
        if (!hasIdentityKey(context))
            return null;

        try {
            MasterCipher masterCipher    = new MasterCipher(masterSecret);
            IdentityKey  publicKey       = getIdentityKey(context);
            byte[]       privateKeyBytes = Base64.decode(WhisperPreferences.getIdentityKeyPrivate(context));
            ECPrivateKey privateKey      = masterCipher.decryptKey(publicKey.getPublicKey().getType(), privateKeyBytes);

            return new IdentityKeyPair(publicKey, privateKey);
        } catch (IOException e) {
            Log.w("IdentityKeyUtil", e);
            return null;
        } catch (InvalidKeyException e) {
            throw new AssertionError(e);
        }
    }

    public static String getFingerprint(Context context) {
        if (!hasIdentityKey(context)) return null;

        IdentityKey identityKey = getIdentityKey(context);

        if (identityKey == null) return null;
        else                     return identityKey.getFingerprint();
    }

    public static void generateIdentityKeys(Context context, MasterSecret masterSecret) {
        MasterCipher masterCipher         = new MasterCipher(masterSecret);
        ECKeyPair    keyPair              = Curve.generateKeyPairForType(Curve.DJB_TYPE);
        IdentityKey  identityKey          = new IdentityKey(keyPair.getPublicKey());
        byte[]       serializedPublicKey  = identityKey.serialize();
        byte[]       serializedPrivateKey = masterCipher.encryptKey(keyPair.getPrivateKey());

        WhisperPreferences.setIdentityKeyPublic(context, Base64.encodeBytes(serializedPublicKey));
        WhisperPreferences.setIdentityKeyPrivate(context, Base64.encodeBytes(serializedPrivateKey));
    }
}
