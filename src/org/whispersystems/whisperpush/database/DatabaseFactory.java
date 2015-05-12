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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseFactory {

    private static final String DATABASE_NAME    = "whisper_push";
    private static final int    DATABASE_VERSION = 1;

    private static DatabaseFactory instance;

    private final CanonicalAddressDatabase addressDatabase;
    private final IdentityDatabase         identityDatabase;
    private final PendingApprovalDatabase  pendingApprovalDatabase;

    public synchronized static DatabaseFactory getInstance(Context context) {
        if (instance == null)
            instance = new DatabaseFactory(context);

        return instance;
    }

    private DatabaseFactory(Context context) {
        DatabaseHelper databaseHelper = new DatabaseHelper(context, DATABASE_NAME, null, DATABASE_VERSION);

        this.identityDatabase        = new IdentityDatabase(context, databaseHelper);
        this.addressDatabase         = new CanonicalAddressDatabase(databaseHelper);
        this.pendingApprovalDatabase = new PendingApprovalDatabase(context, databaseHelper);
    }

    public static CanonicalAddressDatabase getAddressDatabase(Context context) {
        return getInstance(context).addressDatabase;
    }

    public static IdentityDatabase getIdentityDatabase(Context context) {
        return getInstance(context).identityDatabase;
    }

    public static PendingApprovalDatabase getPendingApprovalDatabase(Context context) {
        return getInstance(context).pendingApprovalDatabase;
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {

        public DatabaseHelper(Context context, String name,
                              SQLiteDatabase.CursorFactory factory,
                              int version)
        {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            CanonicalAddressDatabase.onCreate(db);
            IdentityDatabase.onCreate(db);
            PendingApprovalDatabase.onCreate(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }
}
