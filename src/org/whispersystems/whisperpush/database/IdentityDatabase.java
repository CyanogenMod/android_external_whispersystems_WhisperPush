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
package org.whispersystems.whisperpush.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;

import org.whispersystems.libaxolotl.IdentityKey;
import org.whispersystems.libaxolotl.InvalidKeyException;
import org.whispersystems.textsecure.internal.util.Base64;
import org.whispersystems.whisperpush.crypto.MasterCipher;
import org.whispersystems.whisperpush.crypto.MasterSecret;
import org.whispersystems.whisperpush.crypto.MessagePeer;

import java.io.IOException;

public class IdentityDatabase {

    private static final Uri CHANGE_URI = Uri.parse("content://whisperpush/identities");

    private static final String TABLE_NAME   = "identities";
    private static final String ID           = "_id";
    public  static final String ADDRESS      = "address";
    public  static final String IDENTITY_KEY = "key";
    public  static final String MAC          = "mac";

    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME +
            " (" + ID + " INTEGER PRIMARY KEY, " +
            ADDRESS + " INTEGER, " +
            IDENTITY_KEY + " TEXT, " +
            MAC + " TEXT);";

    private static final String CREATE_INDEX[]  = {"CREATE INDEX IF NOT EXISTS address_index ON " +
            TABLE_NAME + " (" + ADDRESS + ");",
            "CREATE UNIQUE INDEX IF NOT EXISTS address_key_index ON " +
                    TABLE_NAME + " (" + ADDRESS + ", " + IDENTITY_KEY + ");"};

    private final Context context;
    private final SQLiteOpenHelper databaseHelper;

    public IdentityDatabase(Context context, SQLiteOpenHelper databaseHelper) {
        this.context        = context;
        this.databaseHelper = databaseHelper;
    }

    public boolean isFreshIdentity(MessagePeer address) {
        SQLiteDatabase database  = databaseHelper.getReadableDatabase();
        long           addressId = address.getCanonicalAddress(context);
        Cursor         cursor    = null;

        try {
            cursor = database.query(TABLE_NAME, null, ADDRESS + " = ?",
                    new String[] {String.valueOf(addressId)}, null, null, null);

            return cursor == null || cursor.isAfterLast() || cursor.getCount() == 0;
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    public boolean isValidIdentity(MasterSecret masterSecret,
                                   MessagePeer address,
                                   IdentityKey theirIdentity)
    {
        SQLiteDatabase database     = databaseHelper.getReadableDatabase();
        MasterCipher   masterCipher = new MasterCipher(masterSecret);
        long           addressId    = address.getCanonicalAddress(context);
        Cursor         cursor       = null;

        try {
            cursor = database.query(TABLE_NAME, null, ADDRESS + " = ?",
                    new String[] {String.valueOf(addressId)}, null, null,null);

            if (cursor == null || cursor.isAfterLast() || cursor.getCount() == 0) {
                return true;
            }

            while (cursor.moveToNext()) {
                try {
                    String serializedIdentity = cursor.getString(cursor.getColumnIndexOrThrow(IDENTITY_KEY));
                    String mac                = cursor.getString(cursor.getColumnIndexOrThrow(MAC));

                    if (!masterCipher.verifyMacFor(addressId + serializedIdentity, Base64.decode(mac))) {
                        Log.w("IdentityDatabase", "MAC failed");
                        continue;
                    }

                    IdentityKey ourIdentity = new IdentityKey(Base64.decode(serializedIdentity), 0);

                    if (ourIdentity.equals(theirIdentity)) {
                        return true;
                    }
                } catch (IOException e) {
                    Log.w("IdentityDatabase", e);
                    return false;
                } catch (InvalidKeyException e) {
                    Log.w("IdentityDatabase", e);
                    return false;
                }
            }

            return false;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void saveIdentity(MasterSecret masterSecret,
                             MessagePeer address,
                             IdentityKey identityKey)
    {
        SQLiteDatabase database          = databaseHelper.getWritableDatabase();
        long           addressId         = address.getCanonicalAddress(context);
        MasterCipher   masterCipher      = new MasterCipher(masterSecret);
        String         identityKeyString = Base64.encodeBytes(identityKey.serialize());
        String         macString         = Base64.encodeBytes(masterCipher.getMacFor(addressId +
                identityKeyString  ));

        ContentValues contentValues = new ContentValues();
        contentValues.put(ADDRESS, addressId);
        contentValues.put(IDENTITY_KEY, identityKeyString);
        contentValues.put(MAC, macString);

        database.replace(TABLE_NAME, null, contentValues);
        context.getContentResolver().notifyChange(CHANGE_URI, null);
    }

    public Cursor getIdentities() {
        SQLiteDatabase database = databaseHelper.getReadableDatabase();
        Cursor cursor           = database.query(TABLE_NAME, null, null, null, null, null, ADDRESS);

        if (cursor != null)
            cursor.setNotificationUri(context.getContentResolver(), CHANGE_URI);

        return cursor;
    }

    public Reader readerFor(MasterSecret masterSecret, Cursor cursor) {
        return new Reader(masterSecret, cursor);
    }

    public class Reader {

        private final Cursor cursor;
        private final MasterCipher cipher;

        public Reader(MasterSecret masterSecret, Cursor cursor) {
            this.cursor = cursor;
            this.cipher = new MasterCipher(masterSecret);
        }

        public Pair<String, IdentityKey> getCurrent() {
            String number = null;

            try {
                long canonicalAddress = cursor.getLong(cursor.getColumnIndexOrThrow(ADDRESS));
                number                = DatabaseFactory.getAddressDatabase(context)
                        .getNumberFromCanonicalAddress(canonicalAddress);

                String identityKeyString = cursor.getString(cursor.getColumnIndexOrThrow(IDENTITY_KEY));
                String mac               = cursor.getString(cursor.getColumnIndexOrThrow(MAC));

                if (!cipher.verifyMacFor(canonicalAddress + identityKeyString, Base64.decode(mac))) {
                    return new Pair<String, IdentityKey>(number, null);
                }

                IdentityKey identityKey = new IdentityKey(Base64.decode(identityKeyString), 0);
                return new Pair<String, IdentityKey>(number, identityKey);
            } catch (IOException e) {
                Log.w("IdentityDatabase", e);
                return new Pair<String, IdentityKey>(number, null);
            } catch (InvalidKeyException e) {
                Log.w("IdentityDatabase", e);
                return new Pair<String, IdentityKey>(number, null);
            } catch (NoSuchAddressException e) {
                Log.w("IdentityDatabase", e);
                return new Pair<String, IdentityKey>(null, null);
            }
        }
    }

    public static void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
        for (String index : CREATE_INDEX) {
            db.execSQL(index);
        }
    }
}
