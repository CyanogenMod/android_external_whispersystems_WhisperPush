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

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import org.whispersystems.textsecure.crypto.IdentityKey;
import org.whispersystems.textsecure.crypto.MasterSecret;
import org.whispersystems.textsecure.crypto.PreKeyUtil;
import org.whispersystems.textsecure.directory.Directory;
import org.whispersystems.textsecure.push.ContactTokenDetails;
import org.whispersystems.textsecure.push.PushServiceSocket;
import org.whispersystems.textsecure.storage.PreKeyRecord;
import org.whispersystems.textsecure.util.Util;
import org.whispersystems.whisperpush.crypto.IdentityKeyUtil;
import org.whispersystems.whisperpush.crypto.MasterSecretUtil;
import org.whispersystems.whisperpush.gcm.GcmHelper;
import org.whispersystems.whisperpush.util.PushServiceSocketFactory;
import org.whispersystems.whisperpush.util.WhisperPreferences;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        challengeReceiver = new ChallengeReceiver();
        IntentFilter filter = new IntentFilter(CHALLENGE_EVENT);
        registerReceiver(challengeReceiver, filter);
    }

    private void initializePreKeyGenerator() {
        synchronized (GENERATING_PREKEYS_SEMAPHOR) {
            if (generatingPreKeys) return;
            else                   generatingPreKeys = true;
        }

        new Thread() {
            public void run() {
                Context     context       = RegistrationService.this;
                MasterSecret masterSecret = MasterSecretUtil.getMasterSecret(context);

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

            PushServiceSocket socket = PushServiceSocketFactory.create(this, number, password);
            handleCommonRegistration(socket, number);

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
            PushServiceSocket socket = PushServiceSocketFactory.create(this, number, password);
            socket.createAccount(false);

            setState(new RegistrationState(RegistrationState.STATE_VERIFYING, number));
            String challenge = waitForChallenge();
            registrationTimerHandler.stop();
            socket.verifyAccount(challenge, signalingKey);

            handleCommonRegistration(socket, number);

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

    private void handleCommonRegistration(PushServiceSocket socket, String number)
            throws IOException
    {
        setState(new RegistrationState(RegistrationState.STATE_GENERATING_KEYS, number));
        MasterSecret       masterSecret  = MasterSecretUtil.getMasterSecret(this);
        List<PreKeyRecord> records       = waitForPreKeys(masterSecret);
        PreKeyRecord       lastResortKey = PreKeyUtil.generateLastResortKey(this, masterSecret);
        IdentityKey        identityKey   = IdentityKeyUtil.getIdentityKey(this);
        socket.registerPreKeys(identityKey, lastResortKey, records);

        setState(new RegistrationState(RegistrationState.STATE_GCM_REGISTERING, number));
        String gcmRegistrationId = GcmHelper.getRegistrationId(this);
        socket.registerGcmId(gcmRegistrationId);

        Set<String>               eligibleContactTokens = Directory.getInstance(this).getPushEligibleContactTokens(number);
        List<ContactTokenDetails> activeTokens          = socket.retrieveDirectory(eligibleContactTokens);

        if (activeTokens != null) {
            for (ContactTokenDetails activeToken : activeTokens) {
                eligibleContactTokens.remove(activeToken.getToken());
            }
            Directory.getInstance(this).setTokens(activeTokens, eligibleContactTokens);
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

        if (registrationStateHandler != null) {
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
