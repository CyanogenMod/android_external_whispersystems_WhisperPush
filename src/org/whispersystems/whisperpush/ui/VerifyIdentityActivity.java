package org.whispersystems.whisperpush.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.whispersystems.textsecure.crypto.IdentityKey;
import org.whispersystems.textsecure.crypto.InvalidKeyException;
import org.whispersystems.textsecure.crypto.InvalidVersionException;
import org.whispersystems.textsecure.crypto.MasterSecret;
import org.whispersystems.textsecure.crypto.protocol.PreKeyBundleMessage;
import org.whispersystems.textsecure.push.IncomingPushMessage;
import org.whispersystems.whisperpush.R;
import org.whispersystems.whisperpush.contacts.Contact;
import org.whispersystems.whisperpush.crypto.MasterSecretUtil;
import org.whispersystems.whisperpush.crypto.MessagePeer;
import org.whispersystems.whisperpush.database.DatabaseFactory;
import org.whispersystems.whisperpush.database.PendingApprovalDatabase;
import org.whispersystems.whisperpush.service.MessageNotifier;
import org.whispersystems.whisperpush.service.SendReceiveService;

public class VerifyIdentityActivity extends KeyScanningActivity {

  private Contact contact;

  public void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    getActionBar().setDisplayHomeAsUpEnabled(true);
    setContentView(R.layout.verify_identity_activity);

    initializeResources();
  }

  private void initializeResources() {
    ImageView imageView       = (ImageView) findViewById(R.id.avatar              );
    TextView  contactText     = (TextView ) findViewById(R.id.contact_name        );
    TextView  fingerprintText = (TextView ) findViewById(R.id.identity_fingerprint);
    Button    validButton     = (Button   ) findViewById(R.id.valid_button        );
    Button    invalidButton   = (Button   ) findViewById(R.id.invalid_button      );

    contact = getIntent().getParcelableExtra("contact");

    imageView.setImageBitmap(contact.getAvatar());
    contactText.setText(contact.toShortString());
    fingerprintText.setText(identityKey.getFingerprint());
    validButton.setOnClickListener(new MarkValidListener());
    invalidButton.setOnClickListener(new MarkInvalidListener());
  }

  private class MarkValidListener implements View.OnClickListener {
    @Override
    public void onClick(View v) {
      new ReleaseMatchingKeysTask(VerifyIdentityActivity.this, identityKey, contact).execute();
    }
  }

  private class MarkInvalidListener implements View.OnClickListener {
    @Override
    public void onClick(View v) {
      final Context context = VerifyIdentityActivity.this;
      AlertDialog.Builder builder = new AlertDialog.Builder(context);
      builder.setTitle(getString(R.string.VerifyIdentityActivity_discard_messages));
      builder.setMessage(getString(R.string.VerifyIdentityActivity_are_you_sure_you_would_like_to_mark_this_identity_fingerprint_as_invalid_and_discard));
      builder.setIcon(android.R.drawable.ic_dialog_alert);
      builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          new DiscardMatchingKeysTask(VerifyIdentityActivity.this, contact, identityKey).execute();
        }
      });
      builder.setNegativeButton(android.R.string.cancel, null);
      builder.show();
    }
  }

  private class DiscardMatchingKeysTask extends AsyncTask<Void, Void, Void> {

    private final Context      context;
    private final Contact      contact;
    private final IdentityKey  identityKey;

    private ProgressDialog progressDialog;

    public DiscardMatchingKeysTask(Context context, Contact contact, IdentityKey identityKey) {
      this.context      = context;
      this.contact      = contact;
      this.identityKey  = identityKey;
    }

    @Override
    protected void onPreExecute() {
      progressDialog = ProgressDialog.show(context,
                                           getString(R.string.VerifyIdentityActivity_processing),
                                           getString(R.string.VerifyIdentityActivity_discarding_messages));
    }

    @Override
    protected Void doInBackground(Void... params) {
      final PendingApprovalDatabase database = DatabaseFactory.getPendingApprovalDatabase(context);

      Cursor cursor = null;

      try {
        cursor = database.getPending(contact.getNumber());

        PendingApprovalDatabase.Reader reader = database.readerFor(cursor);
        IncomingPushMessage message;

        while ((message = reader.getNext()) != null) {
          try {
            PreKeyBundleMessage keyExchangeMessage = new PreKeyBundleMessage(message.getBody());

            if (keyExchangeMessage.getIdentityKey().equals(identityKey)) {
              database.delete(reader.getCurrentId());
            }
          } catch (InvalidKeyException e) {
            Log.w("VerifyIdentityActivity", e);
          } catch (InvalidVersionException e) {
            Log.w("VerifyIdentityActivity", e);
          }
        }
      } finally {
        if (cursor != null)
          cursor.close();
      }

      MessageNotifier.updateNotifications(context);
      return null;
    }

    @Override
    protected void onPostExecute(Void result) {
      if (progressDialog != null)
        progressDialog.dismiss();

      finish();
    }
  }

  private class ReleaseMatchingKeysTask extends AsyncTask<Void, Void, Void> {

    private final Context     context;
    private final MasterSecret masterSecret;
    private final IdentityKey identityKey;
    private final Contact     contact;

    private ProgressDialog progressDialog;

    public ReleaseMatchingKeysTask(Context context,
                                   IdentityKey identityKey,
                                   Contact contact)
    {
      this.context     = context;
      this.masterSecret = MasterSecretUtil.getMasterSecret(context);
      this.identityKey = identityKey;
      this.contact     = contact;
    }

    @Override
    protected void onPreExecute() {
      progressDialog = ProgressDialog.show(context,
                                           getString(R.string.VerifyIdentityActivity_processing),
                                           getString(R.string.VerifyIdentityActivity_processing_identity_key),
                                           true, false);
    }

    @Override
    protected Void doInBackground(Void... params) {
      PendingApprovalDatabase database = DatabaseFactory.getPendingApprovalDatabase(context);
      MessagePeer             address  = new MessagePeer(context, contact.getNumber());

      DatabaseFactory.getIdentityDatabase(context).saveIdentity(masterSecret, address, identityKey);

      Cursor                         cursor = database.getPending(contact.getNumber());
      PendingApprovalDatabase.Reader reader = database.readerFor(cursor);

      IncomingPushMessage message;

      while ((message = reader.getNext()) != null) {
        try {
          PreKeyBundleMessage keyExchange = new PreKeyBundleMessage(message.getBody());

          if (keyExchange.getIdentityKey().equals(identityKey)) {
            Intent intent = new Intent(context, SendReceiveService.class);
            intent.setAction(SendReceiveService.RECEIVE_SMS);
            intent.putExtra("message", message);
            context.startService(intent);
            database.delete(reader.getCurrentId());
          }
        } catch (InvalidKeyException e) {
          Log.w("VerifyIdentityActivity", e);
        } catch (InvalidVersionException e) {
          Log.w("VerifyIdentityActivity", e);
        }
      }

      MessageNotifier.updateNotifications(context);
      return null;
    }

    @Override
    protected void onPostExecute(Void results) {
      if (progressDialog != null)
        progressDialog.dismiss();

      finish();
    }
  }
}
