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
import android.widget.Toast;

import org.whispersystems.libaxolotl.IdentityKey;
import org.whispersystems.textsecure.internal.util.Base64;
import org.whispersystems.whisperpush.R;
import org.whispersystems.whisperpush.util.Util;
import org.whispersystems.whisperpush.zxing.IntentIntegrator;
import org.whispersystems.whisperpush.zxing.IntentResult;

public abstract class KeyScanningActivity extends Activity {

    protected IdentityKey identityKey;

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.identityKey = Util.deserializeIdentityKey(getIntent());
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuInflater inflater = this.getMenuInflater();
        menu.clear();

        inflater.inflate(R.menu.view_identity_activity_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case R.id.menu_scan:        initiateScan();    return true;
            case R.id.menu_get_scanned: initiateDisplay(); return true;
            case android.R.id.home:     finish();          return true;
        }

        return false;
    }

    private void initiateScan() {
        IntentIntegrator.initiateScan(this);
    }

    private void initiateDisplay() {
        IntentIntegrator.shareText(this, Base64.encodeBytes(identityKey.serialize()));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);

        if ((scanResult != null) && (scanResult.getContents() != null)) {
            String data = scanResult.getContents();

            if (data.equals(Base64.encodeBytes(identityKey.serialize()))) {
                Util.showAlertDialog(this,
                        getString(R.string.ViewIdentityActivity_verified),
                        getString(R.string.ViewIdentityActivity_the_scanned_key_matches));
            } else {

                Util.showAlertDialog(this,
                        getString(R.string.ViewIdentityActivity_not_verified),
                        getString(R.string.ViewIdentityActivity_warning_the_scanned_key_does_not_match));
            }
        } else {
            Toast.makeText(this, getString(R.string.ViewIdentityActivity_no_scanned_key_found),
                    Toast.LENGTH_LONG).show();
        }
    }
}
