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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.whispersystems.libaxolotl.IdentityKeyPair;
import org.whispersystems.libaxolotl.state.PreKeyRecord;
import org.whispersystems.libaxolotl.state.SignedPreKeyRecord;
import org.whispersystems.libaxolotl.util.KeyHelper;
import org.whispersystems.libaxolotl.util.guava.Optional;
import org.whispersystems.textsecure.api.TextSecureAccountManager;
import org.whispersystems.textsecure.api.push.ContactTokenDetails;
import org.whispersystems.whisperpush.R;
import org.whispersystems.whisperpush.WhisperPush;
import org.whispersystems.whisperpush.crypto.IdentityKeyUtil;
import org.whispersystems.whisperpush.crypto.MasterSecret;
import org.whispersystems.whisperpush.crypto.MasterSecretUtil;
import org.whispersystems.whisperpush.crypto.PreKeyUtil;
import org.whispersystems.whisperpush.directory.Directory;
import org.whispersystems.whisperpush.gcm.GcmHelper;
import org.whispersystems.whisperpush.sms.IncomingSmsListener;
import org.whispersystems.whisperpush.util.Util;
import org.whispersystems.whisperpush.util.WhisperPreferences;
import org.whispersystems.whisperpush.util.WhisperServiceFactory;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * The RegisterationService handles the process of PushService registration and verification.
 * If it receives an intent with a REGISTER_NUMBER_ACTION, it does the following through
 * an executor:
 *
 * 1) Generate secrets.
 * 2) Register the specified number and those secrets with the server.
 * 3) Wait for a challenge SMS.
 * 4) Verify the challenge with the server.
 * 5) Start the GCM registration process.
 * 6) Retrieve the current directory.
 *
 * The RegistrationService broadcasts its state throughout this process, and also makes its
 * state available through service binding.  This enables a View to display progress.
 *
 * @author Moxie Marlinspike
 *
 */

public class RegistrationService extends Service {

    public static final String REGISTER_NUMBER_ACTION = "org.thoughtcrime.securesms.RegistrationService.REGISTER_NUMBER";
    public static final String VOICE_REQUESTED_ACTION = "org.thoughtcrime.securesms.RegistrationService.VOICE_REQUESTED";
    public static final String VOICE_REGISTER_ACTION  = "org.thoughtcrime.securesms.RegistrationService.VOICE_REGISTER";

    public static final String CHALLENGE_EVENT        = "org.thoughtcrime.securesms.CHALLENGE_EVENT";
    public static final String CHALLENGE_EXTRA        = "CAAChallenge";

    private static final long   REGISTRATION_TIMEOUT_MILLIS = 120000;
    private static final Object GENERATING_PREKEYS_SEMAPHOR = new Object();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Binder          binder   = new RegistrationServiceBinder();

    private volatile RegistrationState registrationState = new RegistrationState(RegistrationState.STATE_IDLE);

    private volatile Handler                      registrationStateHandler;
    private          RegistrationTimerHandler     registrationTimerHandler;
    private volatile ChallengeReceiver            challengeReceiver;
    private          String                       challenge;
    private          long                         verificationStartTime;
    private          boolean                      generatingPreKeys;
    private          RegistrationStateNotifier    registrationStateNotifier;

