package com.forwiz.nursetree;

import android.app.Application;
import android.content.Context;
import android.media.AudioManager;

public class ApplicationClass extends Application {

    private static Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = this;




    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        AudioManager am = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        am.setMode(AudioManager.MODE_NORMAL);
        am.setSpeakerphoneOn(false);
    }

    public static Context getAppContext() {
        return appContext;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
//        MultiDex.install(this);
    }
}
