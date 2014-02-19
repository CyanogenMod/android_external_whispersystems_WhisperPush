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

import org.whispersystems.textsecure.push.PushServiceSocket;
import org.whispersystems.whisperpush.Release;

public class PushServiceSocketFactory {
    public static PushServiceSocket create(Context context, String number, String password) {
        return new PushServiceSocket(context, Release.PUSH_URL, new WhisperPushTrustStore(context),
                number, password);
    }

    public static PushServiceSocket create(Context context) {
        return create(context,
                WhisperPreferences.getLocalNumber(context),
                WhisperPreferences.getPushServerPassword(context));
    }
}
