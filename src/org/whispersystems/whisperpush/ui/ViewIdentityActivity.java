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
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import org.whispersystems.textsecure.crypto.IdentityKey;
import org.whispersystems.textsecure.util.Base64;
import org.whispersystems.textsecure.util.Util;
import org.whispersystems.textsecure.zxing.integration.IntentIntegrator;
import org.whispersystems.textsecure.zxing.integration.IntentResult;
import org.whispersystems.whisperpush.R;

/**
 * Activity for displaying an identity key.
 *
 * @author Moxie Marlinspike
 */
public class ViewIdentityActivity extends KeyScanningActivity {

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.view_identity_activity);

        initializeResources();
    }

    private void initializeResources() {
        TextView identityFingerprint = (TextView) findViewById(R.id.identity_fingerprint);
        identityFingerprint.setText(identityKey.getFingerprint());
    }

}
