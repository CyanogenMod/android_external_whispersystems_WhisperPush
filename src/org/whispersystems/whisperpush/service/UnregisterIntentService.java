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
package org.whispersystems.whisperpush.service;

import android.app.IntentService;
import android.content.Intent;
import android.preference.PreferenceActivity;
import org.whispersystems.textsecure.push.PushServiceSocket;
import org.whispersystems.whisperpush.util.PushServiceSocketFactory;
import org.whispersystems.whisperpush.util.WhisperPreferences;

import java.io.IOException;

public class UnregisterIntentService extends IntentService {

    public UnregisterIntentService() {
        super(UnregisterIntentService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
    }

    @Override
    public void onCreate() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        PushServiceSocket socket = PushServiceSocketFactory.create(this);
        try {
            socket.unregisterGcmId();
            WhisperPreferences.setRegistered(this, false);
            MessageNotifier.notifyUnRegistered(this);
            Intent i = new Intent(this, PreferenceActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (IOException e) {
            //
        }
        return IntentService.START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
