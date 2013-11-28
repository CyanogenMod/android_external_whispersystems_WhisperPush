package org.whispersystems.whisperpush.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import org.whispersystems.whisperpush.R;
import org.whispersystems.whisperpush.util.WhisperPreferences;

public class RegistrationCompletedActivity extends Activity implements View.OnClickListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registration_completed_activity);

        findViewById(R.id.registerAgainButton).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        WhisperPreferences.setRegistered(this, false);
        startActivity(new Intent(this, RegistrationActivity.class));
        finish();
    }
}
