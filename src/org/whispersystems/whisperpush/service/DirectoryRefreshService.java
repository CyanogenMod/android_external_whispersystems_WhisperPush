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
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import org.whispersystems.textsecure.directory.Directory;
import org.whispersystems.textsecure.push.ContactTokenDetails;
import org.whispersystems.textsecure.push.PushServiceSocket;
import org.whispersystems.whisperpush.util.PushServiceSocketFactory;
import org.whispersystems.whisperpush.util.WhisperPreferences;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DirectoryRefreshService extends Service {

    public  static final String REFRESH_ACTION = "org.whispersystems.whisperpush.REFRESH_ACTION";

    private static final Executor executor = Executors.newSingleThreadExecutor();

    @Override
    public int onStartCommand (Intent intent, int flags, int startId) {
        if (REFRESH_ACTION.equals(intent.getAction())) {
            handleRefreshAction();
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void handleRefreshAction() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Directory Refresh");
        wakeLock.acquire();

        executor.execute(new RefreshRunnable(wakeLock));
    }

    private class RefreshRunnable implements Runnable {
        private final PowerManager.WakeLock wakeLock;
        private final Context context;

        public RefreshRunnable(PowerManager.WakeLock wakeLock) {
            this.wakeLock = wakeLock;
            this.context  = DirectoryRefreshService.this.getApplicationContext();
        }

        public void run() {
            try {
                Log.w("DirectoryRefreshService", "Refreshing directory...");
                Directory         directory   = Directory.getInstance(context);
                String            localNumber = WhisperPreferences.getLocalNumber(context);
                PushServiceSocket socket      = PushServiceSocketFactory.create(context);

                Set<String>               eligibleContactTokens = directory.getPushEligibleContactTokens(localNumber);
                List<ContactTokenDetails> activeTokens          = socket.retrieveDirectory(eligibleContactTokens);

                if (activeTokens != null) {
                    for (ContactTokenDetails activeToken : activeTokens) {
                        eligibleContactTokens.remove(activeToken.getToken());
                    }

                    directory.setTokens(activeTokens, eligibleContactTokens);
                }

                Log.w("DirectoryRefreshService", "Directory refresh complete...");
            } finally {
                if (wakeLock != null && wakeLock.isHeld())
                    wakeLock.release();
            }
        }
    }
}
