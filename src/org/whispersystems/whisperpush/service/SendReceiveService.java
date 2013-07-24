package org.whispersystems.whisperpush.service;


import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver.PendingResult;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.util.Pair;

import org.whispersystems.textsecure.push.IncomingPushMessage;
import org.whispersystems.textsecure.push.PushServiceSocket;
import org.whispersystems.textsecure.util.PhoneNumberFormatter;
import org.whispersystems.textsecure.util.Util;
import org.whispersystems.whisperpush.attachments.AttachmentManager;
import org.whispersystems.whisperpush.sms.OutgoingSmsQueue;
import org.whispersystems.whisperpush.sms.OutgoingSmsQueue.OutgoingMessageCandidate;
import org.whispersystems.whisperpush.util.SmsServiceBridge;
import org.whispersystems.whisperpush.util.WhisperPreferences;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SendReceiveService extends Service {

  public static final String RECEIVE_SMS = "org.whispersystems.SendReceiveService.RECEIVE_SMS";
  public static final String SEND_SMS    = "org.whispersystems.SendReceiveService.SEND_SMS";

  public  static final String DESTINATION  = "destAddr";
  private static final String PARTS        = "parts";
  private static final String SENT_INTENTS = "sentIntents";

  private final ExecutorService executor = Executors.newCachedThreadPool();

  @Override
  public int onStartCommand(final Intent intent, int flags, int startId) {
    if (intent != null) {
      executor.execute(new Runnable() {
        @Override
        public void run() {
          if      (RECEIVE_SMS.equals(intent.getAction())) handleReceiveSms(intent);
          else if (SEND_SMS.equals(intent.getAction()))    handleSendSms();
        }
      });
    }

    return START_NOT_STICKY;
  }

  private void handleReceiveSms(Intent intent) {
    IncomingPushMessage message = intent.getParcelableExtra("message");

    if (message == null)
      return;

    List<String> attachmentTokens       = new LinkedList<String>();
    List<String> attachmentContentTypes = new LinkedList<String>();

    if (message.hasAttachments()) {
      try {
        String                   localNumber    = WhisperPreferences.getLocalNumber(this);
        String                   pushPassphrase = WhisperPreferences.getPushServerPassword(this);
        PushServiceSocket        socket         = new PushServiceSocket(this, localNumber, pushPassphrase);
        List<Pair<File, String>> attachments    = socket.retrieveAttachments(message.getAttachments());

        storeAttachments(attachments, attachmentTokens, attachmentContentTypes);
      } catch (IOException e) {
        Log.w("SendReceiveService", e);
      }
    }

    SmsServiceBridge.receivedPushMessage(message.getSource(), message.getDestinations(),
                                         message.getMessageText(), attachmentTokens,
                                         attachmentContentTypes, message.getTimestampMillis());
  }

  private void handleSendSms() {
    OutgoingMessageCandidate candidate = OutgoingSmsQueue.getInstance().get();

    Log.w("SendReceiveService", "Got outgoing message candidate: " + candidate);

    if (candidate == null)
      return;

    Intent        sendIntent    = candidate.getIntent();
    PendingResult pendingResult = candidate.getPendingResult();

    try {
      List<String>        messageParts   = sendIntent.getStringArrayListExtra(PARTS);
      String              destination    = sendIntent.getStringExtra(DESTINATION);
      List<PendingIntent> sentIntents    = sendIntent.getParcelableArrayListExtra(SENT_INTENTS);
      String              localNumber    = WhisperPreferences.getLocalNumber(this);
      String              pushPassphrase = WhisperPreferences.getPushServerPassword(this);

      PushServiceSocket socket    = new PushServiceSocket(this, localNumber, pushPassphrase);
      String formattedDestination = PhoneNumberFormatter.formatNumber(destination, localNumber);
      String message              = Util.join(messageParts, "");

      socket.sendMessage(formattedDestination, message);

      for (PendingIntent sentIntent : sentIntents) {
        try {
          sentIntent.send(Activity.RESULT_OK);
        } catch (PendingIntent.CanceledException e) {
          Log.w("SendReceiveService", e);
        }
      }

      pendingResult.abortBroadcast();
      pendingResult.setResultCode(Activity.RESULT_CANCELED);
      pendingResult.finish();
    } catch (IOException e) {
      Log.w("SendReceiveService", e);
      pendingResult.finish();
    }
  }

  private void storeAttachments(List<Pair<File, String>> attachments,
                                List<String> attachmentTokens,
                                List<String> attachmentContentTypes)
      throws IOException
  {
    AttachmentManager attachmentManager = AttachmentManager.getInstance(this);

    for (Pair<File, String> attachment : attachments) {
      String token = attachmentManager.store(attachment.first);
      attachmentTokens.add(token);
      attachmentContentTypes.add(attachment.second);
    }
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
}
