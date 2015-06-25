/**
 * Copyright (C) 2015 The CyanogenMod Project
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

import org.whispersystems.whisperpush.util.WhisperPreferences;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.RemoteException;

public class UpgradeService extends Service {
    volatile boolean mDisableOnUnbind = false;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /** do this after all clients are disconnected */
    @Override
    public void onDestroy() {
        if (mDisableOnUnbind) {
            // goodbye, cruel world...
            PackageManager pm = getPackageManager();
            pm.setApplicationEnabledSetting(getPackageName(),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0);
        }
    }

    private final IUpgrade.Stub mBinder = new IUpgrade.Stub() {
        @Override
        public boolean isRegistered() throws RemoteException {
            return WhisperPreferences.isRegistered(UpgradeService.this);
        }

        @Override
        public String getLocalNumber() throws RemoteException {
            return WhisperPreferences.getLocalNumber(UpgradeService.this);
        }

        @Override
        public void disableOnUnbind() throws RemoteException {
            mDisableOnUnbind = true;
            stopSelf();
        }
    };
}