/**
 * Copyright (C) 2014 The CyanogenMod Project
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

package org.whispersystems.whisperpush.db;

import org.whispersystems.textsecure.api.util.InvalidNumberException;
import org.whispersystems.textsecure.api.util.PhoneNumberFormatter;
import org.whispersystems.whisperpush.util.WhisperPreferences;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

public class ActiveSessionProvider extends ContentProvider {

    private static final String TAG = ActiveSessionProvider.class.getSimpleName();

    public static final String AUTHORITY = "org.whispersystems.whisperpush.sessionprovider";
    public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder)
    {
        if (!WhisperPreferences.isRegistered(getContext())) {
            return null;
        }

        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables("contacts");

        String[] activeSessionColumn = new String[] { CMDatabase.ACTIVE_SESSION };
        String selectionClause = CMDatabase.NUMBER + " = ?";

        if (selectionArgs == null || selectionArgs.length < 1) {
            Log.w(TAG, "No selection args in query");
            return null;
        }

        if (selectionArgs.length > 1) {
            Log.w(TAG, "Attemping to query with multiple selection args, returning only first");
        }

        // We're only going to return one row
        // based off one number if the token
        // is not null.
        String selectionArg = formatNumber(selectionArgs[0]);

        if (selectionArg == null) {
            return null;
        }

        String[] newSelectionArg = new String[] { selectionArg };

        CMDatabase database = CMDatabase.getInstance(getContext());
        Cursor cursor = queryBuilder.query(database.getReadableDatabaseFromHelper(),
                activeSessionColumn, selectionClause,
                newSelectionArg , null, null, sortOrder);
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    private String formatNumber(String number) {
        String localNumber = WhisperPreferences.getLocalNumber(getContext());
        String e164number;
        try {
            e164number = PhoneNumberFormatter.formatNumber(number, localNumber);
        } catch (InvalidNumberException e) {
            Log.w(TAG, e);
            return null;
        }

        if (e164number.equals(localNumber)) {
            return null;
        }

        return e164number;
    }
}