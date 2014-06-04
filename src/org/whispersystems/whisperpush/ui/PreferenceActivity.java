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

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.whispersystems.textsecure.push.PushServiceSocket;
import org.whispersystems.whisperpush.R;
import org.whispersystems.whisperpush.service.MessageNotifier;
import org.whispersystems.whisperpush.util.PushServiceSocketFactory;
import org.whispersystems.whisperpush.util.WhisperPreferences;

import java.io.IOException;

public class PreferenceActivity extends Activity {

    private static final String PRIVACY_POLICY_URL = "http://www.cyanogenmod.org/docs/privacy";
    private static final String TERMS_OF_SERVICE_URL = "http://www.cyanogenmod.org/docs/terms";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        fragmentTransaction.replace(android.R.id.content, new WhisperPushPreferenceFragment());
        fragmentTransaction.commit();

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.preferences_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.privacy_policy) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(PRIVACY_POLICY_URL));
            startActivity(intent);
            return true;
        } else if (itemId == R.id.terms_of_service) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(TERMS_OF_SERVICE_URL));
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class WhisperPushPreferenceFragment extends PreferenceFragment
            implements Preference.OnPreferenceClickListener {

        private static final String TAG = WhisperPushPreferenceFragment.class.getSimpleName();

        private static final String PREF_REGISTRATION_CATEGORY = "pref_registration_category";
        private static final String PREF_OTHER_CATEGORY = "pref_other_category";
        private static final String PREF_REGISTER = "pref_register";
        private static final String PREF_UNREGISTER = "pref_unregister";
        private static final String PREF_MYIDENTITY = "pref_myIdentity_setting";

        private ProgressDialog mProgressDialog;

        private PreferenceCategory mRegistrationCategory;
        private PreferenceCategory mOtherCategory;
        private Preference mRegisterPreference;
        private Preference mUnregisterPreference;
        private Preference mMyIdentityPreference;

        public WhisperPushPreferenceFragment() {
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Log.d(TAG, "onCreate");
        }

        @Override
        public void onResume() {
            super.onResume();
            Log.d(TAG, "onResume");
            setupPreferences();
        }

        private void setupPreferences() {
            PreferenceScreen root = getPreferenceScreen();
            if (root != null) {
                root.removeAll();
            }
            addPreferencesFromResource(R.xml.preferences);

            mRegistrationCategory = (PreferenceCategory) findPreference(PREF_REGISTRATION_CATEGORY);
            mRegisterPreference = findPreference(PREF_REGISTER);
            mUnregisterPreference = findPreference(PREF_UNREGISTER);
            mUnregisterPreference.setOnPreferenceClickListener(this);

            mOtherCategory = (PreferenceCategory) findPreference(PREF_OTHER_CATEGORY);
            mMyIdentityPreference = findPreference(PREF_MYIDENTITY);

            if (WhisperPreferences.isRegistered(getActivity())) {
                Log.d(TAG, "WhisperPush is registered");
                mRegistrationCategory.removePreference(mRegisterPreference);
            } else {
                Log.d(TAG, "WhisperPush is not registered");
                mRegistrationCategory.removePreference(mUnregisterPreference);
                mOtherCategory.removePreference(mMyIdentityPreference);
            }
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            if (PREF_UNREGISTER.equals(preference.getKey())) {
                handleUnregister();
            }

            return true;
        }

        private void handleUnregister() {
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setTitle(getActivity().getString(R.string.pref_unregister__progress_title));
            mProgressDialog.setMessage(getActivity().getString(R.string.generic__please_wait));
            mProgressDialog.setCancelable(false);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.show();

            new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void... param) {
                    PushServiceSocket socket = PushServiceSocketFactory.create(getActivity());
                    try {
                        socket.unregisterGcmId();
                    } catch (IOException e) {
                        return false;
                    }
                    WhisperPreferences.setRegistered(getActivity(), false);
                    return true;
                }

                @Override
                protected void onPostExecute(Boolean result) {
                    if(result) {
                        MessageNotifier.notifyUnRegistered(getActivity());
                    }
                    mProgressDialog.dismiss();
                    setupPreferences();
                }
            }.execute();
        }
    }

}
