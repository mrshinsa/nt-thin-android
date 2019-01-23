package com.forwiz.nursetree.util;

import android.content.Context;

import com.forwiz.nursetree.R;

/**
 * Created by YunHo on 2017-06-28.
 */

public class MediaPlayer {

    private static android.media.MediaPlayer mMediaPlayer = null;


    public static android.media.MediaPlayer getInstance(Context context, String mediaSoundType) {
        switch (mediaSoundType) {
            case "endalert":
                if (mMediaPlayer == null) {
                    mMediaPlayer = android.media.MediaPlayer.create(context, R.raw.endalert);
                }
                break;

            case "endcall":
                if (mMediaPlayer == null) {
                    mMediaPlayer = android.media.MediaPlayer.create(context, R.raw.endcall);
                }
                break;

            case "alertshort":
                if (mMediaPlayer == null) {
                    mMediaPlayer = android.media.MediaPlayer.create(context, R.raw.alertshort);
                }
                break;

            default:
                if (MediaPlayer.mMediaPlayer == null) {
                    mMediaPlayer = android.media.MediaPlayer.create(context, R.raw.callsound);
                }
                break;


        }
        return mMediaPlayer;
    }

    public static void finishMediaPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }


}

