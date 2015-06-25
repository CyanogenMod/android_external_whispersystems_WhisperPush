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

    @Override
    public boolean onUnbind(Intent intent) {
        boolean result = super.onUnbind(intent);
        if (mDisableOnUnbind) {
            // goodbye, cruel world...
            PackageManager pm = getPackageManager();
            pm.setApplicationEnabledSetting(getPackageName(),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0);
        }
        return result;
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
        }
    };
}