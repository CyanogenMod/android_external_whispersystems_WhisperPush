package org.whispersystems.whisperpush.util;

import org.whispersystems.textsecure.internal.util.Base64;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.EditText;

public class Util {
    public static boolean isEmpty(String value) {
        return value == null || value.trim().length() == 0;
    }

    public static boolean isEmpty(EditText value) {
        return value == null || value.getText() == null
                || isEmpty(value.getText().toString());
    }

    public static boolean isEmpty(CharSequence value) {
        return value == null || value.length() == 0;
    }

    public static void showAlertDialog(Context context, String title, String message) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setIcon(android.R.drawable.ic_dialog_alert);
        dialog.setPositiveButton(android.R.string.ok, null);
        dialog.show();
    }

    public static String getSecret(int size) {
        return Base64.encodeBytes(
            org.whispersystems.textsecure.internal.util.Util.getSecretBytes(size));
    }
}