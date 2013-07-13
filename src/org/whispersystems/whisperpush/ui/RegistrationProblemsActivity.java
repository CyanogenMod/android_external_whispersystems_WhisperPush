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
package org.whispersystems.whisperpush.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import org.whispersystems.whisperpush.R;

/**
 * Displays a mock dialog that enumerates some common problems with SMS delivery.
 *
 * @author Moxie Marlinspike
 */
public class RegistrationProblemsActivity extends Activity {

  @Override
  public void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    setContentView(R.layout.registration_problems);
    setTitle(getString(R.string.RegistrationProblemsActivity_possible_problems));

    ((Button)findViewById(R.id.close_button)).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        finish();
      }
    });
  }
}
