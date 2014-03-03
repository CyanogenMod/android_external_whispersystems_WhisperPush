package org.whispersystems.whisperpush.ui;

import android.os.Bundle;
import android.widget.TextView;

import org.whispersystems.whisperpush.R;
import org.whispersystems.whisperpush.crypto.IdentityKeyUtil;

/**
 * Activity for viewing the user's identity key.
 *
 * @author ctso
 */
public class ViewMyIdentityActivity extends KeyScanningActivity {

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.identityKey = IdentityKeyUtil.getIdentityKey(this);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.view_identity_activity);

        initializeResources();
    }

    private void initializeResources() {
        TextView identityFingerprint = (TextView) findViewById(R.id.identity_fingerprint);
        identityFingerprint.setText(identityKey.getFingerprint());
    }

}
