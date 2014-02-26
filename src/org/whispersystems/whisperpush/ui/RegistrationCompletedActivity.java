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
package org.whispersystems.whisperpush.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;

import org.whispersystems.whisperpush.R;
import org.whispersystems.whisperpush.util.WhisperPreferences;
import org.whispersystems.textsecure.push.PushServiceSocket;
import org.whispersystems.whisperpush.util.PushServiceSocketFactory;

public class RegistrationCompletedActivity extends Activity implements View.OnClickListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registration_completed_activity);

        findViewById(R.id.registerAgainButton).setOnClickListener(this);
        findViewById(R.id.unregisterButton).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v == findViewById(R.id.registerAgainButton)) {
            WhisperPreferences.setRegistered(this, false);
            startActivity(new Intent(this, RegistrationActivity.class));
            finish();
        } else if (v == findViewById(R.id.unregisterButton)) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... param) {
                    Boolean success = Boolean.TRUE;

                    PushServiceSocket socket = PushServiceSocketFactory.create(getApplicationContext());
                    try {
                        socket.unregisterGcmId();
                    } catch (java.io.IOException e) {
                        return null;
                    }
                    WhisperPreferences.setRegistered(getApplicationContext(), false);
                    return null;
                }
                @Override
                protected void onPostExecute(Void result) {
                    finish();
                }
            }.execute();
        }
    }
}
