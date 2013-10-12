package org.whispersystems.whisperpush.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import org.whispersystems.textsecure.crypto.IdentityKey;
import org.whispersystems.textsecure.util.Base64;
import org.whispersystems.textsecure.util.Util;
import org.whispersystems.textsecure.zxing.integration.IntentIntegrator;
import org.whispersystems.textsecure.zxing.integration.IntentResult;
import org.whispersystems.whisperpush.R;

public abstract class KeyScanningActivity extends Activity {

  protected IdentityKey identityKey;

  public void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    this.identityKey = getIntent().getParcelableExtra("identity_key");
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
