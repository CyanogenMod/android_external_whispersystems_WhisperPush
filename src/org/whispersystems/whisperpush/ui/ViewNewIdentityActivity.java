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

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.whispersystems.whisperpush.R;
import org.whispersystems.whisperpush.contacts.Contact;

/**
 * Activity for displaying a new identity.
 *
 * @author ctso
 */
public class ViewNewIdentityActivity extends KeyScanningActivity {

    private Contact contact;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.view_new_identity_activity);

        initializeResources();
    }

    private void initializeResources() {
        ImageView imageView = (ImageView) findViewById(R.id.avatar);
        TextView contactText = (TextView) findViewById(R.id.contact_name);
        TextView fingerprintText = (TextView) findViewById(R.id.identity_fingerprint);
        Button continueButton = (Button) findViewById(R.id.continue_button);

        contact = getIntent().getParcelableExtra("contact");

        fingerprintText.setText(identityKey.getFingerprint());
        imageView.setImageBitmap(contact.getAvatar());
        contactText.setText(contact.toShortString());
        continueButton.setOnClickListener(new ContinueListener());
    }

    private class ContinueListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            finish();
        }
    }

}
