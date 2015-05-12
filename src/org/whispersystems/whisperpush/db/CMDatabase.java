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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

/**
 * I would rather not modify the TextSecure library, so this database exists to store
 * values that are specific to CyanogenMod.
 *
 * @author ctso
 */
public class CMDatabase {

    private final String DATABASE_NAME = "cyanogenmod.db";
    private final int DATABASE_VERSION = 2;

    public static final String ID = "_id";
    public static final String NUMBER = "number";
    public static final String ACTIVE_SESSION = "active_session";

    private static CMDatabase sInstance;

    private final DatabaseHelper mDatabaseHelper;

    public synchronized static CMDatabase getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new CMDatabase(context);
        }
        return sInstance;
    }

    public CMDatabase(Context context) {
        this.mDatabaseHelper = new DatabaseHelper(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public boolean hasActiveSession(String number) {
        if (TextUtils.isEmpty(number)) {
            return false;
        }

        SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.query("contacts", new String[]{ACTIVE_SESSION}, NUMBER + " = ?",
                    new String[]{number}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(0) == 1;
            } else {
                return false;
            }
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    public void setActiveSession(String number, boolean hasActiveSession) {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(NUMBER, number);
        values.put(ACTIVE_SESSION, hasActiveSession ? 1 : 0);
        db.replace("contacts", null, values);
    }

    public void setActiveSession(String number) {
        setActiveSession(number, true);
    }

    public SQLiteDatabase getReadableDatabaseFromHelper() {
        return mDatabaseHelper.getReadableDatabase();
    }

    private class DatabaseHelper extends SQLiteOpenHelper {

        public DatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory,
                              int version) {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE contacts (" + ID + " INTEGER PRIMARY KEY, " +
                    NUMBER + " TEXT UNIQUE, " +
                    ACTIVE_SESSION + " INTEGER DEFAULT 0);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion < DATABASE_VERSION) {
                // FIXME: if we figure out a migration solutions from our old server to
                // Moxie's server, then we should do something more graceful here, or at
                // least warn users - WF
                db.execSQL("DROP TABLE contacts;");
                onCreate(db);
            }
        }
    }
}