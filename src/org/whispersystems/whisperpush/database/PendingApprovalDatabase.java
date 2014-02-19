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

import org.whispersystems.textsecure.push.IncomingPushMessage;
import org.whispersystems.textsecure.util.Base64;
import org.whispersystems.textsecure.util.Util;

import java.io.IOException;
import java.util.List;

public class PendingApprovalDatabase {

    private static final Uri CHANGE_URI = Uri.parse("content://whisperpush/identities");

    private static final String TABLE_NAME   = "pending_approval";

    public  static final String ID           = "_id";
    public  static final String TYPE         = "type";
    public  static final String SOURCE       = "source";
    public  static final String DESTINATIONS = "destinations";
    public  static final String BODY         = "body";
    public  static final String TIMESTAMP    = "timestamp";

    public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" + ID + " INTEGER PRIMARY KEY, " +
            TYPE + " INTEGER, " + SOURCE + " TEXT, " + DESTINATIONS + " TEXT, " + BODY + " TEXT, " + TIMESTAMP + " INTEGER);";

    private final Context context;
    private final SQLiteOpenHelper databaseHelper;

    public PendingApprovalDatabase(Context context, SQLiteOpenHelper databaseHelper) {
        this.context        = context;
        this.databaseHelper = databaseHelper;
    }

    public long insert(IncomingPushMessage message) {
        ContentValues values = new ContentValues();
        values.put(TYPE, message.getType());
        values.put(SOURCE, message.getSource());
        values.put(DESTINATIONS, Util.join(message.getDestinations(), ","));
        values.put(BODY, Base64.encodeBytes(message.getBody()));
        values.put(TIMESTAMP, message.getTimestampMillis());

        long result = databaseHelper.getWritableDatabase().insert(TABLE_NAME, null, values);
        context.getContentResolver().notifyChange(CHANGE_URI, null);

        return result;
    }

    public Cursor getPending() {
        Cursor cursor = databaseHelper.getReadableDatabase().query(TABLE_NAME, null, null,
                null, null, null, null);

        cursor.setNotificationUri(context.getContentResolver(), CHANGE_URI);
        return cursor;
    }

    public Cursor getPending(String number) {
        Cursor cursor = databaseHelper.getReadableDatabase().query(TABLE_NAME, null, SOURCE + " = ?",
                new String[] {number}, null, null, null);

        cursor.setNotificationUri(context.getContentResolver(), CHANGE_URI);
        return cursor;
    }

    public void delete(long id) {
        databaseHelper.getWritableDatabase().delete(TABLE_NAME, ID + " = ?", new String[] {id+""});
        context.getContentResolver().notifyChange(CHANGE_URI, null);
    }

    public Reader readerFor(Cursor cursor) {
        return new Reader(cursor);
    }

    public static class Reader {
        private final Cursor cursor;

        public Reader(Cursor cursor) {
            this.cursor = cursor;
        }

        public IncomingPushMessage getCurrent() {
            try {
                int          type         = cursor.getInt(cursor.getColumnIndexOrThrow(TYPE));
                String       source       = cursor.getString(cursor.getColumnIndexOrThrow(SOURCE));
                List<String> destinations = Util.split(cursor.getString(cursor.getColumnIndexOrThrow(DESTINATIONS)), ",");
                byte[]       body         = Base64.decode(cursor.getString(cursor.getColumnIndexOrThrow(BODY)));
                long         timestamp    = cursor.getLong(cursor.getColumnIndexOrThrow(TIMESTAMP));

                return new IncomingPushMessage(type, source, destinations, body, timestamp);
            } catch (IOException e) {
                throw new AssertionError(e);
            }
        }

        public IncomingPushMessage getNext() {
            if (cursor == null || !cursor.moveToNext())
                return null;

            return getCurrent();
        }

        public long getCurrentId() {
            return cursor.getLong(cursor.getColumnIndexOrThrow(ID));
        }

        public void close() {
            this.cursor.close();
        }
    }

    public static void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

}

