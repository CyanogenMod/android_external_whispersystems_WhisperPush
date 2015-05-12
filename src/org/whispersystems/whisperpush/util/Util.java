package org.whispersystems.whisperpush.util;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import org.whispersystems.libaxolotl.IdentityKey;
import org.whispersystems.libaxolotl.InvalidKeyException;
import org.whispersystems.textsecure.internal.util.Base64;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.EditText;

public class Util {
    public static boolean isEmpty(String value) {
        return value == null || value.trim().length() == 0;
    }

    public static boolean isEmpty(EditText value) {
        return value == null || value.getText() == null
                || isEmpty(value.getText().toString());
    }

    public static void showAlertDialog(Context context, String title, String message) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setIcon(android.R.drawable.ic_dialog_alert);
        dialog.setPositiveButton(android.R.string.ok, null);
        dialog.show();
    }

    @SuppressLint("TrulyRandom") // we are not running on old Androids
    public static SecureRandom getSecureRandom() {
        try {
            return SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    public static String getSecret(int size) {
        return Base64.encodeBytes(
            org.whispersystems.textsecure.internal.util.Util.getSecretBytes(size));
    }

    public static IdentityKey deserializeIdentityKey(Intent intent) {
        byte[] key = intent.getByteArrayExtra("identity_key");
        if(key == null) { return null; } // no key passed
        try {
            return new IdentityKey(key, 0);
        } catch (InvalidKeyException e) {
            Log.e("WPUtil", "intent passed bad identity key");
            return null;
        }
    }
}