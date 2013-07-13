/*
 * Copyright (C) 2013 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.whispersystems.whisperpush.loaders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import android.content.AsyncTaskLoader;
import android.content.Context;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import org.whispersystems.textsecure.util.PhoneNumberFormatter;

/**
 * Loader that builds a list of all the countries and their respective country codes
 * supported by libphonenumber.
 *
 * @author Moxie Marlinspike
 */
public class CountryListLoader extends AsyncTaskLoader<ArrayList<Map<String, String>>> {

  public CountryListLoader(Context context) {
    super(context);
  }

  @Override
  public ArrayList<Map<String, String>> loadInBackground() {
    Set<String> regions                    = PhoneNumberUtil.getInstance().getSupportedRegions();
    ArrayList<Map<String, String>> results = new ArrayList<Map<String, String>>(regions.size());

    for (String region : regions) {
      Map<String, String> data = new HashMap<String, String>(2);
      data.put("country_name", PhoneNumberFormatter.getRegionDisplayName(region));
      data.put("country_code", "+" +PhoneNumberUtil.getInstance().getCountryCodeForRegion(region));
      results.add(data);
    }

    Collections.sort(results, new RegionComparator());

    return results;
  }

  private class RegionComparator implements Comparator<Map<String, String>> {
    @Override
    public int compare(Map<String, String> lhs, Map<String, String> rhs) {
      return lhs.get("country_name").compareTo(rhs.get("country_name"));
    }
  }
}
