package com.forwiz.nursetree.util;

/**
 * Created by YunHo on 2017-05-10.
 */

import android.content.Context;
import android.os.PowerManager;
import android.view.WindowManager;


public class PowerWakelock {
    private static PowerManager.WakeLock sCpuWakeLock;

    public static void acquireCpuWakeLock(Context context) {

        if (sCpuWakeLock != null) {
            return;
        }
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        sCpuWakeLock = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
                        PowerManager.ACQUIRE_CAUSES_WAKEUP |
                        PowerManager.ON_AFTER_RELEASE|
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD|
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON, "hello");

        sCpuWakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);
    }

    static void releaseCpuLock() {
        if (sCpuWakeLock != null) {
            sCpuWakeLock.release();
            sCpuWakeLock = null;
        }
    }
}