    @Override
    public void onCreate() {
        super.onCreate();
        // Add registration number to whitelist
        TelephonyManager tm =
                (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        List<String> regNumbers = Arrays
                .asList(getResources().getStringArray(R.array.default_registration_numbers));
        for (String number : regNumbers) {
            tm.addProtectedSmsAddress(number);
        }

        registrationStateNotifier = new RegistrationStateNotifier(this);
        registrationTimerHandler = new RegistrationTimerHandler();
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        if (intent != null) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    // Check for Play Services
                    switch (GcmHelper.checkPlayServices(RegistrationService.this)) {
                        case GcmHelper.RESULT_USER_RECOVERABLE:
                            setState(new RegistrationState(RegistrationState.STATE_GCM_UNSUPPORTED_RECOVERABLE));
                            return;
                        case GcmHelper.RESULT_UNSUPPORTED:
                            setState(new RegistrationState(RegistrationState.STATE_GCM_UNSUPPORTED));
                            return;
                    }
                    if      (intent.getAction() == null)                        return;
                    else if (intent.getAction().equals(REGISTER_NUMBER_ACTION)) handleRegistrationIntent(intent);
                    else if (intent.getAction().equals(VOICE_REQUESTED_ACTION)) handleVoiceRequestedIntent(intent);
                    else if (intent.getAction().equals(VOICE_REGISTER_ACTION))  handleVoiceRegisterIntent(intent);
                }
            });
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
        shutdown();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void shutdown() {
        shutdownChallengeListener();
        markAsVerifying(false);
        registrationState = new RegistrationState(RegistrationState.STATE_IDLE);
    }

    public synchronized int getSecondsRemaining() {
        long millisPassed;

        if (verificationStartTime == 0) millisPassed = 0;
        else                            millisPassed = System.currentTimeMillis() - verificationStartTime;

        return Math.max((int)(REGISTRATION_TIMEOUT_MILLIS - millisPassed) / 1000, 0);
    }

    public RegistrationState getRegistrationState() {
        return registrationState;
    }

    private void initializeChallengeListener() {
        this.challenge      = null;
        challengeReceiver   = new ChallengeReceiver();
        IntentFilter filter = new IntentFilter(CHALLENGE_EVENT);

        registerReceiver(challengeReceiver, filter);
        enableSmsListener(true);
    }

    private void enableSmsListener(boolean enable) {
        ComponentName incomingSms = new ComponentName(this, IncomingSmsListener.class);
        int state = enable
            ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        getPackageManager().setComponentEnabledSetting(
            incomingSms, state, PackageManager.DONT_KILL_APP);
    }

    private void initializePreKeyGenerator() {
        synchronized (GENERATING_PREKEYS_SEMAPHOR) {
            if (generatingPreKeys) return;
            else                   generatingPreKeys = true;
        }

        new Thread() {
            public void run() {
                Context      context      = RegistrationService.this;
                MasterSecret masterSecret = MasterSecretUtil.getMasterSecret(context);

                if (WhisperPreferences.getInstallId(context) == 0) {
                    WhisperPreferences.setInstallId(context, KeyHelper.generateRegistrationId(false));
                }

                if (!IdentityKeyUtil.hasIdentityKey(context)) {
                    IdentityKeyUtil.generateIdentityKeys(context, masterSecret);
                }

                if (PreKeyUtil.getPreKeys(context, masterSecret).size() < PreKeyUtil.BATCH_SIZE) {
                    PreKeyUtil.generatePreKeys(context, masterSecret);
                }

                synchronized (GENERATING_PREKEYS_SEMAPHOR) {
                    generatingPreKeys = false;
                    GENERATING_PREKEYS_SEMAPHOR.notifyAll();
                }
            }
        }.start();
    }

    private synchronized void shutdownChallengeListener() {
        if (challengeReceiver != null) {
            enableSmsListener(false);
            unregisterReceiver(challengeReceiver);
            challengeReceiver = null;
        }
    }

    private void handleVoiceRequestedIntent(Intent intent) {
        setState(new RegistrationState(RegistrationState.STATE_VOICE_REQUESTED,
                intent.getStringExtra("e164number"),
                intent.getStringExtra("password")));
    }

    private void handleVoiceRegisterIntent(Intent intent) {
        markAsVerifying(true);

        String number       = intent.getStringExtra("e164number"   );
        String password     = intent.getStringExtra("password"     );
        String signalingKey = intent.getStringExtra("signaling_key");

        try {
            initializePreKeyGenerator();

            TextSecureAccountManager manager = WhisperServiceFactory.
                    initAccountManager(getApplicationContext(), number, password);
            handleCommonRegistration(manager, number);

            markAsVerified(number, password, signalingKey);

            setState(new RegistrationState(RegistrationState.STATE_COMPLETE, number));
        } catch (UnsupportedOperationException uoe) {
            Log.w("RegistrationService", uoe);
            setState(new RegistrationState(RegistrationState.STATE_GCM_UNSUPPORTED, number));
        } catch (IOException e) {
            Log.w("RegistrationService", e);
            setState(new RegistrationState(RegistrationState.STATE_NETWORK_ERROR, number));
        }
    }

    private void handleRegistrationIntent(Intent intent) {
        markAsVerifying(true);

        String number = intent.getStringExtra("e164number");

        try {
            String password     = Util.getSecret(18);
            String signalingKey = Util.getSecret(52);

            initializeChallengeListener();
            initializePreKeyGenerator();

            setState(new RegistrationState(RegistrationState.STATE_CONNECTING, number));
            TextSecureAccountManager manager = WhisperServiceFactory.
                    initAccountManager(getApplicationContext(), number, password);
            manager.requestSmsVerificationCode();

            setState(new RegistrationState(RegistrationState.STATE_VERIFYING, number));
            String challenge = waitForChallenge();
            registrationTimerHandler.stop();
            manager.verifyAccount(challenge, signalingKey, false,
                                  WhisperPreferences.getInstallId(this));

            handleCommonRegistration(manager, number);

            markAsVerified(number, password, signalingKey);

            setState(new RegistrationState(RegistrationState.STATE_COMPLETE, number));
        } catch (UnsupportedOperationException uoe) {
            Log.w("RegistrationService", uoe);
            setState(new RegistrationState(RegistrationState.STATE_GCM_UNSUPPORTED, number));
        } catch (AccountVerificationTimeoutException avte) {
            Log.w("RegistrationService", avte);
            registrationTimerHandler.stop();
            setState(new RegistrationState(RegistrationState.STATE_TIMEOUT, number));
        } catch (IOException e) {
            Log.w("RegistrationService", e);
            setState(new RegistrationState(RegistrationState.STATE_NETWORK_ERROR, number));
        } finally {
            shutdownChallengeListener();
        }
    }

    private void handleCommonRegistration(TextSecureAccountManager manager, String number)
            throws IOException
    {
        setState(new RegistrationState(RegistrationState.STATE_GENERATING_KEYS, number));
        MasterSecret       masterSecret    = MasterSecretUtil.getMasterSecret(this);
        List<PreKeyRecord> records         = waitForPreKeys(masterSecret);
        PreKeyRecord       lastResortKey   = PreKeyUtil.generateLastResortKey(this, masterSecret);
        IdentityKeyPair    identityKeyPair = IdentityKeyUtil.getIdentityKeyPair(this, masterSecret);
        SignedPreKeyRecord signedPreKey    = PreKeyUtil.generateSignedPreKey(this, masterSecret, identityKeyPair);
        manager.setPreKeys(identityKeyPair.getPublicKey(), lastResortKey, signedPreKey, records);

        setState(new RegistrationState(RegistrationState.STATE_GCM_REGISTERING, number));
        String gcmRegistrationId = GcmHelper.getRegistrationId(this);
        manager.setGcmId(Optional.of(gcmRegistrationId));

        Set<String>               eligibleContactTokens = Directory.getInstance(this).getPushEligibleContactNumbers(number);
        List<ContactTokenDetails> activeTokens          = manager.getContacts(eligibleContactTokens);

        if (activeTokens != null) {
            for (ContactTokenDetails activeToken : activeTokens) {
                eligibleContactTokens.remove(activeToken.getToken());
            }
            Directory.getInstance(this).setNumbers(activeTokens, eligibleContactTokens);
        }

        DirectoryRefreshListener.schedule(this);
    }

    private synchronized String waitForChallenge() throws AccountVerificationTimeoutException {
        this.verificationStartTime = System.currentTimeMillis();
        registrationTimerHandler.sendEmptyMessageDelayed(0, 1000);

        if (this.challenge == null) {
            try {
                wait(REGISTRATION_TIMEOUT_MILLIS);
            } catch (InterruptedException e) {
                throw new IllegalArgumentException(e);
            }
        }

        if (this.challenge == null)
            throw new AccountVerificationTimeoutException();

        return this.challenge;
    }

    private List<PreKeyRecord> waitForPreKeys(MasterSecret masterSecret) {
        synchronized (GENERATING_PREKEYS_SEMAPHOR) {
            while (generatingPreKeys) {
                try {
                    GENERATING_PREKEYS_SEMAPHOR.wait();
                } catch (InterruptedException e) {
                    throw new AssertionError(e);
                }
            }
        }

        return PreKeyUtil.getPreKeys(this, masterSecret);
    }

    private synchronized void challengeReceived(String challenge) {
        this.challenge = challenge;
        notifyAll();
    }

    private void markAsVerifying(boolean verifying) {
        WhisperPreferences.setVerifying(this, verifying);

        if (verifying) {
            WhisperPreferences.setRegistered(this, false);
        }
    }

    private void markAsVerified(String number, String password, String signalingKey) {
        WhisperPreferences.setVerifying(this, false);
        WhisperPreferences.setRegistered(this, true);
        WhisperPreferences.setLocalNumber(this, number);
        WhisperPreferences.setPushServerPassword(this, password);
        WhisperPreferences.setSignalingKey(this, signalingKey);
    }

    private void setState(RegistrationState state) {
        this.registrationState = state;

        if (WhisperPush.isActivityVisible() && registrationStateHandler != null) {
            registrationStateHandler.obtainMessage(state.state, state).sendToTarget();
        } else {
            registrationStateNotifier.notify(state);
        }
    }

    public void setRegistrationStateHandler(Handler registrationStateHandler) {
        this.registrationStateHandler = registrationStateHandler;
    }

    public class RegistrationServiceBinder extends Binder {
        public RegistrationService getService() {
            return RegistrationService.this;
        }
    }

    private class ChallengeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.w("RegistrationService", "Got a challenge broadcast...");
            challengeReceived(intent.getStringExtra(CHALLENGE_EXTRA));
        }
    }

    public static class RegistrationState {

        public static final int STATE_IDLE                        =  0;
        public static final int STATE_CONNECTING                  =  1;
        public static final int STATE_VERIFYING                   =  2;
        public static final int STATE_TIMER                       =  3;
        public static final int STATE_COMPLETE                    =  4;
        public static final int STATE_TIMEOUT                     =  5;
        public static final int STATE_NETWORK_ERROR               =  6;

        public static final int STATE_GCM_UNSUPPORTED             =  8;
        public static final int STATE_GCM_UNSUPPORTED_RECOVERABLE = 9;
        public static final int STATE_GCM_REGISTERING             =  10;
        public static final int STATE_GCM_TIMEOUT                 = 11;

        public static final int STATE_VOICE_REQUESTED             = 12;
        public static final int STATE_GENERATING_KEYS             = 13;

        public final int    state;
        public final String number;
        public final String password;

        public RegistrationState(int state) {
            this(state, null);
        }

        public RegistrationState(int state, String number) {
            this(state, number, null);
        }

        public RegistrationState(int state, String number, String password) {
            this.state    = state;
            this.number   = number;
            this.password = password;
        }
    }

    private class RegistrationTimerHandler extends Handler {

        private boolean running = true;
        private int lastProgress = -1;

        @Override
        public void handleMessage(Message message) {
            long millis = REGISTRATION_TIMEOUT_MILLIS - (getSecondsRemaining() * 1000);
            int progress = (int)((millis * 100.0f) / REGISTRATION_TIMEOUT_MILLIS);

            if (progress != lastProgress) registrationStateNotifier.updateProgress(progress);
            lastProgress = progress;

            if (running) {
                registrationTimerHandler.sendEmptyMessageDelayed(0, 1000);
            }
        }

        public void stop() {
            running = false;
            this.removeMessages(0);
        }
    }
}
