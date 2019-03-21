package com.forwiz.nursetree.view;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.CallLog;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.TextViewCompat;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.forwiz.nursetree.R;
import com.forwiz.nursetree.statistics.StaticsUtility;
import com.forwiz.nursetree.statistics.UserInfo;
import com.forwiz.nursetree.util.NTAlertDialog;
import com.jakewharton.threetenabp.AndroidThreeTen;
import com.opentok.android.AudioDeviceManager;
import com.opentok.android.BaseAudioDevice;
import com.opentok.android.Connection;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;
import com.opentok.android.SubscriberKit;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.threeten.bp.Duration;
import org.threeten.bp.LocalDateTime;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by YunHo on 2017-03-21.
 */

@EActivity(R.layout.activity_webrtc)
public class TalkChatActivity extends BaseAppCompatActivity implements Session.SessionListener, PublisherKit.PublisherListener, Session.SignalListener, Session.ConnectionListener, Session.ReconnectionListener, Session.ArchiveListener, Session.StreamPropertiesListener, PublisherKit.AudioLevelListener, Subscriber.StreamListener, Subscriber.SubscriberListener {


    public static final int FORCE_CANCEL_LIMIT_MILLIS = 3000;
    @Extra
    String API_KEY = "";
    private String SESSION_ID = StaticsUtility.INBOUND;
    @Extra
    String TBOX_TOKEN = "";

    int expireSecond = 7;
    @Extra
    boolean needPaidCall = false;


    //현재 재시도 요청중 상태인지 체크 (session disconnect 콜벡메소드와 error 콜벡 메소드 둘다 탈때 한번만 재연결 할 수 있도록 하는 변수)
    boolean isReconnect = false;

    private static final String LOG_TAG = TalkChatActivity.class.getSimpleName();

    private Session mSession;

    private Publisher mPublisher;

    private Subscriber mSubscriber;

    //학생의 다음 선생님과의 통화를 위한 변수
    boolean hasNextCall = false;
    String nextRoomNum;

    List<Subscriber> subscriberList = new ArrayList<>();
    List<Publisher> publisherList = new ArrayList<>();

    private static final String FIELD_POWER_SCREEN_WAKE_LOCK = "PROXIMITY_SCREEN_OFF_WAKE_LOCK";

    @ViewById(R.id.voice_topic_state_layout)
    RelativeLayout voiceTopicLayout;

    @ViewById(R.id.ai_title_textview)
    TextView aiTitleTextView;

    @ViewById(R.id.voice_minor_topic_textview)
    TextView voiceMinorTopicText;

    @ViewById(R.id.next_word_layout)
    RelativeLayout nextWordLayout;

    @ViewById(R.id.toggle_video_btn)
    Button toggleVideoBtn;

    @ViewById(R.id.toggle_speaker_btn)
    CheckBox toggleSpeakerBtn;

    @ViewById(R.id.switch_camera_btn)
    Button switchCameraBtn;

    @ViewById(R.id.toggle_mute_btn)
    CheckBox toggleMuteBtn;

    @ViewById(R.id.toggle_mute_text)
    TextView toggleMuteText;

    @ViewById(R.id.toggle_speaker_text)
    TextView toggleSpeakerText;

    @ViewById(R.id.voice_call_my_state_text)
    TextView voiceCallMyStateText;

    @ViewById(R.id.video_call_my_state_text)
    TextView videoCallMyStateText;

    @ViewById(R.id.glview)
    FrameLayout glview;

    @ViewById(R.id.glview_my)
    FrameLayout glviewMyCamera;

    @ViewById(R.id.glview_layout)
    FrameLayout glviewLayout;

    @ViewById(R.id.voice_item_layout)
    LinearLayout voiceItemLayout;

    @ViewById(R.id.video_item_layout)
    LinearLayout videoItemLayout;

    @ViewById(R.id.toggle_voice_btn)
    Button requestVoiceBtn;

    @ViewById(R.id.voice_call_layout)
    RelativeLayout voiceCallLayout;

    @ViewById(R.id.video_call_layout)
    RelativeLayout videoCallLayout;

    @ViewById(R.id.disconnect_call_layout)
    RelativeLayout disconncetCallLayout;

