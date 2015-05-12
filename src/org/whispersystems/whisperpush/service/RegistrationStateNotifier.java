/**
 * Copyright (C) 2014 The CyanogenMod Project
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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import org.whispersystems.whisperpush.R;
import org.whispersystems.whisperpush.WhisperPush;
import org.whispersystems.whisperpush.ui.GooglePlayServicesUpdateActivity;
import org.whispersystems.whisperpush.ui.RegistrationCompletedActivity;
import org.whispersystems.whisperpush.ui.RegistrationProgressActivity;

/**
 * Helper to display registration progress notifications
 *
 * @author Chris Soyars
 */
public class RegistrationStateNotifier {

    public final int NOTIFICATION_ID = 31338;

    private Context context;
    private Notification.Builder builder;
    private NotificationManager manager;

    public RegistrationStateNotifier(Context context) {
        this.context = context;
        this.manager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private void resetBuilder() {
        this.builder = new Notification.Builder(context);
        this.builder.setSmallIcon(R.drawable.ic_notify);
        this.builder.setAutoCancel(true);
        this.builder.setOngoing(true);
        this.builder.setContentTitle(context.getString(R.string.registration_progress_notification__title_registering));
        attachPendingIntent(RegistrationProgressActivity.class);
    }

    private void sendNotification() {
        builder.setWhen(System.currentTimeMillis());
        manager.notify(NOTIFICATION_ID, builder.build());
    }

    public void clearProgress() {
        builder.setProgress(0, 0, false);
    }

    private void cancelNotification() {
        manager.cancel(NOTIFICATION_ID);
    }

    private void attachPendingIntent(Class<?> clazz) {
        builder.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, clazz), 0));
    }

    private void addVibrateAndSound() {
        builder.setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND);
    }

    public void notify(RegistrationService.RegistrationState state) {
        if (WhisperPush.isActivityVisible()) {
            cancelNotification();
            return;
        }

        resetBuilder();
        switch (state.state) {
            case RegistrationService.RegistrationState.STATE_CONNECTING:
                handleConnecting();
                break;
            case RegistrationService.RegistrationState.STATE_VERIFYING:
                handleVerify();
                break;
            case RegistrationService.RegistrationState.STATE_GENERATING_KEYS:
                handleGeneratingKeys();
                break;
            case RegistrationService.RegistrationState.STATE_GCM_REGISTERING:
                handleGCMRegistering();
                break;
            case RegistrationService.RegistrationState.STATE_GCM_UNSUPPORTED_RECOVERABLE:
                handleGCMRecoverable();
                break;
            case RegistrationService.RegistrationState.STATE_GCM_TIMEOUT:
            case RegistrationService.RegistrationState.STATE_GCM_UNSUPPORTED:
            case RegistrationService.RegistrationState.STATE_NETWORK_ERROR:
            case RegistrationService.RegistrationState.STATE_TIMEOUT:
                handleFailure();
                break;
            case RegistrationService.RegistrationState.STATE_COMPLETE:
                handleComplete();
                break;
        }
        sendNotification();
    }

    public void updateProgress(int progress) {
        if (WhisperPush.isActivityVisible()) {
            cancelNotification();
            return;
        }
        resetBuilder();
        handleVerify();
        builder.setProgress(100, progress, false);
        sendNotification();
    }

    private void handleConnecting() {
        builder.setContentText(context.getString(R.string.registration_progress_activity__connecting));
        builder.setProgress(100, 0, true);
    }

    private void handleVerify() {
        builder.setContentText(context.getString(R.string.registration_progress_activity__waiting_for_sms_verification));
        builder.setProgress(100, 0, false);
    }

    private void handleGeneratingKeys() {
        builder.setContentText(context.getString(R.string.registration_progress_activity__generating_keys));
        builder.setProgress(100, 0, true);
    }

    private void handleGCMRegistering() {
        builder.setContentText(context.getString(R.string.registration_progress_activity__registering_with_server));
        builder.setProgress(100, 0, true);
    }

    private void handleGCMRecoverable() {
        builder.setContentTitle(context.getString(R.string.registration_progress_notification__title_failure));
        builder.setContentText(context.getString(R.string.registration_progress_notification__gms_update));
        attachPendingIntent(GooglePlayServicesUpdateActivity.class);
        addVibrateAndSound();

        clearProgress();
    }

    private void handleComplete() {
        builder.setContentTitle(context.getString(R.string.registration_progress_notification__title_complete));
        builder.setContentText(context.getString(R.string.registration_progress_notification__complete));
        builder.setOngoing(false);
        attachPendingIntent(RegistrationCompletedActivity.class);
        addVibrateAndSound();

        clearProgress();
    }

    private void handleFailure() {
        builder.setContentTitle(context.getString(R.string.registration_progress_notification__title_failure));
        builder.setContentText(context.getString(R.string.registration_progress_notification__failure));
        builder.setOngoing(false);
        addVibrateAndSound();

        clearProgress();
    }
}