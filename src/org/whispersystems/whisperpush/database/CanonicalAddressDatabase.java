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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CanonicalAddressDatabase {

    private static final String TABLE_NAME      = "canonical_addresses";
    private static final String ID_COLUMN       = "_id";
    private static final String NUMBER_COLUMN   = "number";

    private static final String CREATE_TABLE  = "CREATE TABLE " + TABLE_NAME + " (" + ID_COLUMN +
            " integer PRIMARY KEY, " + NUMBER_COLUMN +
            " TEXT NOT NULL);";
    private static final String CREATE_INDEX  = "CREATE INDEX IF NOT EXISTS number_index ON " +
            TABLE_NAME + " (" + NUMBER_COLUMN + ");";

    private static final String[] ID_PROJECTION  = {ID_COLUMN};
    private static final String SELECTION        = "PHONE_NUMBERS_EQUAL(" + NUMBER_COLUMN + ", ?)";

    private final Map<String,Long> addressCache = Collections.synchronizedMap(new HashMap<String, Long>());
    private final Map<Long,String> numberCache  = Collections.synchronizedMap(new HashMap<Long,String>());

    private final SQLiteOpenHelper databaseHelper;

    public CanonicalAddressDatabase(SQLiteOpenHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
    }

    public String getNumberFromCanonicalAddress(long canonicalAddress) throws NoSuchAddressException {
        String number;

        if ((number = getNumberFromCache(canonicalAddress)) != null)
            return number;

        number = getNumberFromDatabase(canonicalAddress);
        numberCache.put(canonicalAddress, number);

        return number;
    }

    public long getCanonicalAddressFromNumber(String number) {
        long canonicalAddress;

        if ((canonicalAddress = getCanonicalAddressFromCache(number)) != -1)
            return canonicalAddress;

        canonicalAddress = getCanonicalAddressFromDatabase(number);
        addressCache.put(number, canonicalAddress);

        return canonicalAddress;
    }

    private long getCanonicalAddressFromCache(String number) {
        if (addressCache.containsKey(number))
            return addressCache.get(number);

        return -1L;
    }

    private String getNumberFromCache(long canonicalAddress) {
        if (numberCache.containsKey(canonicalAddress))
            return numberCache.get(canonicalAddress);

        return null;
    }

    private long getCanonicalAddressFromDatabase(String number) {
        Cursor cursor = null;

        try {
            SQLiteDatabase db = databaseHelper.getWritableDatabase();
            cursor            = db.query(TABLE_NAME, ID_PROJECTION, SELECTION,
                    new String[]{number}, null, null, null);

            if (cursor.getCount() == 0 || !cursor.moveToFirst()) {
                ContentValues contentValues = new ContentValues(1);
                contentValues.put(NUMBER_COLUMN, number);

                return db.insert(TABLE_NAME, null, contentValues);
            }

            return cursor.getLong(cursor.getColumnIndexOrThrow(ID_COLUMN));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private String getNumberFromDatabase(long canonicalAddress) throws NoSuchAddressException {
        Cursor cursor = null;

        try {
            SQLiteDatabase db = databaseHelper.getReadableDatabase();
            cursor            = db.query(TABLE_NAME, null, ID_COLUMN + " = ?",
                    new String[] {String.valueOf(canonicalAddress)},
                    null, null, null);

            if (!cursor.moveToFirst())
                throw new NoSuchAddressException();

            return cursor.getString(cursor.getColumnIndexOrThrow(NUMBER_COLUMN));
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    public static void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
        db.execSQL(CREATE_INDEX);
    }

}
