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
