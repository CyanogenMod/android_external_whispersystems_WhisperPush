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
package org.whispersystems.whisperpush.service;

import java.util.Date;

import org.whispersystems.libaxolotl.InvalidMessageException;
import org.whispersystems.libaxolotl.InvalidVersionException;
import org.whispersystems.libaxolotl.protocol.PreKeyWhisperMessage;
import org.whispersystems.textsecure.api.messages.TextSecureEnvelope;
import org.whispersystems.whisperpush.R;
import org.whispersystems.whisperpush.contacts.Contact;
import org.whispersystems.whisperpush.contacts.ContactsFactory;
import org.whispersystems.whisperpush.database.DatabaseFactory;
import org.whispersystems.whisperpush.database.PendingApprovalDatabase;
import org.whispersystems.whisperpush.ui.ErrorAndResetActivity;
import org.whispersystems.whisperpush.ui.PreferenceActivity;
import org.whispersystems.whisperpush.ui.VerifyIdentitiesActivity;
import org.whispersystems.whisperpush.ui.VerifyIdentityActivity;
import org.whispersystems.whisperpush.ui.ViewNewIdentityActivity;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

public class MessageNotifier {

    private static final int NOTIFICATION_ID = 31339;
    private static final int PROBLEM_ID      = 31334;

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

    public static void notifyUnRegistered(Context context) {
        Notification notification = new Notification.BigTextStyle(
                new Notification.Builder(context)
                        .setSmallIcon(R.drawable.ic_notify)
                        .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                                R.drawable.ic_notify))
                        .setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, PreferenceActivity.class), 0))
                        .setContentTitle(context.getString(R.string.MessageNotifier_user_unregistered_from_service_title))
                        .setContentText(context.getString(R.string.MessageNotifier_user_unregistered_from_service_content))
        ).bigText(context.getString(R.string.MessageNotifier_user_unregistered_from_service_content)).build();

        ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(NOTIFICATION_ID, notification);
    }

    public static void notifyProblem(Context context, Contact contact, String description) {
        Notification notification = new Notification.BigTextStyle(
                new Notification.Builder(context)
                        .setSmallIcon(R.drawable.ic_notify)
                        .setLargeIcon(contact.getAvatar())
                        .setContentTitle(contact.toShortString())
                        .setContentText(description)
        ).bigText(description).build();

        ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(PROBLEM_ID, notification);
    }

    public static void notifyBlacklisted(Context context, String number) {
        Notification notification = new Notification.BigTextStyle(
                new Notification.Builder(context)
                        .setSmallIcon(R.drawable.ic_notify)
                        .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                                R.drawable.ic_notify))
                        .setContentTitle(context.getString(
                                R.string.MessageNotifier_user_received_message_from_blacklisted_number_title))
                        .setContentText(String.format(context.getString(
                                R.string.MessageNotifier_user_received_message_from_blacklisted_number_content), number))
        ).bigText(String.format(context.getString(
                R.string.MessageNotifier_user_received_message_from_blacklisted_number_content), number)).build();

        ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(PROBLEM_ID, notification);
    }

    public static void notifyProblem(Context context, String title, String description) {
        Notification notification = new Notification.BigTextStyle(
                new Notification.Builder(context)
                        .setSmallIcon(R.drawable.ic_notify)
                        .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                                R.drawable.ic_notify))
                        .setContentTitle(title)
                        .setContentText(description)
        ).bigText(description).build();

        ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(PROBLEM_ID, notification);
    }

    public static void notifyProblemAndUnregister(Context context, String title, String description) {
        Notification notification = new Notification.BigTextStyle(
                new Notification.Builder(context)
                        .setSmallIcon(R.drawable.ic_notify)
                        .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                                R.drawable.ic_notify))
                        .setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, ErrorAndResetActivity.class), 0))
                        .setContentTitle(title)
                        .setContentText(description)
        ).bigText(description).build();

        ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(PROBLEM_ID, notification);
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

        TextSecureEnvelope message;

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

            TextSecureEnvelope message = reader.getNext();
            Contact            contact = ContactsFactory.
                    getContactFromNumber(context, message.getSource(), false);

            Notification.Builder builder = new Notification.Builder(context);

            Intent intent = new Intent(context, VerifyIdentityActivity.class);
            intent.putExtra("identity_key", new PreKeyWhisperMessage(message.getMessage()).getIdentityKey().serialize());
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

    public static void notifyNewSessionIncoming(Context context, TextSecureEnvelope message) {
        Notification.Builder builder = new Notification.Builder(context);
        try {
            Contact contact = ContactsFactory.getContactFromNumber(context, message.getSource(), false);
            Intent intent = new Intent(context, ViewNewIdentityActivity.class);
            intent.putExtra("identity_key", new PreKeyWhisperMessage(message.getMessage()).getIdentityKey().serialize());
            intent.putExtra("contact", contact);
            intent.setData((Uri.parse("custom://" + System.currentTimeMillis())));
            PendingIntent verifyPi = PendingIntent.getActivity(context, 0, intent, 0);

            builder.setSmallIcon(R.drawable.ic_notify);
            builder.setLargeIcon(contact.getAvatar());
            builder.setContentTitle(context.getString(R.string.MessageNotifier_new_session_title));

            String notificationText = String.format(context.getString(R.string.MessageNotifier_new_session__incoming_text), contact.toShortString());
            builder.setContentText(notificationText);
            builder.setStyle(new Notification.BigTextStyle().bigText(notificationText));

            // Add verify pending intent
            builder.setContentIntent(verifyPi);

            builder.setAutoCancel(true);
            builder.setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND | Notification.DEFAULT_LIGHTS);

            ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE))
                    .notify((int) new Date().getTime(), builder.build());
        } catch (InvalidVersionException e) {
            Log.w("MessageNotifier", e);
        } catch (InvalidMessageException e) {
            Log.w("MessageNotifier", e);
        }
    }

}
