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
package org.whispersystems.whisperpush.loaders;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;

import org.whispersystems.whisperpush.database.DatabaseFactory;

public class IdentityLoader extends CursorLoader {

    private final Context context;

    public IdentityLoader(Context context) {
        super(context);
        this.context      = context.getApplicationContext();
    }

    @Override
    public Cursor loadInBackground() {
        return DatabaseFactory.getIdentityDatabase(context).getIdentities();
    }

}
