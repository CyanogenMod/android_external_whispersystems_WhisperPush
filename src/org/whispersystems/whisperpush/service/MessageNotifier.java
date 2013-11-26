package org.whispersystems.whisperpush.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import org.whispersystems.textsecure.crypto.InvalidMessageException;
import org.whispersystems.textsecure.crypto.InvalidVersionException;
import org.whispersystems.textsecure.crypto.protocol.PreKeyWhisperMessage;
import org.whispersystems.textsecure.push.IncomingPushMessage;
import org.whispersystems.whisperpush.R;
import org.whispersystems.whisperpush.contacts.Contact;
import org.whispersystems.whisperpush.contacts.ContactsFactory;
import org.whispersystems.whisperpush.database.DatabaseFactory;
import org.whispersystems.whisperpush.database.PendingApprovalDatabase;
import org.whispersystems.whisperpush.ui.VerifyIdentitiesActivity;
import org.whispersystems.whisperpush.ui.VerifyIdentityActivity;

public class MessageNotifier {

  private static final int NOTIFICATION_ID = 31339;

  public static void updateNotifications(Context context) {
    Cursor cursor = null;

    try {
      cursor = DatabaseFactory.getPendingApprovalDatabase(context).getPending();

      if      (cursor == null || cursor.isAfterLast()) clearNotifications(context);
      else if (cursor.getCount() > 1)                  handleMultipleMessagesNotification(context, cursor);
      else                                             handleSingleMessageNotification(context, cursor);
    } finally {
      if (cursor != null)
        cursor.close();
    }
  }

  private static void handleMultipleMessagesNotification(Context context, Cursor cursor) {
    PendingApprovalDatabase.Reader reader  = DatabaseFactory.getPendingApprovalDatabase(context).readerFor(cursor);
    Notification.Builder           builder = new Notification.Builder(context);

    builder.setSmallIcon(android.R.drawable.ic_dialog_alert);
    builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                                                      android.R.drawable.ic_dialog_alert));
    builder.setContentTitle(String.format(context.getString(R.string.MessageNotifier_d_pending_messages_require_validation),
                                          cursor.getCount()));
    builder.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, VerifyIdentitiesActivity.class), 0));

    Notification.InboxStyle style = new Notification.InboxStyle();

    IncomingPushMessage message;

    while ((message = reader.getNext()) != null) {
      style.addLine(message.getSource());
    }

    builder.setStyle(style);

    builder.setTicker(context.getString(R.string.MessageNotifier_pending_message_requires_validation));

    ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE))
        .notify(NOTIFICATION_ID, builder.build());
  }

  private static void handleSingleMessageNotification(Context context, Cursor cursor) {
    try {
      PendingApprovalDatabase.Reader reader = DatabaseFactory.getPendingApprovalDatabase(context)
                                                             .readerFor(cursor);

      IncomingPushMessage  message = reader.getNext();
      Contact              contact = ContactsFactory.getContactFromNumber(context,
                                                                          message.getSource(),
                                                                          false);

      Notification.Builder builder = new Notification.Builder(context);

      Intent intent = new Intent(context, VerifyIdentityActivity.class);
      intent.putExtra("identity_key", new PreKeyWhisperMessage(message.getBody()).getIdentityKey());
      intent.putExtra("contact", contact);
      intent.setData((Uri.parse("custom://" + System.currentTimeMillis())));

      builder.setSmallIcon(android.R.drawable.ic_dialog_alert);
      builder.setLargeIcon(contact.getAvatar());
      builder.setContentTitle(contact.toShortString());
      builder.setContentText(context.getString(R.string.MessageNotifier_you_have_received_a_message_that_requires_you_to_validate_it));
      builder.setContentIntent(PendingIntent.getActivity(context, 0, intent, 0));

      ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE))
          .notify(NOTIFICATION_ID, builder.build());
    } catch (InvalidVersionException e) {
      Log.w("MessageNotifier", e);
    } catch (InvalidMessageException e) {
      Log.w("MessageNotifier", e);
    }
  }

  private static void clearNotifications(Context context) {
    ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE))
        .cancel(NOTIFICATION_ID);
  }

}
