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

package org.whispersystems.whisperpush.ui;

import java.io.IOException;

import org.whispersystems.libaxolotl.util.guava.Optional;
import org.whispersystems.textsecure.api.TextSecureAccountManager;
import org.whispersystems.whisperpush.R;
import org.whispersystems.whisperpush.util.WhisperPreferences;
import org.whispersystems.whisperpush.util.WhisperServiceFactory;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class ErrorAndResetActivity extends Activity {

    private Button mResetButton;
    private ProgressDialog mProgressDialog;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.error_and_reset_activity);

        mResetButton = (Button) findViewById(R.id.reset_button);

        mResetButton.setOnClickListener(clickListener);
    }

    View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            handleUnregister();
        }
    };

    private void handleUnregister() {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle(getString(R.string.pref_unregister__progress_title));
        mProgressDialog.setMessage(getString(R.string.generic__please_wait));
        mProgressDialog.setCancelable(false);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.show();

        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... param) {
                TextSecureAccountManager manager =
                        WhisperServiceFactory.createAccountManager(getApplicationContext());
                try {
                    manager.setGcmId(Optional.<String>absent()); // unregister
                } catch (IOException e) {
                    return false;
                }
                WhisperPreferences.resetPreferences(ErrorAndResetActivity.this);
                return true;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result) {
                    Intent i = new Intent();
                    i.setClass(ErrorAndResetActivity.this, RegistrationProgressActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ErrorAndResetActivity.this.startActivity(i);
                }
                mProgressDialog.dismiss();
                finish();
            }
        }.execute();
    }
}
