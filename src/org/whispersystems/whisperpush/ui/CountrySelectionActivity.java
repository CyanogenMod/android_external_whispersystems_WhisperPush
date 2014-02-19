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

import org.whispersystems.whisperpush.R;

/**
 * The activity that displays a list of supported countries to select from during
 * registration.
 *
 * @author Moxie Marlinspike
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
