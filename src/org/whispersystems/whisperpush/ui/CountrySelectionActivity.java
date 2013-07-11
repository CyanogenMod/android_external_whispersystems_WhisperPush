package org.whispersystems.whisperpush.ui;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import org.whispersystems.whisperpush.R;

/**
 * The activity that displays a list of supported countries to select from during
 * registration.
 */
public class CountrySelectionActivity extends Activity
    implements CountrySelectionFragment.CountrySelectedListener
{

  @Override
  public void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    this.setContentView(R.layout.country_selection);
  }

  @Override
  public void countrySelected(String countryName, int countryCode) {
    Intent result = getIntent();
    result.putExtra("country_name", countryName);
    result.putExtra("country_code", countryCode);

    this.setResult(RESULT_OK, result);
    this.finish();
  }
}
