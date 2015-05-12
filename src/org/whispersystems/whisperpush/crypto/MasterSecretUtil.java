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

import org.whispersystems.textsecure.internal.util.Base64;
import org.whispersystems.textsecure.internal.util.Util;
import org.whispersystems.whisperpush.util.WhisperPreferences;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * This mainly exists as a placeholder, and for TextSecure-library
 * compatibility.  The MasterSecret is used to encrypt any private
 * key material before writing it to disk, and the MasterSecret key
 * material would itself be encrypted using a user specified passphrase.
 *
 * This project does not currently employ a user-specified passphrase
 * for local secret encryption since full disk encryption is available
 * through the system.  However, we still generate a MasterSecret since
 * the TextSecure library functions require it and the overhead is negligable.
 * We just don't encrypt it with anything, meaning that it doesn't provide
 * any protection by design.
 */
public class MasterSecretUtil {

    public static synchronized MasterSecret getMasterSecret(Context context) {
        if (WhisperPreferences.getMasterSecret(context) == null) {
            return generateMasterSecret(context);
        } else {
            MasterSecret masterSecret = retrieveMasterSecret(context);
            return masterSecret == null ? generateMasterSecret(context) : masterSecret;
        }
    }

    private static MasterSecret generateMasterSecret(Context context) {
        byte[] encryptionSecret = generateEncryptionSecret();
        byte[] macSecret        = generateMacSecret();
        byte[] masterSecret     = Util.join(encryptionSecret, macSecret);

        WhisperPreferences.setMasterSecret(context, Base64.encodeBytes(masterSecret));

        return new MasterSecret(new SecretKeySpec(encryptionSecret, "AES"),
                new SecretKeySpec(macSecret, "HmacSHA1"));
    }

    private static MasterSecret retrieveMasterSecret(Context context) {
        try {
            byte[] combinedSecrets  = Base64.decode(WhisperPreferences.getMasterSecret(context));
            byte[] encryptionSecret = getEncryptionSecret(combinedSecrets);
            byte[] macSecret        = getMacSecret(combinedSecrets);

            return new MasterSecret(new SecretKeySpec(encryptionSecret, "AES"),
                    new SecretKeySpec(macSecret, "HmacSHA1"));
        } catch (IOException e) {
            Log.w("MasterSecretUtil", e);
            return null;
        }
    }

    private static byte[] getEncryptionSecret(byte[] combinedSecrets) {
        byte[] encryptionSecret = new byte[16];
        System.arraycopy(combinedSecrets, 0, encryptionSecret, 0, encryptionSecret.length);
        return encryptionSecret;
    }

    private static byte[] getMacSecret(byte[] combinedSecrets) {
        byte[] macSecret = new byte[20];
        System.arraycopy(combinedSecrets, 16, macSecret, 0, macSecret.length);
        return macSecret;
    }

    private static byte[] generateEncryptionSecret() {
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(128);

            SecretKey key = generator.generateKey();
            return key.getEncoded();
        } catch (NoSuchAlgorithmException ex) {
            Log.w("keyutil", ex);
            return null;
        }
    }

    private static byte[] generateMacSecret() {
        try {
            KeyGenerator generator = KeyGenerator.getInstance("HmacSHA1");
            return generator.generateKey().getEncoded();
        } catch (NoSuchAlgorithmException e) {
            Log.w("keyutil", e);
            return null;
        }
    }
}