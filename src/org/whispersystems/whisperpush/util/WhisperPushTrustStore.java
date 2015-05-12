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
package org.whispersystems.whisperpush.util;

import android.content.Context;

import org.whispersystems.textsecure.api.push.TrustStore;
import org.whispersystems.whisperpush.R;

import java.io.InputStream;

public class WhisperPushTrustStore implements TrustStore {

    private final Context context;

    public WhisperPushTrustStore(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public InputStream getKeyStoreInputStream() {
        return context.getResources().openRawResource(R.raw.whisper);
    }

    @Override
    public String getKeyStorePassword() {
        return "whisper";
    }
}
