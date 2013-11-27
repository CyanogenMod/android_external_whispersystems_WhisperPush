package org.whispersystems.whisperpush.service;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import org.whispersystems.textsecure.crypto.AttachmentCipherInputStream;
import org.whispersystems.textsecure.crypto.InvalidMessageException;
import org.whispersystems.textsecure.crypto.MasterSecret;
import org.whispersystems.textsecure.directory.Directory;
import org.whispersystems.textsecure.directory.NotInDirectoryException;
import org.whispersystems.textsecure.push.ContactTokenDetails;
import org.whispersystems.textsecure.push.IncomingPushMessage;
import org.whispersystems.textsecure.push.PushDestination;
import org.whispersystems.textsecure.push.PushMessageProtos.PushMessageContent;
import org.whispersystems.textsecure.push.PushMessageProtos.PushMessageContent.AttachmentPointer;
import org.whispersystems.textsecure.push.PushServiceSocket;
import org.whispersystems.textsecure.util.InvalidNumberException;
import org.whispersystems.whisperpush.attachments.AttachmentManager;
import org.whispersystems.whisperpush.crypto.IdentityMismatchException;
import org.whispersystems.whisperpush.crypto.MasterSecretUtil;
import org.whispersystems.whisperpush.crypto.WhisperCipher;
import org.whispersystems.whisperpush.database.DatabaseFactory;
import org.whispersystems.whisperpush.util.PushServiceSocketFactory;
import org.whispersystems.whisperpush.util.SmsServiceBridge;
import org.whispersystems.whisperpush.util.WhisperPreferences;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class MessageReceiver {

  private final Context context;

  public MessageReceiver(Context context) {
    this.context = context;
  }

  public void handleReceiveMessage(IncomingPushMessage message) {
    if (message == null)
      return;

    updateDirectoryIfNecessary(message);

    try {
      PushMessageContent         content     = getPlaintext(message);
      List<Pair<String, String>> attachments = new LinkedList<Pair<String, String>>();

      if (content.getAttachmentsCount() > 0) {
        attachments = retrieveAttachments(message.getRelay(), content.getAttachmentsList());
      }

      SmsServiceBridge.receivedPushMessage(message.getSource(), message.getDestinations(),
                                           content.getBody(), attachments,
                                           message.getTimestampMillis());
    } catch (IdentityMismatchException e) {
      Log.w("MessageReceiver", e);
      DatabaseFactory.getPendingApprovalDatabase(context).insert(message);
      MessageNotifier.updateNotifications(context);
    } catch (InvalidMessageException e) {
      Log.w("MessageReceiver", e);
      // XXX
    } catch (IOException e) {
      Log.w("MessageReceiver", e);
      // XXX
    }
  }

  private PushMessageContent getPlaintext(IncomingPushMessage message)
      throws IdentityMismatchException, InvalidMessageException
  {
    try {
      MasterSecret    masterSecret    = MasterSecretUtil.getMasterSecret(context);
      String          localNumber     = WhisperPreferences.getLocalNumber(context);
      PushDestination pushDestination = PushDestination.create(context, localNumber, message.getSource());
      WhisperCipher   whisperCipher   = new WhisperCipher(context, masterSecret, pushDestination);

      return whisperCipher.getDecryptedMessage(message);
    } catch (InvalidNumberException e) {
      throw new InvalidMessageException(e);
    }
  }

  private List<Pair<String, String>> retrieveAttachments(String relay, List<AttachmentPointer> attachments)
      throws IOException, InvalidMessageException
  {
    AttachmentManager          attachmentManager = AttachmentManager.getInstance(context);
    PushServiceSocket          socket            = PushServiceSocketFactory.create(context);
    List<Pair<String, String>> results           = new LinkedList<Pair<String, String>>();

    for (AttachmentPointer attachment : attachments) {
      byte[]                      key              = attachment.getKey().toByteArray();
      File file             = socket.retrieveAttachment(relay, attachment.getId());
      AttachmentCipherInputStream attachmentStream = new AttachmentCipherInputStream(file, key);
      String                      storedToken      = attachmentManager.store(attachmentStream);

      file.delete();
      results.add(new Pair<String, String>(storedToken, attachment.getContentType()));
    }

    return results;
  }

  private void updateDirectoryIfNecessary(IncomingPushMessage message) {
    if (!isActiveNumber(message.getSource())) {
      Directory           directory           = Directory.getInstance(context);
      String              contactToken        = directory.getToken(message.getSource());
      String              relay               = message.getRelay();
      ContactTokenDetails contactTokenDetails = new ContactTokenDetails(contactToken, relay);

      directory.setToken(contactTokenDetails, true);
    }
  }

  private boolean isActiveNumber(String e164number) {
    try {
      return Directory.getInstance(context).isActiveNumber(e164number);
    } catch (NotInDirectoryException e) {
      return false;
    }
  }



}
