package org.whispersystems.whisperpush.service;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver.PendingResult;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.whispersystems.textsecure.crypto.MasterSecret;
import org.whispersystems.textsecure.directory.Directory;
import org.whispersystems.textsecure.directory.NotInDirectoryException;
import org.whispersystems.textsecure.push.ContactTokenDetails;
import org.whispersystems.textsecure.push.PushBody;
import org.whispersystems.textsecure.push.PushDestination;
import org.whispersystems.textsecure.push.PushMessageProtos.PushMessageContent;
import org.whispersystems.textsecure.push.PushServiceSocket;
import org.whispersystems.textsecure.push.PushServiceSocket.PushCredentials;
import org.whispersystems.textsecure.util.InvalidNumberException;
import org.whispersystems.textsecure.util.PhoneNumberFormatter;
import org.whispersystems.textsecure.util.Util;
import org.whispersystems.whisperpush.Release;
import org.whispersystems.whisperpush.crypto.MasterSecretUtil;
import org.whispersystems.whisperpush.crypto.WhisperCipher;
import org.whispersystems.whisperpush.sms.OutgoingSmsQueue.OutgoingMessageCandidate;
import org.whispersystems.whisperpush.util.WhisperPushCredentials;

import java.io.IOException;
import java.util.List;

public class MessageSender {

  private static final String PARTS        = "parts";
  private static final String SENT_INTENTS = "sentIntents";

  private final Context context;

  public MessageSender(Context context) {
    this.context = context.getApplicationContext();
  }

  public void handleSendMessage(OutgoingMessageCandidate candidate) {
    Log.w("MessageSender", "Got outgoing message candidate: " + candidate);

    if (candidate == null)
      return;

    Intent sendIntent  = candidate.getIntent();
    String destination = sendIntent.getStringExtra(SendReceiveService.DESTINATION);

    if (!isRegisteredUser(destination)) {
      Log.w("MessageSender", "Not a registered user...");
      abortSendOperation(candidate);
      return;
    }

    try {
      List<String>      messageParts    = sendIntent.getStringArrayListExtra(PARTS);
      PushCredentials   credentials     = WhisperPushCredentials.getInstance();
      PushDestination   pushDestination = PushDestination.create(context, credentials, destination);
      PushServiceSocket socket          = new PushServiceSocket(context, Release.PUSH_URL, credentials);
      PushBody          body            = getEncryptedMessage(pushDestination, messageParts);

      socket.sendMessage(pushDestination, body);

      notifySendComplete(sendIntent);
      completeSendOperation(candidate);
    } catch (IOException e) {
      Log.w("MessageSender", e);
      abortSendOperation(candidate);
    } catch (InvalidNumberException e) {
      Log.w("MessageSender", e);
      abortSendOperation(candidate);
    }
  }

  private void completeSendOperation(OutgoingMessageCandidate candidate) {
    PendingResult pendingResult = candidate.getPendingResult();
    pendingResult.abortBroadcast();
    pendingResult.setResultCode(Activity.RESULT_CANCELED);
    pendingResult.finish();
  }

  private void abortSendOperation(OutgoingMessageCandidate candidate) {
    PendingResult pendingResult = candidate.getPendingResult();
    pendingResult.finish();
  }

  private void notifySendComplete(Intent sendIntent) {
    List<PendingIntent> sentIntents = sendIntent.getParcelableArrayListExtra(SENT_INTENTS);

    if (sentIntents == null) {
      Log.w("MessageSender", "Warning, no sent intents available!");
      return;
    }

    for (PendingIntent sentIntent : sentIntents) {
      try {
        sentIntent.send(Activity.RESULT_OK);
      } catch (PendingIntent.CanceledException e) {
        Log.w("MessageSender", e);
      }
    }
  }

  private PushBody getEncryptedMessage(PushDestination pushDestination,
                                                    List<String> messageParts)
      throws IOException
  {
    PushServiceSocket socket        = new PushServiceSocket(context, Release.PUSH_URL, WhisperPushCredentials.getInstance());
    String            message       = Util.join(messageParts, "");
    byte[]            plaintext     = PushMessageContent.newBuilder().setBody(message).build().toByteArray();
    MasterSecret      masterSecret  = MasterSecretUtil.getMasterSecret(context);
    WhisperCipher     whisperCipher = new WhisperCipher(context, masterSecret, pushDestination);

    return whisperCipher.getEncryptedMessage(socket, plaintext);
  }

  private boolean isRegisteredUser(String number) {
    Log.w("MessageSender", "Number to canonicalize: " + number);
    PushCredentials credentials = WhisperPushCredentials.getInstance();
    Directory       directory   = Directory.getInstance(context);

    String e164number;

    try {
      e164number  = PhoneNumberFormatter.formatNumber(number, credentials.getLocalNumber(context));
    } catch (InvalidNumberException e) {
      Log.w("MessageSender", e);
      return false;
    }

    try {
      return directory.isActiveNumber(e164number);
    } catch (NotInDirectoryException e) {
      try {
        PushServiceSocket   socket              = new PushServiceSocket(context, Release.PUSH_URL, WhisperPushCredentials.getInstance());
        Log.w("MessageSender", "Getting contact token for: " + e164number);
        String              contactToken        = directory.getToken(e164number);
        ContactTokenDetails contactTokenDetails = socket.getContactTokenDetails(contactToken);

        if (contactTokenDetails != null) {
          directory.setToken(contactTokenDetails, true);
          return true;
        } else {
          contactTokenDetails = new ContactTokenDetails(contactToken);
          directory.setToken(contactTokenDetails, false);
          return false;
        }
      } catch (IOException e1) {
        Log.w("MessageSender", e1);
        return false;
      }
    }
  }


}
