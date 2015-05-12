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

import java.io.IOException;

import org.whispersystems.libaxolotl.IdentityKey;
import org.whispersystems.libaxolotl.IdentityKeyPair;
import org.whispersystems.libaxolotl.InvalidKeyException;
import org.whispersystems.libaxolotl.ecc.ECPrivateKey;
import org.whispersystems.libaxolotl.util.KeyHelper;
import org.whispersystems.textsecure.internal.util.Base64;
import org.whispersystems.whisperpush.util.WhisperPreferences;

import android.content.Context;
import android.util.Log;

/**
 * Responsible for generating, loading, and storing the identity
 * key for the user.
 */
public class IdentityKeyUtil {

    public static boolean hasIdentityKey(Context context) {
        return WhisperPreferences.getIdentityKeyPrivate(context) != null &&
               WhisperPreferences.getIdentityKeyPublic(context)  != null;
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
            ECPrivateKey privateKey      = masterCipher.decryptKey(privateKeyBytes);

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
        MasterCipher    masterCipher         = new MasterCipher(masterSecret);
        IdentityKeyPair identityKey          = KeyHelper.generateIdentityKeyPair();
        byte[]          serializedPublicKey  = identityKey.getPublicKey().serialize();
        byte[]          serializedPrivateKey = masterCipher.encryptKey(identityKey.getPrivateKey());

        WhisperPreferences.setIdentityKeyPublic(context, Base64.encodeBytes(serializedPublicKey));
        WhisperPreferences.setIdentityKeyPrivate(context, Base64.encodeBytes(serializedPrivateKey));
    }
}