    @ViewById(R.id.disconnect_call_state)
    TextView disconnectCallState;

    @ViewById(R.id.voice_call_time_state)
    TextView voiceCallTimeText;

    @ViewById(R.id.video_call_time_state)
    TextView videoCallTimeText;

    @ViewById(R.id.end_call_btn)
    Button callEndBtn;

    @ViewById(R.id.waiting_video_layout)
    LinearLayout waitingLayout;

    @ViewById(R.id.refresh_background_view)
    View refreshBackgroundView;

    @ViewById(R.id.refresh_layout)
    RelativeLayout refreshLayout;

    @ViewById(R.id.nursetree_ai_layout)
    LinearLayout nurseTreeAILayout;
    @ViewById(R.id.interest_or_analyzing_imageview)
    ImageView interestOrAnalyzingImageView;
    @ViewById(R.id.ai_subtitle_textview)
    TextView aiSubTitleTextView;

    @ViewById(R.id.topic_bottom_view)
    View topicBottomView;

    int durationTime = 0;

    Connection subscribeConnection = null;

    //start signal을 받았었는지 확인하는 플래그
    boolean receivedStartSignal = false;

    //재연결 시도를 위한 플래그 (true 일시 publisher 재 설정)
    boolean receivedConnectionDestroyed = false;

    //통화하는 상대방이 변경될때 low connection signal 메시지를 방지하기위한 플래그
    boolean lowConnectSignalWhileTransit = false;


    /**
     * 학생인지 여부 (true: 학생, false: 선생)
     **/
    @Extra
    boolean isHost;

    Context context;
    Activity activity;
    InputMethodManager imm;

    boolean alreadyEnd = false;

    MediaPlayer mediaPlayer;
    LocalDateTime startDateTime;

    boolean isTransit = false;

    CallStopCounterTask callStopTimeTask = null;

    LocalDateTime startCallTime;

    /**
     * 통화가 연결되기전엔 캔슬푸시를 받을수있도록 변수 지정
     * 연결된 후 캔슬 푸시를 받아도 통화 끊기지 않도록 상태를 false로 바꿔준다.
     **/
    boolean availCancelPushState = true;

    String myUserToken;

    /**
     * 화면 전환시 상대방과 연결이 끊기지 않도록 설정하기 위한 변수
     **/
    boolean isChangeOrientation = false;

    PowerManager.WakeLock wakeLock;

    OrientationEventListener mOrientationListener;

    /**
     * RTC Relay 서비스를 강제로 사용하는지 여부를 확인하는 플래그
     **/
    boolean isUserRequestExit = false;


    private static final float ROTATE_FROM = -10.0f * 360.0f;
    private static final float ROTATE_TO = 0.0f;

    RotateAnimation inAnim = new RotateAnimation(ROTATE_FROM, ROTATE_TO, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);

