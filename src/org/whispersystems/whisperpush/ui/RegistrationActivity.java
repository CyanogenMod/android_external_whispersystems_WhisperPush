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

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.i18n.phonenumbers.AsYouTypeFormatter;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import org.whispersystems.textsecure.api.util.PhoneNumberFormatter;
import org.whispersystems.whisperpush.R;
import org.whispersystems.whisperpush.util.Util;
import org.whispersystems.whisperpush.util.WhisperPreferences;

/**
 * The register account activity.  Prompts this user for their registration information
 * and begins the account registration process.
 *
 * @author Moxie Marlinspike
 *
 */
public class RegistrationActivity extends Activity {

    private static final int PICK_COUNTRY = 1;
    private static final String TWILIO_URL = "http://twilio.com/?utm_source=cyanogenmod&utm_medium=appbadge&utm_campaign=cyanogenverify";

    private AsYouTypeFormatter   countryFormatter;
    private ArrayAdapter<String> countrySpinnerAdapter;
    private Spinner              countrySpinner;
    private TextView             countryCode;
    private TextView             number;
    private Button               createButton;
    private ImageButton          twilioButton;
    private TextView             privacyPolicy;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if (WhisperPreferences.isRegistered(this)) {
            startActivity(new Intent(this, RegistrationCompletedActivity.class));
            finish();
        }

