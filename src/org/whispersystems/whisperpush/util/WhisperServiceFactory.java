package org.whispersystems.whisperpush.util;

import org.whispersystems.libaxolotl.util.guava.Optional;
import org.whispersystems.textsecure.api.TextSecureAccountManager;
import org.whispersystems.textsecure.api.TextSecureMessageReceiver;
import org.whispersystems.textsecure.api.TextSecureMessageSender;
import org.whispersystems.textsecure.api.TextSecureMessageSender.EventListener;
import org.whispersystems.whisperpush.Release;
import org.whispersystems.whisperpush.database.WPAxolotlStore;

import android.content.Context;

public class WhisperServiceFactory {
    public static TextSecureMessageSender createMessageSender(Context context) {
        return new TextSecureMessageSender(Release.PUSH_URL,
                                           new WhisperPushTrustStore(context),
                                           WhisperPreferences.getLocalNumber(context),
                                           WhisperPreferences.getPushServerPassword(context),
                                           WPAxolotlStore.getInstance(context),
                                           Optional.<EventListener>absent());
    }

    public static TextSecureMessageReceiver createMessageReceiver(Context context) {
        return new TextSecureMessageReceiver(Release.PUSH_URL,
                                             new WhisperPushTrustStore(context),
                                             WhisperPreferences.getLocalNumber(context),
                                             WhisperPreferences.getPushServerPassword(context),
                                             WhisperPreferences.getSignalingKey(context));
    }

    public static TextSecureAccountManager createAccountManager(Context context) {
        return new TextSecureAccountManager(Release.PUSH_URL,
                                            new WhisperPushTrustStore(context),
                                            WhisperPreferences.getLocalNumber(context),
                                            WhisperPreferences.getPushServerPassword(context));
    }

    public static TextSecureAccountManager initAccountManager(Context context, String number, String password) {
        WhisperPreferences.setLocalNumber(context, number);
        WhisperPreferences.setPushServerPassword(context, password);
        return createAccountManager(context);
    }
}