    @AfterViews
    void afterView() {
        context = this;
        activity = this;

        UserInfo.myPushState = true;
        finishMediaPlayer();
        AndroidThreeTen.init(this);

        if (startDateTime == null) {
            startDateTime = LocalDateTime.now();
            if (!StaticsUtility.IS_CARE_TEAM) {
                if (startDateTime.getMinute() == 59) {
                    startDateTime = startDateTime.withHour(startDateTime.getHour() + 1);
                    startDateTime = startDateTime.withMinute(0);
                    startDateTime = startDateTime.withSecond(0);
                } else if (startDateTime.getMinute() % 10 == 9) {
                    startDateTime = startDateTime.withMinute(((startDateTime.getMinute() / 10) * 10) + 10);
                    startDateTime = startDateTime.withSecond(0);
                } else {
                    startDateTime = startDateTime.withMinute((startDateTime.getMinute() / 10) * 10);
                    startDateTime = startDateTime.withSecond(0);
                }
            }
        }

//        ActivityManager.getInstance().addActivity(this);


        inAnim.setInterpolator(new LinearInterpolator());
        inAnim.setRepeatCount(Animation.INFINITE);
        inAnim.setDuration(8000);


        nextWordLayout.setClickable(false);
        toggleMuteBtn.setClickable(false);
        toggleSpeakerBtn.setClickable(false);
        toggleVideoBtn.setClickable(false);

        if (StaticsUtility.IS_CARE_TEAM) {
            voiceCallMyStateText.setText(R.string.common_nursetree_carecenter_text);
            videoCallMyStateText.setText(R.string.common_nursetree_carecenter_text);
        } else if (UserInfo.myUserType.equals("callcenter")) {
            voiceCallMyStateText.setText(R.string.common_nursetree_student_text);
            videoCallMyStateText.setText(R.string.common_nursetree_student_text);
        } else {
            voiceCallMyStateText.setText(R.string.common_nursetree_teacher_text);
            videoCallMyStateText.setText(R.string.common_nursetree_teacher_text);
        }

        this.isHost = (!(UserInfo.myUserType.equalsIgnoreCase("american") || UserInfo.myUserType.equals("callcenter")));
        this.myUserToken = StaticsUtility.CALLER_ID;

        videoCallLayout.setAlpha(0.5f);

        imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        // mBluetoothHelper = new BluetoothHelper(this);
        // mBluetoothHelper.setBlueToothListener(onBlueToothListener);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        // WakeLock 설정
        internalPrepareWakeLock();
        internalAcquireWakeLock();

        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(voiceMinorTopicText, 12, 32, 2, TypedValue.COMPLEX_UNIT_DIP);


        startTime();
        mSession = new Session.Builder(this, API_KEY, SESSION_ID).build();
        mSession.setSessionListener(this);
        mSession.setConnectionListener(this);
        mSession.setReconnectionListener(this);
        mSession.setArchiveListener(this);
        mSession.setStreamPropertiesListener(this);
        mSession.connect(TBOX_TOKEN);
        isReconnect = true;
        Log.e(LOG_TAG, "session connect afterView");

        Log.d("transit", "afterView(): API_KEY: " + API_KEY );
        Log.d("transit", "afterView(): Session ID: " + SESSION_ID );
        Log.d("transit", "afterView(): Token: " + TBOX_TOKEN);


        mOrientationListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) { /* Empty */ }
        };

        if (mOrientationListener.canDetectOrientation()) {
            mOrientationListener.enable();
        } else {
            mOrientationListener.disable();
        }

        mediaPlayer = com.forwiz.nursetree.util.MediaPlayer.getInstance(context, "waitmedia");
        mediaPlayer.setLooping(true);
        mediaPlayer.start();

        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        am.setMode(AudioManager.MODE_IN_COMMUNICATION);
        am.setSpeakerphoneOn(false);
        if (StaticsUtility.IS_CARE_TEAM) {
            nextWordLayout.setVisibility(View.INVISIBLE);
        }

        receivedStartSignal = true;
        if (mSession != null) {
            Handler forceCancelCallHandler = new Handler();
            forceCancelCallHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    for (Subscriber subscriber : subscriberList) {
                        if (subscriber != null) {
                            subscriber.setSubscribeToAudio(true);
                        }
                    }
                }
            }, 1500);

        }
        lowConnectSignalWhileTransit = false;
    }

    public float convertDpToPx(Context context, float dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }

    //region Override - onKeyDown for Block BackPress
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return keyCode != KeyEvent.KEYCODE_BACK && super.onKeyDown(keyCode, event);
    }
    //endregion

    //region Override - onConfigurationChanged for Block Rotation screen
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            isChangeOrientation = true;

        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            isChangeOrientation = true;

        }
    }
    //endregion

    @Override
    protected void onResume() {
        super.onResume();

        LocalBroadcastManager.getInstance(context).registerReceiver(mCancelReceiver,
                new IntentFilter("android.intent.action.TalkChat.cancel"));
    }

    private BroadcastReceiver mCancelReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context rcvContext, Intent intent) {
            //alert data here
            if (availCancelPushState) {
                commonCallEnd();
            }
            Bundle extras = intent.getExtras();
            if (extras != null) {
                String cancelTboxRoomNum = extras.getString("cancel_room_number");
                if (cancelTboxRoomNum != null && !cancelTboxRoomNum.isEmpty() && nextRoomNum != null && !nextRoomNum.isEmpty()) {
                    if (cancelTboxRoomNum.equals(nextRoomNum)) {   //여기넘어오는 token이 null이다. cancel 에서는 tokbox token 정보가없다
                        receivedStartSignal = false;
                    }
                }
            }

        }
    };

    @Override
    protected void onDestroy() {
        StaticsUtility.ALREADY_CALL = false;

        UserInfo.myPushState = false;

        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        am.setMode(AudioManager.MODE_NORMAL);
        am.setSpeakerphoneOn(false);

//        if (myReceiver != null) {
//            unregisterReceiver(myReceiver);
//        }
        if (callStopTimeTask != null) {
            callStopTimeTask.cancel(true);
            callStopTimeTask = null;
        }

        if (mSession != null) {
            if (mPublisher != null) {
                mSession.unpublish(mPublisher);
                mPublisher = null;
            }
            if (mSubscriber != null) {
                mSession.unsubscribe(mSubscriber);
                mSubscriber = null;
            }
            mSession.disconnect();
            mSession = null;
        }
//        스트림이 중단되면 Publisher의 뷰를 제거 한다.
        if (mSubscriber != null) {
            mSubscriber = null;
        }
        if (mPublisher != null) {
            mPublisher = null;
        }
        if (glview != null) {
            glview.removeAllViews();
        }
        if (glviewMyCamera != null) {
            glviewMyCamera.removeAllViews();
        }

        // Render View Release
        if (glview != null) {
            glview = null;
        }

        if (glviewMyCamera != null) {
            glviewMyCamera = null;
        }

        internalReleaseWakeLock();
        finishMediaPlayer();
        super.onDestroy();
    }

    @Click(R.id.end_call_btn)
    void onClickEndCallButton() {

        Log.d("Kevin", "Hanginup now");
        if (!this.isFinishing()) {
            if (StaticsUtility.IS_CARE_TEAM) {
                requestEnd(false); return;
//                NTAlertDialog.getComfirmCancelDialog(
//                        context,
//                        R.string.really_end_call_care_center_title,
//                        R.string.really_end_call_care_center_msg,
//                        (DialogInterface dialog, int which) ->
//                                requestEnd(false)
//                        ,
//                        (DialogInterface dialog, int which) ->
//                                dialog.dismiss()
//                ).setCancelable(false).show();
            } else if (isHost) {

                requestEnd(true); return;
//                SpannableString link = makeLinkSpan("기술문제신고", (View v) -> {
//                    Log.d("checkbox", "click text");
//                    gotoFeedbackActivity(true, 0);
//                });
//
//                View gotoFeedBackView = View.inflate(context, R.layout.checkbox, null);
//                TextView textView = (TextView) gotoFeedBackView.findViewById(R.id.feedback_link_textview);
//                textView.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View view) {
//
//                    }
//                });
//                textView.setText(link);
//                makeLinksFocusable(textView);
//                duringCallEndDialog = NTAlertDialog.getComfirmCancelDialog(
//                        context,
//                        R.string.really_end_call_title,
//                        R.string.really_end_call_msg_student,
//                        (DialogInterface dialog, int which) ->
//                                requestEnd(true)
//                        ,
//                        (DialogInterface dialog, int which) ->
//                                dialog.dismiss()
//                ).setView(gotoFeedBackView).setCancelable(false).show();
            } else {
                requestEnd(false); return;
//                NTAlertDialog.getComfirmCancelDialog(
//                        context,
//                        R.string.really_end_call_title,
//                        R.string.really_end_call_msg_teacher,
//                        (DialogInterface dialog, int which) ->
//                                requestEnd(false)
//                        ,
//                        (DialogInterface dialog, int which) ->
//                                dialog.dismiss()
//                ).setCancelable(false).show();
            }
        }
    }

    private SpannableString makeLinkSpan(CharSequence text, View.OnClickListener listener) {
        SpannableString link = new SpannableString(text);

        link.setSpan(new ClickableString(listener), 0, text.length(),
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        link.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.wt_blue_color1)), 0, text.length(),
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        link.setSpan(new UnderlineSpan(), 0, text.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        return link;
    }

    private static class ClickableString extends ClickableSpan {
        private View.OnClickListener mListener;

        private ClickableString(View.OnClickListener listener) {
            mListener = listener;
        }

        @Override
        public void onClick(View v) {
            mListener.onClick(v);
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            ds.setUnderlineText(false);
        }
    }

    private void requestEnd(boolean isDuringCall) {
        UserInfo.myPushState = false;
        availCancelPushState = true;
        if (isDuringCall) {
            if (hasNextCall) {
                receivedStartSignal = false;
                finishMediaPlayer();
                mediaPlayer = com.forwiz.nursetree.util.MediaPlayer.getInstance(context, "waitmedia");
                mediaPlayer.setLooping(true);
                mediaPlayer.start();
            } else {
                commonCallEnd(true);
            }
        } else {
            if (hasNextCall) {
                receivedStartSignal = false;
                finishMediaPlayer();
                mediaPlayer = com.forwiz.nursetree.util.MediaPlayer.getInstance(context, "waitmedia");
                mediaPlayer.setLooping(true);
                mediaPlayer.start();
            } else {
                commonCallEnd();
            }
        }
    }


    @UiThread(propagation = UiThread.Propagation.REUSE)
    void showNoneSessionDialog() {
        if (!activity.isFinishing())
            NTAlertDialog.getComfirmDialog(context, R.string.signin_text, R.string.nt_login_none_session_msg, (DialogInterface dialog, int which) -> {
                dialog.dismiss();
                finish();
//                ChooseSignActivity_.intent(context).flags(Intent.FLAG_ACTIVITY_NEW_TASK).start();
            }).setCancelable(false).show();
    }


    @Override
    public void onConnected(Session session) {

        Log.i(LOG_TAG, "Session Connected");

        for (Subscriber subscriber : subscriberList) {
            if (subscriber != null) {
                subscriber.setSubscribeToAudio(true);
            }
        }

        if (mPublisher == null) {
            mPublisher = new Publisher.Builder(this).audioBitrate(28000).videoTrack(false).build();
            mPublisher.setPublisherListener(this);
            mPublisher.setPublishVideo(false);
            mPublisher.setAudioLevelListener(this);
            publisherList.add(mPublisher);
        }
        AudioDeviceManager.getAudioDevice().setOutputMode(BaseAudioDevice.OutputMode.Handset);

        if (mSession != null && mPublisher != null) {
            mSession.publish(mPublisher);
            mSession.setSignalListener(this);
        }
        if (startCallTime == null) {
            startCallTime = LocalDateTime.now();
        }
        UserInfo.myPushState = true;

        int TALK_TIME_IN_MINUTES = 1000 * 60 * 9;

        Log.d("Kevin", "Will hang up after 10 seconds");

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                onClickEndCallButton();
            }
        }, TALK_TIME_IN_MINUTES);

    }



    @Override
    public void onDisconnected(Session session) {
        Log.i(LOG_TAG, "Session Disconnected");
        commonCallEnd();
    }

    @Override
    public void onStreamReceived(Session session, Stream stream) {
        Log.i(LOG_TAG, "Stream Received");
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        am.setMode(AudioManager.MODE_IN_COMMUNICATION);
        am.setSpeakerphoneOn(false);

        //builder는 앱 컨텍스트, 보고자하는 스트림 이 두가지를 매개 변수로 사용한다.
        mSubscriber = new Subscriber.Builder(this, stream).build();
        if (mSession != null && mSubscriber != null) {
            mSubscriber.setSubscriberListener(this);
            mSubscriber.setSubscribeToVideo(false);
            toggleSpeakerBtn.setChecked(false);

            mSubscriber.setStreamListener(this);
            if (!receivedStartSignal) {
                mSubscriber.setSubscribeToAudio(false);
            }
            subscriberList.add(mSubscriber);
            mSession.subscribe(mSubscriber);
        }

        if (!receivedStartSignal && mSubscriber != null) {
            toggleSpeakerBtn.setChecked(false);

            mSubscriber.setSubscribeToAudio(false);
        } else if (receivedStartSignal) {
            finishMediaPlayer();
            for (Subscriber subscriber : subscriberList) {
                if (subscriber != null) {
                    subscriber.setSubscribeToAudio(true);
                }
            }

            if (receivedConnectionDestroyed) {
                if (mPublisher == null) {
                    mPublisher = new Publisher.Builder(this).audioBitrate(28000).videoTrack(false).build();
                    mPublisher.setPublisherListener(this);
                    mPublisher.setPublishVideo(false);
                    mPublisher.setAudioLevelListener(this);
                    publisherList.add(mPublisher);
                }

                AudioDeviceManager.getAudioDevice().setOutputMode(BaseAudioDevice.OutputMode.Handset);
                for (Publisher publisher : publisherList) {
                    if (publisher != null) {
                        mSession.publish(mPublisher);
                        mSession.setSignalListener(this);
                    }
                }
                UserInfo.myPushState = true;
                receivedConnectionDestroyed = false;
            }
        }
        isReconnect = false;
        availCancelPushState = false;
        disconnectCallState.setText(R.string.connecting_signal_low_text);
        alreadyEnd = false;

    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {
        Log.i(LOG_TAG, "Stream Dropped");
    }

    @Override
    public void onError(Session session, OpentokError opentokError) {
        Log.i(LOG_TAG, "Stream Error");
        int errCode = opentokError.getErrorCode().getErrorCode();
        Log.e(LOG_TAG, "Error Code :" + errCode);
    }

    @Override
    public void onSignalReceived(Session session, String type, String data, Connection
            connection) {

    }

    @Override
    public void onStreamCreated(PublisherKit publisherKit, Stream stream) {
        Log.i(LOG_TAG, "Publisher onStreamCreated");

        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        am.setMode(AudioManager.MODE_IN_COMMUNICATION);
        am.setSpeakerphoneOn(false);
    }

    @Override
    public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {
        Log.i(LOG_TAG, "Publisher onStreamDestroyed");
        receivedConnectionDestroyed = true;
    }

    @Override
    public void onError(PublisherKit publisherKit, OpentokError opentokError) {
        Log.i(LOG_TAG, "Publisher onError");
    }

    @Override
    public void onConnectionCreated(Session session, Connection connection) {
        Log.i(LOG_TAG, "onConnectionCreated");
        subscribeConnection = connection;
    }

    @Override
    public void onConnectionDestroyed(Session session, Connection connection) {
        Log.i(LOG_TAG, "onConnectionDestroyed");
        StaticsUtility.ALREADY_CALL = false;
        subscribeConnection = connection;
        receivedConnectionDestroyed = true;

        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        am.setMode(AudioManager.MODE_NORMAL);
        am.setSpeakerphoneOn(false);

        int permissionCheck = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_CALL_LOG);
        if (permissionCheck != PackageManager.PERMISSION_DENIED) {
            changeCallLogToReceive(getBaseContext().getContentResolver());
        }
    }

    @Override
    public void onReconnecting(Session session) {
        Log.i(LOG_TAG, "Session onReconnecting");

        ringAlertMedia();
    }

    @Override
    public void onReconnected(Session session) {
        Log.i(LOG_TAG, "onReconnected");
        finishMediaPlayer();
    }

    @Override
    public void onAudioLevelUpdated(PublisherKit publisherKit, float v) {
//        Log.i(LOG_TAG, "onAudioLevelUpdated level : "+ v);
    }

    @Override
    public void onArchiveStarted(Session session, String s, String s1) {
        Log.i(LOG_TAG, "onArchiveStarted");
    }

    @Override
    public void onArchiveStopped(Session session, String s) {
        Log.i(LOG_TAG, "onArchiveStopped");
    }

    @Override
    public void onStreamHasAudioChanged(Session session, Stream stream, boolean b) {
        Log.i(LOG_TAG, "onStreamHasAudioChanged");
    }

    @Override
    public void onStreamHasVideoChanged(Session session, Stream stream, boolean b) {
        Log.i(LOG_TAG, "onStreamHasVideoChanged");
    }

    @Override
    public void onStreamVideoDimensionsChanged(Session session, Stream stream, int i, int i1) {
        Log.i(LOG_TAG, "onStreamVideoDimensionsChanged");
    }

    @Override
    public void onStreamVideoTypeChanged(Session session, Stream stream, Stream.StreamVideoType
            streamVideoType) {
        Log.i(LOG_TAG, "onStreamVideoTypeChanged");
    }


    @Override
    public void onReconnected(SubscriberKit subscriberKit) {
        Log.i(LOG_TAG, "SubscriberKit onReconnected");
    }

    @Override
    public void onConnected(SubscriberKit subscriberKit) {
        Log.i(LOG_TAG, "SubscriberKit onConnected");

    }

    @Override
    public void onDisconnected(SubscriberKit subscriberKit) {
        Log.i(LOG_TAG, "SubscriberKit onDisconnected");
    }

    @Override
    public void onError(SubscriberKit subscriberKit, OpentokError opentokError) {
        Log.i(LOG_TAG, "SubscriberKit onError");
    }

    public static final class CallStopCounterTask extends AsyncTask<Integer, Integer, Integer> {
        int currentTime = 0;
        LocalDateTime currentDateTime = LocalDateTime.now();
        WeakReference<TalkChatActivity> activityReference;

        private CallStopCounterTask(TalkChatActivity context) {
            this.activityReference = new WeakReference<>(context);
        }

        protected void onPreExecute() { /* Empty */
            currentTime = 0;
        }

        @Override
        protected Integer doInBackground(Integer... params) {
            Log.d("enterSuccess", "doinbackground enter.");

            TalkChatActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) {
                return null;
            }

            try {
                while (!this.isCancelled()) {

                    if (this.isCancelled()) {
                        return null;
                    }

                    Thread.sleep(1000);
                    activity.expireSecond--;
                    currentTime++;

                    publishProgress(currentTime);
                }

            } catch (InterruptedException e) {
                Log.e("maguro", "[CallStopCounterTask::doInBackground] Interrupted: " + this.isCancelled());
            }

            return null;
        }

        protected void onProgressUpdate(Integer... result) {
            Log.d("enterSuccess", "progressupdate enter");

            TalkChatActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) {
                return;
            }

            currentDateTime = LocalDateTime.now();
            Duration duration = null;

            int callSec = 0;
            if (activity.startDateTime != null && currentDateTime != null) {
                duration = Duration.between(activity.startDateTime, currentDateTime);
            }
            if (duration != null) {
                callSec = (int) duration.getSeconds();
            }
            activity.durationTime = callSec;

            if (callSec <= 0) {
                callSec = 0;
            }
            int hours = callSec / 3600;
            int minutes = (callSec / 60) % 60;
            int secs = callSec % 60;

            String currentTimeString = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs);
            activity.updateTime(currentTimeString);


            Log.e("maguro", "[CallStopCounterTask::onProgressUpdate] currentTime: " + currentDateTime.toString() + "    currentTimeString:" + currentTimeString);

        }
    }

    @UiThread
    void updateTime(String currentTimeString) {
        Log.d("enterSuccess", "update Time enter.");
        if (voiceCallTimeText != null) {
            voiceCallTimeText.setText(currentTimeString);
        }
        if (videoCallTimeText != null) {
            videoCallTimeText.setText(currentTimeString);
        }
    }

    void startTime() {
        Log.d("enterSuccess", "starttime enter.");
        callStopTimeTask = new CallStopCounterTask(this);
        callStopTimeTask.execute();
    }

    @UiThread
    void ringAlertMedia() {
        finishMediaPlayer();
        mediaPlayer = com.forwiz.nursetree.util.MediaPlayer.getInstance(context, "alertshort");
        mediaPlayer.setLooping(true);
        mediaPlayer.start();
    }

    /**
     * 현재 통화를 완전히 종료합니다.
     */
    @UiThread
    void cleanUpRtcDuringCall() {
        UserInfo.myPushState = false;
        if (callStopTimeTask != null) {
            callStopTimeTask.cancel(true);
            callStopTimeTask = null;
        }

        if (mSession != null) {
            if (mPublisher != null) {
                mSession.unpublish(mPublisher);
            }
            if (mSubscriber != null) {
                mSession.unsubscribe(mSubscriber);
            }
            mSession.disconnect();
            mSession = null;
        }
//        스트림이 중단되면 Publisher의 뷰를 제거 한다.
        if (mSubscriber != null) {
            mSubscriber = null;
        }
        if (mPublisher != null) {
            mPublisher = null;
        }

        internalReleaseWakeLock();
        finish();
    }

    @UiThread
    void procCleanupRtcSession() {
        UserInfo.myPushState = false;
        if (callStopTimeTask != null) {
            callStopTimeTask.cancel(true);
            callStopTimeTask = null;
        }

        if (mSession != null) {
            if (mPublisher != null) {
                mSession.unpublish(mPublisher);
            }
            if (mSubscriber != null) {
                mSession.unsubscribe(mSubscriber);
            }
            mSession.disconnect();
            mSession = null;
        }
//        스트림이 중단되면 Publisher의 뷰를 제거 한다.
        if (mSubscriber != null) {
            mSubscriber = null;
        }
        if (mPublisher != null) {
            mPublisher = null;
        }
        internalReleaseWakeLock();
        finish();
    }

    //endregion



    //endregion


    //region Internal - 미디어 플레이어 종료 메소드

    /**
     * 미디어 플레이어를 멈춤
     **/
    @UiThread(propagation = UiThread.Propagation.REUSE)
    void finishMediaPlayer() {
        com.forwiz.nursetree.util.MediaPlayer.finishMediaPlayer();
    }

    /**
     * 1초 뒤에 미디어 플레이어를 멈춤
     **/
    @UiThread(propagation = UiThread.Propagation.REUSE)
    void finishMediaPlayAfter1Secs() {
        isTransit = false;
        final CountDownTimer[] timer = {null};
        timer[0] = new CountDownTimer(1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                timer[0].cancel();
                timer[0] = null;
                finishMediaPlayer();
            }
        };
        timer[0].start();
    }
    //endregion


    //region Internal - Power WakeLock 관련 메소드

    /**
     * Power WakeLock 객체 세팅
     **/
    private void internalPrepareWakeLock() {
        int field = 0;
        try {
            field = PowerManager.class.getField(FIELD_POWER_SCREEN_WAKE_LOCK).getInt(null);
        } catch (Throwable e) { /* Ignored */ }

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(field, getLocalClassName());
        }
    }

    /**
     * Power WakeLock 설정
     **/
    private void internalAcquireWakeLock() {
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);
        }
    }

    /**
     * Power WakeLock 해제
     **/
    private void internalReleaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }


    @UiThread
    void commonCallEnd(boolean isDuringCall) {
        isUserRequestExit = true;
        if (!alreadyEnd) {
            alreadyEnd = true;
            UserInfo.myPushState = false;
            finishMediaPlayer();
            mediaPlayer = com.forwiz.nursetree.util.MediaPlayer.getInstance(context, "endcall");
            mediaPlayer.start();

            if (isDuringCall) {
                cleanUpRtcDuringCall();
            } else {
                procCleanupRtcSession();
            }
        }
    }

    @UiThread
    void commonCallEnd() {
        isUserRequestExit = true;
        if (!alreadyEnd) {
            alreadyEnd = true;
            UserInfo.myPushState = false;
            finishMediaPlayer();
            mediaPlayer = com.forwiz.nursetree.util.MediaPlayer.getInstance(context, "endcall");
            mediaPlayer.start();

            procCleanupRtcSession();
        }
    }

    //통화 로그를 남기기위한 메소드 (수신)
    void changeCallLogToReceive(ContentResolver resolver) {
        Uri UriCalls = Uri.parse("content://call_log/calls");
        final ContentResolver deleteResolver = getContentResolver();
        deleteResolver.delete(UriCalls, "_id IN " + "(SELECT _id FROM calls ORDER BY date DESC LIMIT 1)", null);

        ContentValues values = new ContentValues();
        values.put(CallLog.Calls.NUMBER, "NurseTree");
        values.put(CallLog.Calls.TYPE, 1); // 1:수신, 2:발신, 3:부재중전화
        values.put(CallLog.Calls.DATE, System.currentTimeMillis()); // 시간
        values.put(CallLog.Calls.DURATION, durationTime); // 통화시간
        values.put(CallLog.Calls.NEW, 1);
        resolver.insert(Uri.parse("content://call_log/calls"), values);
    }

}