        setContentView(R.layout.registration_activity);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(getString(R.string.RegistrationActivity_connect_with_textsecure));
        }

        initializeResources();
        initializeSpinner();
        initializeNumber();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_COUNTRY && resultCode == RESULT_OK && data != null) {
            this.countryCode.setText(data.getIntExtra("country_code", 1)+"");
            setCountryDisplay(data.getStringExtra("country_name"));
            setCountryFormatter(data.getIntExtra("country_code", 1));
        }
    }

    private void initializeResources() {
        this.countrySpinner = (Spinner)findViewById(R.id.country_spinner);
        this.countryCode    = (TextView)findViewById(R.id.country_code);
        this.number         = (TextView)findViewById(R.id.number);
        this.createButton   = (Button)findViewById(R.id.registerButton);
        this.twilioButton   = (ImageButton) findViewById(R.id.twilio_button);
        this.privacyPolicy  = (TextView) findViewById(R.id.privacy_policy);

        this.countryCode.addTextChangedListener(new CountryCodeChangedListener());
        this.number.addTextChangedListener(new NumberChangedListener());
        this.createButton.setOnClickListener(new CreateButtonListener());
        this.twilioButton.setOnClickListener(new TwilioButtonListener());
        this.privacyPolicy.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void initializeSpinner() {
        this.countrySpinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        this.countrySpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        setCountryDisplay(getString(R.string.RegistrationActivity_select_your_country));

        this.countrySpinner.setAdapter(this.countrySpinnerAdapter);
        this.countrySpinner.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    Intent intent = new Intent(RegistrationActivity.this, CountrySelectionActivity.class);
                    startActivityForResult(intent, PICK_COUNTRY);
                }
                return true;
            }
        });
    }

    private void initializeNumber() {
        String localNumber = ((TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE))
                .getLine1Number();

        if (!Util.isEmpty(localNumber) && !localNumber.startsWith("+")) {
            if (localNumber.length() == 10) localNumber = "+1" + localNumber;
            else                            localNumber = "+"  + localNumber;
        }

        try {
            if (!Util.isEmpty(localNumber)) {
                PhoneNumberUtil numberUtil                = PhoneNumberUtil.getInstance();
                Phonenumber.PhoneNumber localNumberObject = numberUtil.parse(localNumber, null);

                if (localNumberObject != null) {
                    this.countryCode.setText(localNumberObject.getCountryCode()+"");
                    this.number.setText(localNumberObject.getNationalNumber()+"");
                }
            }
        } catch (NumberParseException npe) {
            Log.w("CreateAccountActivity", npe);
        }
    }

    private void setCountryDisplay(String value) {
        this.countrySpinnerAdapter.clear();
        this.countrySpinnerAdapter.add(value);
    }

    private void setCountryFormatter(int countryCode) {
        PhoneNumberUtil util = PhoneNumberUtil.getInstance();
        String regionCode    = util.getRegionCodeForCountryCode(countryCode);

        if (regionCode == null) this.countryFormatter = null;
        else                    this.countryFormatter = util.getAsYouTypeFormatter(regionCode);
    }

    private String getConfiguredE164Number() {
        return PhoneNumberFormatter.formatE164(countryCode.getText().toString(),
                number.getText().toString());
    }

    private class CreateButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            final RegistrationActivity self = RegistrationActivity.this;

            if (TextUtils.isEmpty(countryCode.getText())) {
                Toast.makeText(self,
                        getString(R.string.RegistrationActivity_you_must_specify_your_country_code),
                        Toast.LENGTH_LONG).show();
                return;
            }

            if (TextUtils.isEmpty(number.getText())) {
                Toast.makeText(self,
                        getString(R.string.RegistrationActivity_you_must_specify_your_phone_number),
                        Toast.LENGTH_LONG).show();
                return;
            }

            final String e164number = getConfiguredE164Number();

            if (!PhoneNumberFormatter.isValidNumber(e164number)) {
                Util.showAlertDialog(self,
                        getString(R.string.RegistrationActivity_invalid_number),
                        String.format(getString(R.string.RegistrationActivity_the_number_you_specified_s_is_invalid),
                                e164number));
                return;
            }

            if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(self) != ConnectionResult.SUCCESS) {
                Util.showAlertDialog(self, getString(R.string.RegistrationActivity_unsupported),
                        getString(R.string.RegistrationActivity_sorry_this_device_is_not_supported_for_data_messaging));
                return;
            }

            AlertDialog.Builder dialog = new AlertDialog.Builder(self);
            dialog.setMessage(String.format(getString(R.string.RegistrationActivity_we_will_now_verify_that_the_following_number_is_associated_with_your_device_s),
                    PhoneNumberFormatter.getInternationalFormatFromE164(e164number)));
            dialog.setPositiveButton(getString(R.string.RegistrationActivity_continue),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(self, RegistrationProgressActivity.class);
                            intent.putExtra("e164number", e164number);
                            startActivity(intent);
                            finish();
                        }
                    });
            dialog.setNegativeButton(getString(R.string.RegistrationActivity_edit), null);
            dialog.show();
        }
    }

    private class CountryCodeChangedListener implements TextWatcher {
        @Override
        public void afterTextChanged(Editable s) {
            if (TextUtils.isEmpty(s)) {
                setCountryDisplay(getString(R.string.RegistrationActivity_select_your_country));
                countryFormatter = null;
                return;
            }

            int countryCode   = Integer.parseInt(s.toString());
            String regionCode = PhoneNumberUtil.getInstance().getRegionCodeForCountryCode(countryCode);

            setCountryFormatter(countryCode);
            setCountryDisplay(PhoneNumberFormatter.getRegionDisplayName(regionCode));

            if (!Util.isEmpty(regionCode) && !regionCode.equals("ZZ")) {
                number.requestFocus();
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }

    private class NumberChangedListener implements TextWatcher {

        @Override
        public void afterTextChanged(Editable s) {
            if (countryFormatter == null)
                return;

            if (TextUtils.isEmpty(s) || !TextUtils.isDigitsOnly(s))
                return;

            countryFormatter.clear();

            String number          = s.toString().replaceAll("[^\\d.]", "");
            String formattedNumber = null;

            for (int i=0;i<number.length();i++) {
                formattedNumber = countryFormatter.inputDigit(number.charAt(i));
            }

            if (!s.toString().equals(formattedNumber)) {
                s.replace(0, s.length(), formattedNumber);
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }
    }

    private class TwilioButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(TWILIO_URL));
            startActivity(intent);
        }
    }
}
