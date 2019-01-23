package com.forwiz.nursetree.view;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.CallLog;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AlertDialog;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.method.ScrollingMovementMethod;
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
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.forwiz.nursetree.R;
import com.forwiz.nursetree.common.ServerApiManager;
import com.forwiz.nursetree.model.ObjectLevelWord;
import com.forwiz.nursetree.model.ObjectWordInterest;
import com.forwiz.nursetree.model.TopicCategoryWordObject;
import com.forwiz.nursetree.model.WordCategory;
import com.forwiz.nursetree.statistics.StaticsUtility;
import com.forwiz.nursetree.statistics.UserInfo;
import com.forwiz.nursetree.util.ActivityManager;
import com.forwiz.nursetree.util.CustomProgressDialog;
import com.forwiz.nursetree.util.NTAlertDialog;
import com.forwiz.nursetree.util.RefreshTimeActivityManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.CheckedChange;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.threeten.bp.Duration;
import org.threeten.bp.LocalDateTime;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by YunHo on 2017-03-21.
 */

@EActivity(R.layout.activity_webrtc)
public class TalkChatActivity extends BaseAppCompatActivity implements Session.SessionListener, PublisherKit.PublisherListener, Session.SignalListener, Session.ConnectionListener, Session.ReconnectionListener, Session.ArchiveListener, Session.StreamPropertiesListener, PublisherKit.AudioLevelListener, Subscriber.AudioStatsListener, Subscriber.StreamListener, Subscriber.SubscriberListener {


    public static final int FORCE_CANCEL_LIMIT_MILLIS = 3000;
    @Extra
    String API_KEY = "";
    private String SESSION_ID = StaticsUtility.INBOUND;
    String previousRoomNum = StaticsUtility.INBOUND;
    @Extra
    String TBOX_TOKEN = "";

    int expireSecond = 7;
    @Extra
    Date expEndDate = null;
    @Extra
    boolean needPaidCall = false;


    //현재 재시도 요청중 상태인지 체크 (session disconnect 콜벡메소드와 error 콜벡 메소드 둘다 탈때 한번만 재연결 할 수 있도록 하는 변수)
    boolean isReconnect = false;

    private static final String LOG_TAG = TalkChatActivity.class.getSimpleName();

    public final int CONNECTION_FAILED = 1006;
    public final int NOT_CONNECTED = 1010;
    public final int SESSION_CONNECTION_TIMEOUT = 1021;
    public final int CONNECTION_DROPPED_ERROR = 1022;
    public final int CONNECTION_REFUSED = 1023;
    public final int SESSION_PUBLISHER_NOT_FOUND = 1113;
    public final int UNKNOWN_PUBLISHER_INSTANCE = 2003;
    public final int UNKNOWN_SUBSCRIBER_INSTANCE = 2004;

    private final int FEEDBACK_REQUEST_CODE = 5555;
    private final int FEEDBACK_REQUEST_CODE_DURING_CALL = 6666;

    private List<Session> mSessionList = new ArrayList<>();
    private Session mSession;

    private Publisher mPublisher;

    private Subscriber mSubscriber;

    //학생의 다음 선생님과의 통화를 위한 변수
    boolean hasNextCall = false;
    String sessionSeqToNextCall;
    String nextRoomNum;

    List<Subscriber> subscriberList = new ArrayList<>();
    List<Publisher> publisherList = new ArrayList<>();

    private static final String LOGTAG = "quality-stats-demo";

    private static final int TIME_WINDOW = 3; //3 seconds

    int previousAudioPacket = 0;
    int nowAudioPacket = 0;

    private double mAudioPLRatio = 0.0;
    public long mAudioBw = 0;

    private long mPrevAudioPacketsLost = 0;
    private long mPrevAudioPacketsRcvd = 0;
    private double mPrevAudioTimestamp = 0;
    private long mPrevAudioBytes = 0;

    NTAlertDialog videoChatDialog;

    private static final String FIELD_POWER_SCREEN_WAKE_LOCK = "PROXIMITY_SCREEN_OFF_WAKE_LOCK";

    public static final String KEY_TBOX_TOKEN = "tbox_token";
    public static final String KEY_TBOX_APIKEY = "tbox_apikey";
    public static final String KEY_ROOM_NUMBER = "room_number";
    public static final String KEY_CALL_SERVER = "call_server";
    public static final String KEY_CALL_HOST = "call_host";
    public static final String KEY_EXPECT_END_DATE = "exp_end_date";
    public static final String KEY_TOPIC_SEQ = "topic_seq";
    public static final String KEY_TOPIC_TITLE = "topic_title";
    public static final String KEY_TCG_SEQ = "topic_ct_seq";
    public static final String KEY_TCG_TITLE = "topic_ct_title";
    public static final String KEY_TCG_ICON = "topic_ct_iconid";

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

    @ViewById(R.id.call_connection_loading_ani)
    ImageView disconncetCallAnimation;

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

    @ViewById(R.id.want_learn_layout)
    RelativeLayout wantLearnLayout;

    @ViewById(R.id.not_interest_layout)
    RelativeLayout notInterestLayout;

    @ViewById(R.id.level_test_know_layout)
    RelativeLayout levelTestKnowLayout;
    @ViewById(R.id.level_test_dont_know_layout)
    RelativeLayout levelTestDontKnowLayout;

    @ViewById(R.id.topic_bottom_view)
    View topicBottomView;

//    @ViewById(R.id.feedback_link_textview)
//    TextView feedbackLinkTextView;

    @ViewById(R.id.student_feedback_message)
    TextView studentFeedbackMessage;

    @Extra
    String passedTopic;

    @Extra
    String passedWord;

    @Extra
    String topicIconSeq;

    @Extra
    String categoryIconId;

    boolean isHeadSetOn = false;
    boolean isBluetoothOn = false;

    int durationTime = 0;

    Connection subscribeConnection = null;

    //start signal을 받았었는지 확인하는 플래그
    boolean receivedStartSignal = false;

    //재연결 시도를 위한 플래그 (true 일시 publisher 재 설정)
    boolean receivedConnectionDestroyed = false;

    //통화하는 상대방이 변경될때 low connection signal 메시지를 방지하기위한 플래그
    boolean lowConnectSignalWhileTransit = false;

    boolean previousAudioOutSpeaker = false;

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

    IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);

    WordCategory wordCategory;
    String selectedSeq = "";
    String selectedTitle = "";
    String selectedWordTitle = "";
    String currentWord = "";

    JSONArray wordArray;

//    RatingDialog ratingDialog2;
    CustomProgressDialog progressDialog;
    AlertDialog duringCallEndDialog;

    int feedbackRating = 0;
    boolean alreadyRating = false;

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

    //region Deprecated - Topic properties

    int[] callIconList = {
            R.drawable.icon_voicecall_0_introdution, R.drawable.icon_voicecall_4_music_art, R.drawable.icon_voicecall_5_movies,
            R.drawable.icon_voicecall_9_work, R.drawable.icon_voicecall_11_business_english, R.drawable.icon_voicecall_8_dail_conversation,
            R.drawable.icon_voicecall_10_school, R.drawable.icon_voicecall_3_fashion, R.drawable.icon_voicecall_6_shopping,
            R.drawable.icon_voicecall_1_sports, R.drawable.icon_voicecall_2_travel, R.drawable.icon_voicecall_15_economy,
            R.drawable.icon_voicecall_14_society, R.drawable.icon_voicecall_7_food, R.drawable.icon_voicecall_13_health,
            R.drawable.icon_voicecall_16_nature, R.drawable.icon_voicecall_12_technology
    };

    boolean alreadyEndInterest = false;
    boolean alreadyLevelTest = false;
    int interestWordIndex = 0;
    int levelTestWordIndex = 0;
    List<Integer> choiceSeqList = new ArrayList<>();
    List<ObjectWordInterest> wordInterestList = new ArrayList<>();
    List<String> wordInterestStringList = new ArrayList<>();

    List<Integer> interestIntegerList = new ArrayList<>();


    List<ObjectLevelWord> levelTestWordList = new ArrayList<>();

    //endregion

    private static final float ROTATE_FROM = -10.0f * 360.0f;
    private static final float ROTATE_TO = 0.0f;

    RotateAnimation inAnim = new RotateAnimation(ROTATE_FROM, ROTATE_TO, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);

    @AfterViews
    void afterView() {
        context = this;
        activity = this;

        UserInfo.myPushState = true;
        finishMediaPlayer();

        if (startDateTime == null) {
            startDateTime = LocalDateTime.now();
            if (!StaticsUtility.IS_CARE_TEAM) {
                if (startDateTime.getMinute() == 59) {
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

        ActivityManager.getInstance().addActivity(this);
        RefreshTimeActivityManager.getInstance().addActivity(this);

        inAnim.setInterpolator(new LinearInterpolator());
        inAnim.setRepeatCount(Animation.INFINITE);
        inAnim.setDuration(8000);

        startAnimationImage();

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
        currentWord = passedWord;

        studentFeedbackMessage.setMovementMethod(ScrollingMovementMethod.getInstance());
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

        invisibleFeedback();

        //선생 버튼 숨김 & 피드백 나타남
        if (!isHost) {
            nextWordLayout.setVisibility(View.INVISIBLE);
            wantLearnLayout.setVisibility(View.GONE);
            notInterestLayout.setVisibility(View.GONE);

            getStudentsFeedback(SESSION_ID);
        } else {
            //학생 피드백 안보이도록
            invisibleFeedback();
        }
        voiceMinorTopicText.setVisibility(View.VISIBLE);
        if (passedWord.equals("")) {
            passedWord = "Free Talk";
            voiceMinorTopicText.setText(passedWord);
        }
        if (!StaticsUtility.NEED_INTEREST_TEST) {
            aiTitleTextView.setVisibility(View.INVISIBLE);
            voiceMinorTopicText.setVisibility(View.VISIBLE);
            voiceMinorTopicText.setText(passedWord);
            if (categoryIconId != null && categoryIconId.isEmpty()) {
                categoryIconId = "0";
            }
            interestOrAnalyzingImageView.setImageResource(callIconList[Integer.parseInt(categoryIconId)]);
            aiSubTitleTextView.setText(passedTopic);
        }

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

//        registerReceiver(myReceiver, filter);
        // mBluetoothHelper.start();

        internalVideoScreenDisable();

        //푸시에서 받은 interest가 필요한 상황일때 interest 목록을 가져온다.
        if (StaticsUtility.NEED_INTEREST_TEST && isHost) {
            requestInterestList();
        } else if (isHost) {
            requestLevelTestList();
        }
        if (StaticsUtility.NEED_INTEREST_TEST && !isHost) {
            receivedInterestType();
            voiceMinorTopicText.setText("AI Learning");
        }

        interestAfterStartSignal(false, 0.5f);

        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        am.setMode(AudioManager.MODE_IN_COMMUNICATION);
        am.setSpeakerphoneOn(false);
        if (StaticsUtility.IS_CARE_TEAM) {
            nextWordLayout.setVisibility(View.INVISIBLE);
        }
    }

    public float convertDpToPx(Context context, float dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }

    @Background
    void getStudentsFeedback(String sessionSeq) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("session_seq", sessionSeq);
        Request request = ServerApiManager.getRequestGet(ServerApiManager.ServerApi.CARE_FEEDBACK, params);

        OkHttpClient.Builder b = new OkHttpClient.Builder();
        b.readTimeout(100, TimeUnit.SECONDS);
        b.writeTimeout(30, TimeUnit.SECONDS);
        OkHttpClient client = b.build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                JSONObject jsonObject = null;
                try {
                    jsonObject = new JSONObject(response.body().string());
                    String result = jsonObject.getString("status");
                    switch (result) {
                        case "success":
                            String feedbackString = jsonObject.getString("feedback");
                            Log.d("feedback", "result success");
                            if (feedbackString != null && !feedbackString.isEmpty()) {
                                updateStudentFeedback(feedbackString);
                            } else {
                                invisibleFeedback();
                            }
                            break;
                        case "none_permission":
                            Log.d("feedback", "result nonePermission");
                            break;
                        case "none_user":
                            Log.d("feedback", "result noneUser");
                            showNoneUserDialog();
                            break;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        });
    }

    @Background
    void requestInterestList() {
        wordInterestList.clear();
        Request request = ServerApiManager.getRequestGet(ServerApiManager.ServerApi.WORD_INTEREST);
        OkHttpClient.Builder b = new OkHttpClient.Builder();
        b.readTimeout(100, TimeUnit.SECONDS);
        b.writeTimeout(30, TimeUnit.SECONDS);
        OkHttpClient client = b.build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    String result = jsonObject.getString("status");

                    if (result.equals("success")) {
                        JSONArray wordJsonArray = jsonObject.getJSONArray("interests");
                        for (int i = 0; i < wordJsonArray.length(); i++) {
                            ObjectWordInterest objectWordInterest = new ObjectWordInterest(wordJsonArray.getJSONObject(i));
                            wordInterestList.add(objectWordInterest);
                        }
                        requestInterestDone();

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Background
    void requestLevelTestList() {
        interestIntegerList.clear();
        interestIntegerList = getIntegerArrayPref(context);
        setIntegerArrayPref(context);

        SharedPreferences prefs = getSharedPreferences("interest", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("interestWordIndex", 0);
        editor.apply();

        Request request = ServerApiManager.getRequestGet(ServerApiManager.ServerApi.WORD_LEVEL_JUDGEMENT);
        OkHttpClient.Builder b = new OkHttpClient.Builder();
        b.readTimeout(100, TimeUnit.SECONDS);
        b.writeTimeout(30, TimeUnit.SECONDS);
        OkHttpClient client = b.build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    String result = jsonObject.getString("status");

                    switch (result) {
                        case "success":
                            JSONObject wordJsonObject = jsonObject.getJSONObject("word");
                            ObjectLevelWord objectLevelWord = new ObjectLevelWord(wordJsonObject);
                            levelTestWordList.add(objectLevelWord);
                            requestLevelWordDone(objectLevelWord);
                            break;
                        case "already_judge":
                            finishSendLevelWord(null);
                            Log.d("levelJudgement", "result alreadyJudge");
                            break;
                        case "finish":
                            finishSendLevelWord(null);
                            Log.d("levelJudgement", "result finish");
                            break;
                        case "none_word":
                            Log.d("levelJudgement", "result none Word");
                            break;
                        case "none_user":
                            Log.d("levelJudgement", "result none User");
                            break;
                        default:
                            Log.d("levelJudgement", "result default : " + result);
                            break;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
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

        currentWord = passedWord;

        LocalBroadcastManager.getInstance(context).registerReceiver(mCancelReceiver,
                new IntentFilter("android.intent.action.TalkChat.cancel"));
    }

    void startAnimationImage() {
        disconncetCallAnimation.setAnimation(inAnim);
        disconncetCallAnimation.startAnimation(inAnim);

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
        // mBluetoothHelper.stop();
        // mBluetoothHelper = null;

        super.onDestroy();
    }

    @Click(R.id.want_learn_layout)
    void wantInterestTopic() {
        if (wordInterestList.size() > interestWordIndex) {
            interestIntegerList.set(interestWordIndex, wordInterestList.get(interestWordIndex).getInterestSeq());
            interestIntegerList.set(interestWordIndex + 1, wordInterestList.get(interestWordIndex + 1).getInterestSeq());
            interestWordIndex = interestWordIndex + 2;

            setIntegerArrayPref(context);
            SharedPreferences prefs = getSharedPreferences("interest", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("interestWordIndex", interestWordIndex);
            editor.apply();
            if (wordInterestList.size() > interestWordIndex) {
                voiceMinorTopicText.setText(wordInterestList.get(interestWordIndex).getTitle() + "\n" + wordInterestList.get(interestWordIndex + 1).getTitle());
                sendSignal("word", wordInterestList.get(interestWordIndex).getTitle() + "\n" + wordInterestList.get(interestWordIndex + 1).getTitle());
            } else {
                noHaveMoreInterestList();
            }
        } else {
            noHaveMoreInterestList();
        }
    }

    @Click(R.id.not_interest_layout)
    void notInterestTopic() {
        if (wordInterestList.size() > interestWordIndex) {
            interestWordIndex = interestWordIndex + 2;

            setIntegerArrayPref(context);
            SharedPreferences prefs = getSharedPreferences("interest", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("interestWordIndex", interestWordIndex);
            editor.apply();

            if (wordInterestList.size() > interestWordIndex) {
                voiceMinorTopicText.setText(wordInterestList.get(interestWordIndex).getTitle() + "\n" + wordInterestList.get(interestWordIndex + 1).getTitle());
                sendSignal("word", wordInterestList.get(interestWordIndex).getTitle() + "\n" + wordInterestList.get(interestWordIndex + 1).getTitle());
            } else {
                noHaveMoreInterestList();
            }
        } else {
            noHaveMoreInterestList();
        }
    }

    @Click(R.id.level_test_know_layout)
    void levelTestKnowClicked() {
        if (levelTestWordList.size() > levelTestWordIndex)
            sendLevelWordJudgement(true, levelTestWordList.get(levelTestWordIndex).getWordSeq());
        else {
            finishSendLevelWord(null);
        }
    }

    @Click(R.id.level_test_dont_know_layout)
    void levelTestDontKnowClicked() {
        if (levelTestWordList.size() > levelTestWordIndex)
            sendLevelWordJudgement(false, levelTestWordList.get(levelTestWordIndex).getWordSeq());
        else {
            finishSendLevelWord(null);
        }
    }

    @Click(R.id.next_word_layout)
    void onClickNextWord() {
        nextWordLayout.setClickable(false);
        setNextTopic();
    }

    @Click({R.id.toggle_video_btn, R.id.toggle_voice_btn})
    void onClickToggleVideo(View v) {
        if (mSession != null) {
            if (glviewLayout.getVisibility() == View.VISIBLE) {
                glview.setBackgroundResource(R.color.white);
                glviewMyCamera.setBackgroundResource(R.color.white);

                if (mSubscriber != null) {
                    mSubscriber.setSubscribeToVideo(false);
                }
                if (mPublisher != null) {
                    mPublisher.setPublishVideo(false);
                }
                glviewLayout.setVisibility(View.INVISIBLE);
                voiceItemLayout.setVisibility(View.VISIBLE);
                voiceCallLayout.setVisibility(View.VISIBLE);
                voiceTopicLayout.setVisibility(View.VISIBLE);
                videoItemLayout.setVisibility(View.INVISIBLE);
                videoCallLayout.setVisibility(View.INVISIBLE);


                if (v.getId() == R.id.toggle_voice_btn) {
                    if (mSession != null && mSubscriber != null) {
                        sendSignal("video", "disconnect");
                    }
                    toggleMuteBtn.setClickable(true);
                    toggleSpeakerBtn.setClickable(true);
                    toggleVideoBtn.setClickable(true);
                    internalVideoScreenDisable();
                    internalAcquireWakeLock();
                }
            }
            if (mSession != null && mSubscriber != null) {
                if (v.getId() == R.id.toggle_video_btn) {

                    sendSignal("video", "connect");
                    waitingLayout.setVisibility(View.VISIBLE);
                    videoItemLayout.setVisibility(View.VISIBLE);
                    videoCallLayout.setVisibility(View.VISIBLE);
                    requestVoiceBtn.setAlpha(0.5f);
                    requestVoiceBtn.setClickable(false);
                    toggleMuteBtn.setClickable(false);
                    toggleSpeakerBtn.setClickable(false);
                    toggleVideoBtn.setClickable(false);
                    internalReleaseWakeLock();
                }
            }
        } else {
            showNonePermission();
        }
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

    private void makeLinksFocusable(TextView tv) {
        MovementMethod m = tv.getMovementMethod();
        if ((m == null) || !(m instanceof LinkMovementMethod)) {
            if (tv.getLinksClickable()) {
                tv.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }
    }


    private void requestEnd(boolean isDuringCall) {
        UserInfo.myPushState = false;
        availCancelPushState = true;
//        if (!isHost) {
//            // CallTracker.track(303);
//        } else {
//            // CallTracker.track(304);
//        }
        sendSignal("announce", "endRequest");

        if (isDuringCall) {
            if (hasNextCall) {
                receivedStartSignal = false;
                finishMediaPlayer();
                mediaPlayer = com.forwiz.nursetree.util.MediaPlayer.getInstance(context, "waitmedia");
                mediaPlayer.setLooping(true);
                mediaPlayer.start();
                transitCallSession(sessionSeqToNextCall);
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
                transitCallSession(sessionSeqToNextCall);
            } else {
                Handler forceCancelCallHandler = new Handler();
                forceCancelCallHandler.postDelayed(this::commonCallEnd, FORCE_CANCEL_LIMIT_MILLIS);
            }
        }
    }


    @Click(R.id.switch_camera_btn)
    void onClickSwitchCameraButton() {
        mPublisher.cycleCamera();
    }

    @CheckedChange(R.id.toggle_speaker_btn)
    void onToggleSpeakerButton(boolean isChecked, CompoundButton button) {
        if (isChecked) {
            previousAudioOutSpeaker = true;
            button.setBackgroundResource(R.drawable.calling_icon_speaker_on);
            toggleSpeakerText.setTextColor(getResources().getColor(R.color.primaray_green_color101));
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            am.setMode(AudioManager.MODE_IN_COMMUNICATION);
            am.setSpeakerphoneOn(true);
            toggleSpeakerText.setText(R.string.nt_speaker_text);
        } else {
            previousAudioOutSpeaker = false;
            button.setBackgroundResource(R.drawable.calling_icon_speaker_off);
            toggleSpeakerText.setTextColor(getResources().getColor(R.color.secondary_black_color201));
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            am.setMode(AudioManager.MODE_IN_COMMUNICATION);
            am.setSpeakerphoneOn(false);


            toggleSpeakerText.setText(R.string.nt_speaker_text);
        }
    }

    @CheckedChange(R.id.toggle_mute_btn)
    void onToggleMuteButton(boolean isChecked, CompoundButton button) {
        if (isChecked) {
            if (mPublisher != null) {
                mPublisher.setPublishAudio(false);
                button.setBackgroundResource(R.drawable.calling_icon_mute_on);
                toggleMuteText.setTextColor(getResources().getColor(R.color.primaray_green_color101));
                toggleMuteText.setText(R.string.nt_mute_text);
            }
        } else {
            if (mPublisher != null) {
                mPublisher.setPublishAudio(true);
                button.setBackgroundResource(R.drawable.calling_icon_mute_off);
                toggleMuteText.setTextColor(getResources().getColor(R.color.secondary_black_color201));
                toggleMuteText.setText(R.string.nt_mute_text);
            }
        }
    }
    //endregion

    @UiThread
    public void updateStudentFeedback(String feedback) {
        studentFeedbackMessage.setVisibility(View.VISIBLE);
        studentFeedbackMessage.setText(feedback);
    }

    @UiThread
    public void invisibleFeedback() {
        studentFeedbackMessage.setVisibility(View.INVISIBLE);
    }

    @UiThread
    public void noHaveMoreInterestList() {
        alreadyEndInterest = true;
        StaticsUtility.NEED_INTEREST_TEST = false;
        interestOrAnalyzingImageView.setImageResource(R.drawable.icon_ai_analyzing);
        aiSubTitleTextView.setText("Analyzing Level");
        setPriorityTopic();
    }

//    @UiThread
//    public void alreadyInterestRequestLevelTest() {
//        interestOrAnalyzingImageView.setImageResource(R.drawable.icon_ai_analyzing);
//        aiSubTitleTextView.setText("Analyzing Level");
//        requestLevelTestList();
//    }

    //관심 토픽 전송
    @Background
    void setPriorityTopic() {
        choiceSeqList.clear();
        choiceSeqList = getIntegerArrayPref(context);

        for (int i = 0; i < choiceSeqList.size(); i++) {
            wordInterestStringList.add(String.valueOf(choiceSeqList.get(i)));
        }
        if (wordInterestStringList.size() == 0) {
            wordInterestStringList.add("0");
        }
        HashMap<String, Object> params = new HashMap<>();
        params.put("choice_seq", wordInterestStringList);

        if (wordInterestStringList.size() > 0) {
            Request request;
            request = ServerApiManager.getRequestPost(ServerApiManager.ServerApi.WORD_INTEREST_PRIORITY, params);

            OkHttpClient.Builder b = new OkHttpClient.Builder();
            b.readTimeout(120, TimeUnit.SECONDS);
            OkHttpClient client = b.build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());

                        String result = jsonObject.getString("status");
                        switch (result) {
                            case "success":
                                Log.d("priorityTopic", "success");
                                requestLevelTestList();
                                break;
                            case "none_interest":
                                Log.d("priorityTopic", "none_interest");
                                requestLevelTestList();
                                break;
                            case "none_user":
                                Log.d("priorityTopic", "none_user");
                                showNoneUserDialog();
                                break;
                            default:
                                Log.d("priorityTopic", "default result : " + result);
                                break;
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            requestLevelTestList();
            Log.d("priorityTopic", "no have interest List ");
        }
    }


    //레벨 판정용 단어 인지 여부
    @Background
    void sendLevelWordJudgement(boolean isKnowWord, int levelWordSeq) {
        HashMap<String, Object> params = new HashMap<>();
        List<String> levelWordSeqList = new ArrayList<>();
        levelWordSeqList.add(String.valueOf(levelWordSeq));
        List<String> levelWordAnswer = new ArrayList<>();
        levelWordAnswer.add(String.valueOf(isKnowWord));

        params.put("word_seq", levelWordSeqList);
        params.put("word_answer", levelWordAnswer);

        Request request = ServerApiManager.getRequestPost(ServerApiManager.ServerApi.WORD_LEVEL_JUDGEMENT, params);

        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                showFailConnectDialog();
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                JSONObject jsonObject;
                try {
                    jsonObject = new JSONObject(response.body().string());
                    String status = jsonObject.getString("status");

                    switch (status) {
                        case "success":
                            ObjectLevelWord receivedWord = new ObjectLevelWord(jsonObject.getJSONObject("word"));
                            levelTestWordList.add(receivedWord);
                            Log.d("sendLevelWord", "result success : " + receivedWord.toString());
                            successSendLevelWord();
                            break;
                        case "finish":
                            Log.d("sendLevelWord", "result finish");
                            levelTestWordIndex++;
                            ObjectLevelWord finishWord = new ObjectLevelWord(jsonObject.getJSONObject("word"));
                            finishSendLevelWord(finishWord);
                            break;
                        case "none_word":
                            levelTestWordIndex++;
                            finishSendLevelWord(null);
                            Log.d("sendLevelWord", "result none_word");
                            break;
                        case "already_judge":
                            Log.d("sendLevelWord", "result already_judge");
                            levelTestWordIndex++;
                            finishSendLevelWord(null);
                            break;
                        case "none_user":
                            Log.d("sendLevelWord", "result none_user");
                            break;
                        default:
                            Log.d("sendLevelWord", "result default : " + status);
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

    @UiThread
    public void successSendLevelWord() {
        levelTestWordIndex++;
        if (levelTestWordList.size() > levelTestWordIndex) {
            voiceMinorTopicText.setText(levelTestWordList.get(levelTestWordIndex).getTitle());
            if (isHost) {
                sendSignal("word", levelTestWordList.get(levelTestWordIndex).getTitle());
            }
        } else {
            Log.d("successSendLevelWord", "exceed word index.");
        }
    }

    @UiThread
    public void receivedViewTypeWord() {
        nurseTreeAILayout.setVisibility(View.VISIBLE);
        wantLearnLayout.setVisibility(View.GONE);
        notInterestLayout.setVisibility(View.GONE);
        levelTestKnowLayout.setVisibility(View.GONE);
        levelTestDontKnowLayout.setVisibility(View.GONE);
        aiTitleTextView.setVisibility(View.INVISIBLE);

    }

    @UiThread
    public void finishSendLevelWord(ObjectLevelWord finishWord) {
        levelTestWordIndex++;
        nurseTreeAILayout.setVisibility(View.VISIBLE);
        wantLearnLayout.setVisibility(View.GONE);
        notInterestLayout.setVisibility(View.GONE);
        levelTestKnowLayout.setVisibility(View.GONE);
        levelTestDontKnowLayout.setVisibility(View.GONE);
        alreadyLevelTest = true;

        aiTitleTextView.setVisibility(View.INVISIBLE);
        interestOrAnalyzingImageView.setImageResource(callIconList[Integer.parseInt(categoryIconId)]);
        aiSubTitleTextView.setText(passedTopic);
        voiceMinorTopicText.setText(currentWord);
        levelTestWordList.clear();
        if (isHost) {
            nextWordLayout.setVisibility(View.VISIBLE);
            nurseTreeAILayout.setVisibility(View.VISIBLE);
            sendSignal("viewType", "word");
            sendSignal("word", currentWord);
            if (finishWord != null) {
                sendSignal("categorySeq", String.valueOf(finishWord.getInterestSeq()));
                sendSignal("categoryTitle", finishWord.getTitle());
            } else {
                sendSignal("categorySeq", String.valueOf(categoryIconId));
                sendSignal("categoryTitle", passedTopic);
            }
        }
        if (StaticsUtility.IS_CARE_TEAM) {
            nextWordLayout.setVisibility(View.INVISIBLE);
        }
    }

    //region LTM 및 단어 기타 상호작용 코드
    @Background
    void setNextTopic() {
        Request request = ServerApiManager.getRequestPost(ServerApiManager.ServerApi.WORD_LTM_FORWARD);

        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                showFailConnectDialog();
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                JSONObject jsonObject;
                try {
                    jsonObject = new JSONObject(response.body().string());
                    String status = jsonObject.getString("status");

                    if (status.equals("success")) {
                        TopicCategoryWordObject receivedWord = new TopicCategoryWordObject(jsonObject.getJSONObject("word"));
                        if (receivedWord.getInterestWord().getIconId().startsWith("icon_")) {
                            receivedWord.getInterestWord().setIconId(receivedWord.getInterestWord().getIconId().replaceAll("icon_", ""));
                        } else {
                            receivedWord.getInterestWord().setIconId("0");
                        }
                        if (receivedWord.getInterestWord().getTitle().isEmpty()) {
                            receivedWord.getInterestWord().setTitle("Free Talk");
                        }
                        categoryIconId = String.valueOf(receivedWord.getInterestWord().getIconId());
                        passedTopic = receivedWord.getInterestWord().getTitle();

                        updateMyCurrentWord(receivedWord);
                    } else if (status.equals("finished_category")) {
                        setNextWordEnable();
                    } else if (status.equals("none_word")) {
                        TopicCategoryWordObject receivedWord = new TopicCategoryWordObject();
                        updateMyCurrentWord(receivedWord);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

    @UiThread
    void receivedLevelType() {
        nurseTreeAILayout.setVisibility(View.VISIBLE);
        aiTitleTextView.setVisibility(View.VISIBLE);
        nurseTreeAILayout.setVisibility(View.VISIBLE);
        aiSubTitleTextView.setText("Analyzing Level");
        interestOrAnalyzingImageView.setImageResource(R.drawable.icon_ai_analyzing);
        levelTestKnowLayout.setVisibility(View.GONE);
        levelTestDontKnowLayout.setVisibility(View.GONE);
    }

    @UiThread
    void receivedInterestType() {
        aiTitleTextView.setVisibility(View.VISIBLE);
        nurseTreeAILayout.setVisibility(View.VISIBLE);
        aiSubTitleTextView.setText("Learning Interests");
        interestOrAnalyzingImageView.setImageResource(R.drawable.icon_ai_interest);
        voiceMinorTopicText.setVisibility(View.VISIBLE);
    }

    @UiThread
    void requestInterestDone() {
        Log.d(LOG_TAG, "requestinterestDone");
        interestIntegerList.clear();
        for (int i = 0; i < wordInterestList.size(); i++) {
            interestIntegerList.add(0);
        }
        nurseTreeAILayout.setVisibility(View.VISIBLE);
        if (isHost) {
            wantLearnLayout.setVisibility(View.VISIBLE);
            notInterestLayout.setVisibility(View.VISIBLE);
        }
        SharedPreferences prefs = getSharedPreferences("interest", MODE_PRIVATE);
        interestWordIndex = prefs.getInt("interestWordIndex", 0);
        if (wordInterestList.size() > interestWordIndex) {
            voiceMinorTopicText.setText(wordInterestList.get(interestWordIndex).getTitle() + "\n" + wordInterestList.get(interestWordIndex + 1).getTitle());
            sendSignal("viewType", "interest");
            sendSignal("word", wordInterestList.get(interestWordIndex).getTitle() + "\n" + wordInterestList.get(interestWordIndex + 1).getTitle());
        } else {
            alreadyEndInterest = true;
        }
    }

    @UiThread
    void requestLevelWordDone(ObjectLevelWord levelWord) {
        interestOrAnalyzingImageView.setImageResource(R.drawable.icon_ai_analyzing);
        aiTitleTextView.setVisibility(View.VISIBLE);
        aiSubTitleTextView.setText("Analyzing Level");
        if (levelWord != null) {
            voiceMinorTopicText.setText(levelWord.getTitle());
        }
        nurseTreeAILayout.setVisibility(View.VISIBLE);
        wantLearnLayout.setVisibility(View.GONE);
        notInterestLayout.setVisibility(View.GONE);
        nextWordLayout.setVisibility(View.GONE);
        if (isHost) {
            levelTestKnowLayout.setVisibility(View.VISIBLE);
            levelTestDontKnowLayout.setVisibility(View.VISIBLE);
            sendSignal("viewType", "level");
            if (levelWord == null) {
                sendSignal("word", "Free Talk");
            }
            sendSignal("word", levelWord.getTitle());
        }
    }

    @UiThread
    void setNextWordEnable() {
        nextWordLayout.setClickable(true);
    }

    @UiThread
    void showNextWordLayout() {
        nextWordLayout.setVisibility(View.VISIBLE);
        voiceMinorTopicText.setText(currentWord);
        if (StaticsUtility.IS_CARE_TEAM) {
            nextWordLayout.setVisibility(View.INVISIBLE);
        }
    }

    @UiThread(propagation = UiThread.Propagation.REUSE)
    void updateMyCurrentWord(TopicCategoryWordObject receivedWord) {
        sendSignal("viewType", "word");
        if (receivedWord != null) {
            passedWord = receivedWord.getTitle();
            currentWord = receivedWord.getTitle();
            if (receivedWord.getInterestWord().getIconId().isEmpty()) {
                receivedWord.getInterestWord().setIconId("0");
            }
            interestOrAnalyzingImageView.setImageResource(callIconList[Integer.parseInt(receivedWord.getInterestWord().getIconId())]);
            aiSubTitleTextView.setText(receivedWord.getInterestWord().getTitle());
        }
        if (receivedWord == null) {
            passedWord = "Free Talk";
            currentWord = "Free Talk";
            interestOrAnalyzingImageView.setImageResource(callIconList[0]);
            aiSubTitleTextView.setText("Free Talk");
        }
        voiceMinorTopicText.setText(currentWord);

        voiceMinorTopicText.setVisibility(View.VISIBLE);
        if (alreadyEndInterest) {
            nextWordLayout.setVisibility(View.VISIBLE);
        }
        if (mSession != null && mSubscriber != null) {
            if (currentWord.equals("")) {
                currentWord = "Free Talk";
                voiceMinorTopicText.setText(currentWord);
                sendSignal("categorySeq", "0");
                sendSignal("categoryTitle", "Free Talk");
            } else {
                sendSignal("categorySeq", receivedWord.getInterestWord().getIconId());
                sendSignal("categoryTitle", receivedWord.getInterestWord().getTitle());
            }
            sendSignal("word", currentWord);
        }
        nextWordLayout.setClickable(true);

        if (StaticsUtility.IS_CARE_TEAM) {
            nextWordLayout.setVisibility(View.INVISIBLE);
        }

    }


    @UiThread(propagation = UiThread.Propagation.REUSE)
    void updateMyCurrentWord(String myCurrentWord) {
        passedWord = myCurrentWord;
        currentWord = myCurrentWord;
        voiceMinorTopicText.setText(currentWord);

        voiceMinorTopicText.setVisibility(View.VISIBLE);
        if (alreadyEndInterest) {
            nextWordLayout.setVisibility(View.VISIBLE);
        }
        if (mSession != null && mSubscriber != null) {
            sendSignal("viewType", "word");
            sendSignal("word", currentWord);
            sendSignal("categorySeq", String.valueOf(categoryIconId));
            sendSignal("categoryTitle", passedTopic);
        }

    }


    @Background
    void sendRating(int ratingStar) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("session_seq", StaticsUtility.INBOUND);

        if (!hasNextCall) {
            params.put("session_seq", StaticsUtility.INBOUND);
        } else if (!previousRoomNum.isEmpty() && previousRoomNum != StaticsUtility.INBOUND) {
            params.put("session_seq", previousRoomNum);
        }
        params.put("rating", String.valueOf(ratingStar));
        Request request = ServerApiManager.getRequestPost(ServerApiManager.ServerApi.SESSION_RATING, params);

        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                showFailConnectDialog();
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                JSONObject jsonObject;
                try {
                    jsonObject = new JSONObject(response.body().string());
                    Log.i("dev_yh_msg", jsonObject.toString());
                    String status = jsonObject.getString("status");
                    switch (status) {
                        case "success":
                            Log.d("successSend", "레이팅 전송 성공 ");
                            if (receivedStartSignal && hasNextCall) {
                                hasNextCall = false;
                            } else {
                                successSendAndCheckRefresh();
                            }
                            break;
                        case "already_rating":
                            showAlreadyRating();
                            break;
                        case "none_permission":
                            showNonePermission();
                            break;
                        case "none_session":
                            showNoneSessionDialog();
                            break;
                        case "none_user":
                            showNoneUserDialog();
                            break;
                        default:
                            showFailConnectDialog(status, response.body().string());
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // 통화가 끝난 후 피드백화면에서 피드백을 전송하였을때
        if (requestCode == FEEDBACK_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                alreadyRating = true;
                if (data != null) {
                    feedbackRating = data.getIntExtra("FEEDBACK_RATING", 0);
                }

                sendRating(feedbackRating);
            }
        }
        // 통화도중 피드백화면에서 피드백을 전송하였을때
        else {
            if (resultCode == RESULT_OK) {
                alreadyRating = true;
                if (data != null) {
                    feedbackRating = data.getIntExtra("FEEDBACK_RATING", 0);
                }
                if (duringCallEndDialog != null) {
                    duringCallEndDialog.dismiss();
                }

                requestEnd(true);
            }
        }
    }

    @Background
    void successSendAndCheckRefresh() {
        Request request = ServerApiManager.getRequestGet(ServerApiManager.ServerApi.WORD_LTM_AVAILABLE);

        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                showFailConnectDialog();
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    String status = jsonObject.getString("status");
                    if (status.equals("success")) {
                        int wordCount;
                        wordArray = jsonObject.getJSONArray("words");
                        wordCount = wordArray.length();
                        if (wordCount > 0) {
                            haveLTMList();
                            gotoRefreshActivity();
                        } else {
                            finish();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

    @UiThread(propagation = UiThread.Propagation.REUSE)
    void haveLTMList() {
        nextWordLayout.setClickable(true);
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        callEndBtn.setAlpha(0.5f);
        refreshBackgroundView.setVisibility(View.VISIBLE);
        refreshLayout.setVisibility(View.VISIBLE);
    }


    @UiThread(propagation = UiThread.Propagation.REUSE)
    void gotoRefreshActivity() {
        nextWordLayout.setClickable(true);
        if (wordArray != null && wordArray.length() > 0) {

            final CountDownTimer[] timer = {null};
            timer[0] = new CountDownTimer(1000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {

                }

                @Override
                public void onFinish() {
                    timer[0].cancel();
                    timer[0] = null;
//                    Intent intent = new Intent(context, RefreshTimeActivity_.class);
//                    intent.putExtra("jsonArray", wordArray.toString());
//                    startActivity(intent);
//                    overridePendingTransition(R.anim.anim_fade_in, R.anim.anim_fade_out);
                }
            };
            timer[0].start();
        }
    }


    @UiThread(propagation = UiThread.Propagation.REUSE)
    void showAlreadyRating() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        if (context != null) {
            if (!activity.isFinishing()) {
                NTAlertDialog.getComfirmDialog(context, R.string.nt_rating_title, R.string.nt_already_rating_msg, (DialogInterface dialog, int which) -> {
                    dialog.dismiss();
                    finish();
                }).setCancelable(false).show();
            }
        }
    }


    @UiThread(propagation = UiThread.Propagation.REUSE)
    void showNonePermission() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        if (context != null) {
            if (!this.isFinishing()) {
                NTAlertDialog.getComfirmDialog(context, R.string.nt_permission_title, R.string.nt_none_permission_msg, (DialogInterface dialog, int which) -> {
                    successSendAndCheckRefresh();
                }).setCancelable(false).show();
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


    @UiThread(propagation = UiThread.Propagation.REUSE)
    void showNoneUserDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        if (!this.isFinishing()) {
            NTAlertDialog.getComfirmDialog(context, R.string.signin_text, R.string.nt_login_none_user_msg, (DialogInterface dialog, int which) -> {
                dialog.dismiss();
                finish();
//                ChooseSignActivity_.intent(context).flags(Intent.FLAG_ACTIVITY_NEW_TASK).start();
            }).setCancelable(false).show();
        }
    }

    @UiThread(propagation = UiThread.Propagation.REUSE)
    void showFailConnectDialog(String status, String result) {
        nextWordLayout.setClickable(true);
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        if (!this.isFinishing()) {
            NTAlertDialog.getComfirmDialog(
                    context,
                    R.string.signin_text,
                    status + "result : " + result,
                    (DialogInterface dialog, int which) -> {
                    }
            ).setCancelable(false).show();
        }
    }

    @UiThread(propagation = UiThread.Propagation.REUSE)
    void showFailConnectDialog() {
        nextWordLayout.setClickable(true);
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        if (!this.isFinishing()) {
            NTAlertDialog.getComfirmDialog(
                    context,
                    R.string.signin_text,
                    R.string.nt_check_your_server_msg,
                    (DialogInterface dialog, int which) -> {
                    }
            ).setCancelable(false).show();
        }
    }
    //endregion


    @Override
    public void onConnected(Session session) {

        Log.i(LOG_TAG, "Session Connected");

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

        if (receivedStartSignal) {
            //재연결 시도 error 에서의 재연결 상태 구분하여 재연결 시도
            if (!isReconnect) {
                isReconnect = true;
                final CountDownTimer[] timer = {null};
                timer[0] = new CountDownTimer(3000, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {

                    }

                    @Override
                    public void onFinish() {
                        timer[0].cancel();
                        timer[0] = null;

                        //받아온 종료시간으로 비교
                        Date nowCheckDate = new Date();
                        Locale systemLocale = getResources().getConfiguration().locale;
                        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", systemLocale);
                        SimpleDateFormat nowCompareFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", systemLocale);
                        format.setTimeZone(TimeZone.getTimeZone("UTC"));
                        String nowTimeString = format.format(nowCheckDate);

                        Date compareNowDate;
                        try {
                            if (expEndDate != null) {
                                compareNowDate = nowCompareFormat.parse(nowTimeString);
                                long diff = expEndDate.getTime() - compareNowDate.getTime();
                                long gapTime = TimeUnit.MILLISECONDS.toSeconds(diff);
                                Log.d("gapexitTime", gapTime + " < this is gap Time.");
                                if (gapTime < 20) {
                                    commonCallEnd();
                                } else {
                                    if (mSession != null && !isUserRequestExit) {
                                        mSession.connect(TBOX_TOKEN);
                                        Log.e(LOG_TAG, "session connect onDisconnected");
                                    }
                                }
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                    }
                };
                timer[0].start();
            }
        } else {
            commonCallEnd();
        }
    }

    @Override
    public void onStreamReceived(Session session, Stream stream) {
        Log.i(LOG_TAG, "Stream Received");

        //builder는 앱 컨텍스트, 보고자하는 스트림 이 두가지를 매개 변수로 사용한다.
        mSubscriber = new Subscriber.Builder(this, stream).build();
        if (mSession != null && mSubscriber != null) {
            mSubscriber.setSubscriberListener(this);
            mSubscriber.setSubscribeToVideo(false);
            toggleSpeakerBtn.setChecked(false);
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            am.setMode(AudioManager.MODE_IN_COMMUNICATION);
            am.setSpeakerphoneOn(false);
            mSubscriber.setAudioStatsListener(this);
            mSubscriber.setStreamListener(this);
            if (!receivedStartSignal) {
                mSubscriber.setSubscribeToAudio(false);
            }
            subscriberList.add(mSubscriber);
            mSession.subscribe(mSubscriber);
        }

        if (!receivedStartSignal && mSubscriber != null) {
            toggleSpeakerBtn.setChecked(false);
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            am.setMode(AudioManager.MODE_IN_COMMUNICATION);
            am.setSpeakerphoneOn(false);
            mSubscriber.setSubscribeToAudio(false);
        } else if (receivedStartSignal) {
            finishMediaPlayer();
            for (Subscriber subscriber : subscriberList) {
                if (subscriber != null) {
                    subscriber.setSubscribeToAudio(true);
                }
            }
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            am.setMode(AudioManager.MODE_IN_COMMUNICATION);
            am.setSpeakerphoneOn(false);
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
        hideDisconnectCallLayout();
        alreadyEnd = false;
        internalVideoScreenDisable();
        hideReconnectCallLayout(true);

        //받아온 종료시간으로 비교
        Date nowCheckDate = new Date();
        Locale systemLocale = getResources().getConfiguration().locale;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", systemLocale);
        SimpleDateFormat nowCompareFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", systemLocale);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        String nowTimeString = format.format(nowCheckDate);

        Date compareNowDate;
        try {
            if (expEndDate != null) {
                compareNowDate = nowCompareFormat.parse(nowTimeString);
                long diff = expEndDate.getTime() - compareNowDate.getTime();
                long gapTime = TimeUnit.MILLISECONDS.toSeconds(diff);
                if (gapTime < 10) {
                    commonCallEnd();
                }
            }

        } catch (ParseException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {
        Log.i(LOG_TAG, "Stream Dropped");
    }

    @Override
    public void onError(Session session, OpentokError opentokError) {
        Log.i(LOG_TAG, "Stream Error");
        int errCode = opentokError.getErrorCode().getErrorCode();
        if (errCode == CONNECTION_FAILED || errCode == NOT_CONNECTED || errCode == SESSION_CONNECTION_TIMEOUT
                || errCode == CONNECTION_DROPPED_ERROR || errCode == CONNECTION_REFUSED || errCode == SESSION_PUBLISHER_NOT_FOUND
                || errCode == UNKNOWN_PUBLISHER_INSTANCE || errCode == UNKNOWN_SUBSCRIBER_INSTANCE) {
            if (receivedStartSignal && !lowConnectSignalWhileTransit) {
                showDisconnectCallLayout();
                showReconnectCallLayout();
            }

            if (!isUserRequestExit && !isReconnect) {
                isReconnect = true;
                final CountDownTimer[] timer = {null};
                timer[0] = new CountDownTimer(3000, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {

                    }

                    @Override
                    public void onFinish() {
                        timer[0].cancel();
                        timer[0] = null;

                        //받아온 종료시간으로 비교
                        Date nowCheckDate = new Date();
                        Locale systemLocale = getResources().getConfiguration().locale;
                        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", systemLocale);
                        SimpleDateFormat nowCompareFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", systemLocale);
                        format.setTimeZone(TimeZone.getTimeZone("UTC"));
                        String nowTimeString = format.format(nowCheckDate);

                        Date compareNowDate;
                        try {
                            if (expEndDate != null) {
                                compareNowDate = nowCompareFormat.parse(nowTimeString);
                                long diff = expEndDate.getTime() - compareNowDate.getTime();
                                long gapTime = TimeUnit.MILLISECONDS.toSeconds(diff);
                                if (gapTime < 20) {
                                    commonCallEnd();
                                } else {
                                    if (mSession != null) {
                                        Log.e(LOG_TAG, "session connect onError");
                                        mSession.connect(TBOX_TOKEN);
                                        isReconnect = false;
                                    }
                                }
                            } else {
                                if (mSession != null) {
                                    Log.e(LOG_TAG, "session connect onError");
                                    mSession.connect(TBOX_TOKEN);
                                    isReconnect = false;
                                }
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                    }
                };
                timer[0].start();
            }
        }

    }

    @Override
    public void onSignalReceived(Session session, String type, String data, Connection
            connection) {
        if (!callEndBtn.isClickable() && !nextWordLayout.isClickable() && !type.equals("wordRequest")) {
            hideReconnectCallLayout(true);
        }
        String myConnectionId = session.getConnection().getConnectionId();
        if (connection != null && connection.getConnectionId().equals(myConnectionId)) {
        } else {
            Log.d("receivedSignal", "signal type : " + type + " signal data : " + data);

            Log.i(LOG_TAG, "receivedSignal signal type : " + type + " signal data : " + data);
            switch (type) {
                case "word":
                    passedWord = data;
                    currentWord = data;
                    if (currentWord.equals("")) {
                        currentWord = "Free Talk";
                    }
                    voiceMinorTopicText.setText(currentWord);
                    voiceMinorTopicText.setVisibility(View.VISIBLE);
                    break;
                case "categorySeq":
                    if (data.equals("")) {
                        interestOrAnalyzingImageView.setImageResource(callIconList[Integer.parseInt("0")]);
                    } else {
                        interestOrAnalyzingImageView.setImageResource(callIconList[Integer.parseInt(data)]);
                    }
                    break;
                case "categoryTitle":
                    aiSubTitleTextView.setText(data);
                    break;
                case "announce":
                    if (data.startsWith("transit")) {
//                        if (isHost) {
//                            // CallTracker.track(143);
//                        } else {
//                            // CallTracker.track(142);
//                        }
                        lowConnectSignalWhileTransit = true;
                        hasNextCall = true;
                        previousRoomNum = StaticsUtility.INBOUND;
                        Log.d("transit", "sessionSeqToNextCall  transit: API_KEY" + API_KEY + " Session ID : " + SESSION_ID + " Tbox Token" + TBOX_TOKEN);
                        int sessionSeqStartIndex = data.indexOf(";");
                        sessionSeqToNextCall = data.substring(sessionSeqStartIndex + 1);
                        transitCallSession(sessionSeqToNextCall);
                        Log.d("transit", "sessionSeqToNextCall " + sessionSeqToNextCall);
                        break;
                    }
                    switch (data) {
                        case "endSoon":
                            lowConnectSignalWhileTransit = true;
//                            if (isHost) {
//                                // CallTracker.track(153);
//                            } else {
//                                // CallTracker.track(152);
//                            }
                            finishMediaPlayer();
                            mediaPlayer = com.forwiz.nursetree.util.MediaPlayer.getInstance(context, "endalert");
                            mediaPlayer.start();

                            mediaPlayer.setOnCompletionListener((MediaPlayer media) -> {
                                finishMediaPlayer();
                                mediaPlayer = com.forwiz.nursetree.util.MediaPlayer.getInstance(context, "endalert");
                                mediaPlayer.start();
                            });
                            break;
                        case "end":
//                            if (isHost) {
//                                // CallTracker.track(156);
//                            } else {
//                                // CallTracker.track(155);
//                            }
                            if (hasNextCall) {
                                finishMediaPlayer();
                                mediaPlayer = com.forwiz.nursetree.util.MediaPlayer.getInstance(context, "waitmedia");
                                mediaPlayer.setLooping(true);
                                mediaPlayer.start();
                                toggleSpeakerBtn.setChecked(false);
                                AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                                am.setMode(AudioManager.MODE_IN_COMMUNICATION);
                                am.setSpeakerphoneOn(false);
                                Log.d("transit", "sessionSeqToNextCall  end: API_KEY" + API_KEY + " Session ID : " + SESSION_ID + " Tbox Token" + TBOX_TOKEN);
//                                // CallTracker.updateSessionId(SESSION_ID);
                                setSession(API_KEY, SESSION_ID);
                            } else {
                                commonCallEnd();
                            }
                            break;
                        case "endRequest":
                            availCancelPushState = true;
//                            if (isHost) {
//                                // CallTracker.track(306);
//                            } else {
//                                // CallTracker.track(305);
//                            }
                            isUserRequestExit = true;
                            sendSignal("announce", "end");
                            if (hasNextCall) {
                                receivedStartSignal = false;
                                finishMediaPlayer();
                                mediaPlayer = com.forwiz.nursetree.util.MediaPlayer.getInstance(context, "waitmedia");
                                mediaPlayer.setLooping(true);
                                mediaPlayer.start();
                                Log.d("transit", "sessionSeqToNextCall   endRequest: API_KEY" + API_KEY + " Session ID : " + SESSION_ID + " Tbox Token" + TBOX_TOKEN);
//                                // CallTracker.updateSessionId(SESSION_ID);
                                setSession(API_KEY, SESSION_ID);
                                if (callStopTimeTask != null) {
                                    callStopTimeTask.cancel(true);
                                    callStopTimeTask = null;
                                }
                            } else {
                                Handler endHandler = new Handler();
                                endHandler.postDelayed(this::commonCallEnd, FORCE_CANCEL_LIMIT_MILLIS);
                            }
                            break;
                        case "start":
//                            if (isHost) {
//                                // CallTracker.track(123);
//                            } else {
//                                // CallTracker.track(122);
//                            }
                            finishMediaPlayer();
                            receivedStartSignal = true;
                            StaticsUtility.userExitRoom = StaticsUtility.INBOUND;
                            Log.d("receivedSignalStart", "session " + mSession + " mPublisher : " + mPublisher + " mSubscriber" + mSubscriber);
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
                                        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                                        am.setMode(AudioManager.MODE_IN_COMMUNICATION);
                                        am.setSpeakerphoneOn(false);
                                    }
                                }, 1500);
                                interestAfterStartSignal(true, 1.0f);
                                if (StaticsUtility.NEED_INTEREST_TEST && !alreadyEndInterest && isHost) {
                                    requestInterestDone();
//                                    sendSignal("viewType", "interest");
//                                    sendSignal("word", wordInterestList.get(interestWordIndex).getTitle() + "\n" + wordInterestList.get(interestWordIndex + 1).getTitle());
                                } else if (isHost) {
                                    if (!levelTestWordList.isEmpty() && levelTestWordList.size() > 0) {
                                        requestLevelWordDone(levelTestWordList.get(0));
                                    } else {
                                        sendSignal("viewType", "word");
                                        sendSignal("word", currentWord);
                                        sendSignal("categorySeq", String.valueOf(categoryIconId));
                                        sendSignal("categoryTitle", passedTopic);
                                    }
                                }
                            }
                            lowConnectSignalWhileTransit = false;
                            break;
                    }
                    break;
                case "wordRequest":
                    if (isHost && receivedStartSignal) {
                        if (StaticsUtility.NEED_INTEREST_TEST) {
                            requestInterestDone();
                        } else if (!levelTestWordList.isEmpty() && levelTestWordList.size() > 0) {
                            requestLevelWordDone(levelTestWordList.get(0));
                        } else {
                            updateMyCurrentWord(currentWord);
                        }
                    }
                    break;

                case "viewType":
                    switch (data) {
                        case "interest":
                            if (!isHost)
                                receivedInterestType();
                            break;
                        case "level":
                            receivedLevelType();
                            break;
                        case "word":
                            receivedViewTypeWord();
                            break;
                    }
            }
        }
    }

    private void interestAfterStartSignal(boolean clickable, float alpha) {
        wantLearnLayout.setClickable(clickable);
        notInterestLayout.setClickable(clickable);
        wantLearnLayout.setAlpha(alpha);
        notInterestLayout.setAlpha(alpha);
    }

    @Background
    void transitCallSession(String passedSessionSeq) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("session_seq", passedSessionSeq);

        Log.d("transit", "transit APISeq :  " + passedSessionSeq);
        Request request = ServerApiManager.getRequestGet(ServerApiManager.ServerApi.SESSION_TRANSIT, params);

        OkHttpClient.Builder b = new OkHttpClient.Builder();
        b.readTimeout(120, TimeUnit.SECONDS);
        b.writeTimeout(30, TimeUnit.SECONDS);

        OkHttpClient client = b.build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                Log.d("transit", "sessionTransit onFailure :  " + e.toString() + " response body : " + e.getMessage().toString());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                JSONObject jsonObject;
                Log.d("transit", " receivedOnResponse! ");
                try {
                    jsonObject = new JSONObject(response.body().string());
                    String status = jsonObject.getString("status");
                    switch (status) {
                        case "success":
                            JSONArray sessionValueArray = jsonObject.getJSONArray("sessions");
                            JSONObject sessionJson = sessionValueArray.getJSONObject(0);
                            JSONObject voipJson = sessionJson.getJSONObject("voipExtras");
                            String sessionSeq;
                            sessionSeq = sessionJson.getString("sessionSeq");
                            Log.d("transit", "sessionSeqToNextCall Success Case :  " + sessionSeq);
                            if (sessionSeq != null && !sessionSeq.isEmpty()) {
                                Date expEDate = null;
                                nextRoomNum = voipJson.getString(KEY_ROOM_NUMBER);
                                String roomNumber = voipJson.getString(KEY_ROOM_NUMBER);
                                String callServer = voipJson.getString(KEY_CALL_SERVER);
                                String callHost = voipJson.getString(KEY_CALL_HOST);
                                String tBoxToken = voipJson.getString(KEY_TBOX_TOKEN);
                                String tBoxApiKey = voipJson.getString(KEY_TBOX_APIKEY);
                                String expEDateString = voipJson.getString(KEY_EXPECT_END_DATE);
                                String topicTitle = voipJson.getString(KEY_TOPIC_TITLE);
                                String categorySeq = voipJson.getString(KEY_TCG_SEQ);
                                String categoryTitle = voipJson.getString(KEY_TCG_TITLE);
                                String categoryIcon = voipJson.getString(KEY_TCG_ICON);

                                if (expEDateString != null && !expEDateString.isEmpty()) {
                                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                                    try {
                                        expEDate = format.parse(expEDateString);
                                    } catch (ParseException e) {
                                        e.printStackTrace();
                                    }
                                    if (expEDate == null) {
                                        format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
                                        try {
                                            expEDate = format.parse(expEDateString);
                                        } catch (ParseException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }

                                int permissionCheck = ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_CALL_LOG);
                                if (permissionCheck != PackageManager.PERMISSION_DENIED) {
                                    addCallLog(context.getContentResolver());
                                }
                                StaticsUtility.CALL_SERVER = callServer;
                                StaticsUtility.ISHOST = Boolean.parseBoolean(callHost);
                                StaticsUtility.INBOUND = roomNumber;

                                SharedPreferences currentRoomNumberPrefs = activity.getSharedPreferences("lastRoomNumber", Activity.MODE_PRIVATE);
                                SharedPreferences.Editor editor = currentRoomNumberPrefs.edit();
                                editor.putString("roomNumber", roomNumber);
                                editor.apply();

                                passedTopic = categoryTitle;
                                passedWord = topicTitle;
                                topicIconSeq = categorySeq;
                                API_KEY = tBoxApiKey;
                                TBOX_TOKEN = tBoxToken;
                                SESSION_ID = roomNumber;
                                expEndDate = expEDate;
                                if (categoryIcon.startsWith("icon_")) {
                                    categoryIconId = categoryIcon.replaceAll("icon_", "");
                                } else {
                                    categoryIconId = "0";
                                }

                                Log.d("transit", "sessionSeqToNextCall SESSION_TRANSIT :  API_KEY" + API_KEY + " Session ID : " + SESSION_ID + " Tbox Token" + TBOX_TOKEN);
                            }

                            break;
                        case "none_session":
                            Log.d("transit", "none_session 세션 트랜짓 " + status + " Json :" + response.body().string());
                            break;
                        case "none_user":
                            Log.d("transit", "none_user 세션 트랜짓 " + status + " Json :" + response.body().string());
                            showNoneUserDialog();
                            break;
                        default:
                            Log.d("transit", "default 세션 트랜짓 " + status + " Json :" + response.body().string());
                            showFailConnectDialog(status, response.body().string());
                            break;
                    }
                } catch (JSONException e) {
                    Log.d("transit", "JSONException ");
                    e.printStackTrace();
                }
            }
        });
    }

    @UiThread
    void setSession(String sessionAPIKey, String sessionID) {
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

        availCancelPushState = true;
        if (mSession == null) {
            mSession = new Session.Builder(context, sessionAPIKey, sessionID).build();
            mSession.setSessionListener(this);
            mSession.setConnectionListener(this);
            mSession.setReconnectionListener(this);
            mSession.setArchiveListener(this);
            mSession.setStreamPropertiesListener(this);

            Log.d("transit", "sessionSeqToNextCall   setSession: API_KEY" + API_KEY + " Session ID : " + SESSION_ID + " Tbox Token" + TBOX_TOKEN);
            mSession.connect(TBOX_TOKEN);
            if (isHost) {


            } else {
                hasNextCall = false;
            }
        }
        Log.d(LOG_TAG, "setSisson enter");
    }

    private void sendSignal(String type, String data) {
//        if (mSession != null && mSubscriber != null && mSubscriber.getStream().getConnection().getConnectionId() != null) {
//            if (!mSubscriber.getStream().getConnection().getConnectionId().equals(subscribeConnection.getConnectionId())) {
//                mSession.sendSignal(type, data);
//            }
        if (mSession != null) {
            mSession.sendSignal(type, data);
        }
//        }
    }

    @Override
    public void onStreamCreated(PublisherKit publisherKit, Stream stream) {
        Log.i(LOG_TAG, "Publisher onStreamCreated");

        sendSignal("wordRequest", "");
        if (receivedStartSignal) {
            sendSignal("announce", "start");
        }
        if (isHost) {
            // CallTracker.track(109);
        } else {
            // CallTracker.track(105);
        }
        toggleSpeakerBtn.setChecked(false);
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

        if (!isUserRequestExit) {
            if (mPublisher != null && mPublisher.getStream() != null) {
                if (previousAudioPacket != 0) {
                    if (disconncetCallLayout.getVisibility() != View.VISIBLE) {
                        ringAlertMedia();
                        disconnectCallState.setText(R.string.opponent_network_unstable_text);
                        if (receivedStartSignal && !lowConnectSignalWhileTransit) {
                            showDisconnectCallLayout(); //1
                            showReconnectCallLayout();
                        }
                    }
                } else {
                    disconnectCallState.setText(R.string.connecting_signal_low_text);
                    if (receivedStartSignal && !lowConnectSignalWhileTransit) {
                        showDisconnectCallLayout();
                        showReconnectCallLayout();
                    }
                }
            }
        }
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
        disconnectCallState.setText(R.string.connecting_signal_low_text);
        ringAlertMedia();
        if (!lowConnectSignalWhileTransit) {
            showDisconnectCallLayout();
            showReconnectCallLayout();
        }

        //받아온 종료시간으로 비교
        Date nowCheckDate = new Date();
        Locale systemLocale = getResources().getConfiguration().locale;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", systemLocale);
        SimpleDateFormat nowCompareFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", systemLocale);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        String nowTimeString = format.format(nowCheckDate);

        Date compareNowDate;
        try {
            if (expEndDate != null) {
                compareNowDate = nowCompareFormat.parse(nowTimeString);
                long diff = expEndDate.getTime() - compareNowDate.getTime();
                long gapTime = TimeUnit.MILLISECONDS.toSeconds(diff);
                if (gapTime < 20) {
                    commonCallEnd();
                } else {
                    if (mSession != null && !isUserRequestExit) {
                        mSession.connect(TBOX_TOKEN);
                    }
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onReconnected(Session session) {
        Log.i(LOG_TAG, "onReconnected");
        finishMediaPlayer();
        hideDisconnectCallLayout();
        hideReconnectCallLayout(true);

        toggleSpeakerBtn.setChecked(previousAudioOutSpeaker);
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        am.setMode(AudioManager.MODE_IN_COMMUNICATION);
        am.setSpeakerphoneOn(previousAudioOutSpeaker);
        //FIXME : collect common procdure for connect/reconnect
        sendSignal("wordRequest", "");
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
    public void onAudioStats(SubscriberKit subscriberKit, SubscriberKit.SubscriberAudioStats
            subscriberAudioStats) {
        nowAudioPacket = subscriberAudioStats.audioPacketsReceived;
        checkAudioStats(subscriberAudioStats);

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

    private void checkAudioStats(SubscriberKit.SubscriberAudioStats stats) {
        double audioTimestamp = stats.timeStamp / 1000;

        if (previousAudioPacket == 0) {
            previousAudioPacket = (int) mPrevAudioPacketsRcvd;
        }
        //initialize values
        if (mPrevAudioTimestamp == 0) {
            mPrevAudioTimestamp = audioTimestamp;
            mPrevAudioBytes = stats.audioBytesReceived;
        }

        if (audioTimestamp - mPrevAudioTimestamp >= TIME_WINDOW) {
            //calculate audio packets lost ratio
            if (mPrevAudioPacketsRcvd != 0) {
                long pl = stats.audioPacketsLost - mPrevAudioPacketsLost;
                long pr = stats.audioPacketsReceived - mPrevAudioPacketsRcvd;
                long pt = pl + pr;

                if (pt > 0) {
                    mAudioPLRatio = (double) pl / (double) pt;
                }
            }
            mPrevAudioPacketsLost = stats.audioPacketsLost;
            mPrevAudioPacketsRcvd = stats.audioPacketsReceived;
            //calculate audio bandwidth
            mAudioBw = (long) ((8 * (stats.audioBytesReceived - mPrevAudioBytes)) / (audioTimestamp - mPrevAudioTimestamp));

            mPrevAudioTimestamp = audioTimestamp;
            mPrevAudioBytes = stats.audioBytesReceived;

            Log.i(LOGTAG, "Audio bandwidth (bps): " + mAudioBw + " Audio Bytes received: " + stats.audioBytesReceived + " Audio packet lost: " + stats.audioPacketsLost + " Audio packet loss ratio: " + mAudioPLRatio);

            if (mSubscriber != null && mSubscriber.getStream() != null) {
                if (!mSubscriber.getStream().hasAudio()) {
                    mAudioBw = 1;
                }
            }

            if (mSubscriber != null && mAudioBw < 1) {
                Log.i(LOGTAG, "Audio bandwidth below 1");
            } else if (mSubscriber != null && mAudioBw >= 1) {
                expireSecond = 7;
                hideDisconnectCallLayout();
                if (waitingLayout.getVisibility() != View.VISIBLE) {
                    hideReconnectCallLayout(false);
                }
            }

            if (expireSecond < 0) {
                //상대방이 통화상태가 좋지않을때(네트워크 상황이 좋지않을때) 체크하여 자신에게 띄워준다.
                if (mPublisher != null && mPublisher.getStream() != null) {
                    if (previousAudioPacket != 0) {
                        if (disconncetCallLayout.getVisibility() != View.VISIBLE) {
                            ringAlertMedia();
                            disconnectCallState.setText(R.string.opponent_network_unstable_text);
                            if (!lowConnectSignalWhileTransit) {
                                showDisconnectCallLayout();
                                showReconnectCallLayout();
                            }
                        }
                    }
                } else {
                    disconnectCallState.setText(R.string.connecting_signal_low_text);
                }
            } else if (receivedStartSignal) {
                finishMediaPlayer();
            } else {
                previousAudioPacket = nowAudioPacket;
            }
        }
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
//            callSec = currentTime;

            if (callSec <= 0) {
                callSec = 0;
            }
            int hours = callSec / 3600;
            int minutes = (callSec / 60) % 60;
            int secs = callSec % 60;

            String currentTimeString = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs);
            activity.updateTime(currentTimeString);

            //받아온 종료시간으로 비교
            Date nowCheckDate = new Date();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            SimpleDateFormat nowCompareFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            String nowTimeString = format.format(nowCheckDate);

            Date compareNowDate;
            try {
                if (activity.expEndDate != null) {
                    compareNowDate = nowCompareFormat.parse(nowTimeString);
                    long diff = activity.expEndDate.getTime() - compareNowDate.getTime();
                    long gapTime = TimeUnit.MILLISECONDS.toSeconds(diff);
                    if (gapTime <= 0) {
                        activity.requestEnd(false);
                    }
                }

            } catch (ParseException e) {
                e.printStackTrace();
            }

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
    void hideDisconnectCallLayout() {
        disconncetCallLayout.setVisibility(View.GONE);
    }

    @UiThread
    void showDisconnectCallLayout() {
        disconncetCallLayout.setVisibility(View.VISIBLE);
    }

    @UiThread
    void ringAlertMedia() {
        finishMediaPlayer();
        mediaPlayer = com.forwiz.nursetree.util.MediaPlayer.getInstance(context, "alertshort");
        mediaPlayer.setLooping(true);
        mediaPlayer.start();
    }

    @UiThread
    void showReconnectCallLayout() {
        callEndBtn.setClickable(false);
        nextWordLayout.setClickable(false);

        toggleMuteBtn.setClickable(false);
        toggleSpeakerBtn.setClickable(false);
        toggleVideoBtn.setClickable(false);
        requestVoiceBtn.setClickable(false);

        requestVoiceBtn.setAlpha(0.3f);
        callEndBtn.setAlpha(0.3f);
        nextWordLayout.setAlpha(0.3f);
        if (StaticsUtility.IS_CARE_TEAM) {
            nextWordLayout.setVisibility(View.INVISIBLE);
        }
    }

    @UiThread
    void hideReconnectCallLayout(boolean wantChange) {
        callEndBtn.setClickable(true);
        nextWordLayout.setClickable(true);

        toggleMuteBtn.setClickable(true);
        toggleSpeakerBtn.setClickable(true);
        toggleVideoBtn.setClickable(true);
        requestVoiceBtn.setClickable(true);
        if (wantChange) {
            toggleMuteBtn.setChecked(false);
            toggleSpeakerBtn.setChecked(false);
        }

        requestVoiceBtn.setAlpha(1.0f);
        callEndBtn.setAlpha(1.0f);
        nextWordLayout.setAlpha(1.0f);

        if (StaticsUtility.IS_CARE_TEAM) {
            nextWordLayout.setVisibility(View.INVISIBLE);
        }
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
        if (StaticsUtility.IS_CARE_TEAM) {
            finish();
        } else if (isHost) {

        } else {
            finish();
        }
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
        if (StaticsUtility.IS_CARE_TEAM) {
            finish();
        } else if (isHost) {

        } else {
            finish();
        }
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


    //region Internal - 영상통화 출력부 활성/비활성 설정 메소드
    @UiThread(propagation = UiThread.Propagation.REUSE)
    void internalVideoScreenEnable() {
        glviewMyCamera.removeAllViews();
        glview.removeAllViews();
        previousAudioOutSpeaker = toggleSpeakerBtn.isChecked();

        mSubscriber.setSubscribeToVideo(true);
        mPublisher.setPublishVideo(true);

        glviewLayout.setVisibility(View.VISIBLE);
        glviewMyCamera.setVisibility(View.VISIBLE);
        glview.setVisibility(View.VISIBLE);

        voiceItemLayout.setVisibility(View.INVISIBLE);
        voiceCallLayout.setVisibility(View.INVISIBLE);
        voiceTopicLayout.setVisibility(View.INVISIBLE);
        videoItemLayout.setVisibility(View.VISIBLE);
        videoCallLayout.setVisibility(View.VISIBLE);

        glviewMyCamera.setBackgroundResource(R.color.transparent);
        glview.setBackgroundResource(R.color.transparent);

        glviewMyCamera.addView(mPublisher.getView());
        if (mPublisher.getView() instanceof GLSurfaceView) {
            ((GLSurfaceView) mPublisher.getView()).setZOrderOnTop(true);
        }
        ViewCompat.setTranslationZ(videoItemLayout, 1);

        glview.addView(mSubscriber.getView());

        if (isHeadSetOn || isBluetoothOn) {
            toggleSpeakerBtn.setChecked(false);
        } else {
            toggleSpeakerBtn.setChecked(true);
        }
    }

    @UiThread(propagation = UiThread.Propagation.REUSE)
    void internalVideoScreenDisable() {
        if (glview != null) {
            glview.setBackgroundResource(R.color.white);
            glview.setVisibility(View.INVISIBLE);
            glview.removeAllViews();
        }
        if (glviewMyCamera != null) {
            glviewMyCamera.setBackgroundResource(R.color.white);
            glviewMyCamera.setVisibility(View.INVISIBLE);
            glviewMyCamera.removeAllViews();
        }

        if (mSubscriber != null)
            mSubscriber.setSubscribeToVideo(false);
        if (mPublisher != null)
            mPublisher.setPublishVideo(false);
        if (glviewLayout != null) {
            glviewLayout.setVisibility(View.INVISIBLE);
        }

        if (isHeadSetOn || isBluetoothOn) {
            toggleSpeakerBtn.setChecked(false);
        } else {
            toggleSpeakerBtn.setChecked(previousAudioOutSpeaker);
        }

        voiceItemLayout.setVisibility(View.VISIBLE);
        voiceCallLayout.setVisibility(View.VISIBLE);
        voiceTopicLayout.setVisibility(View.VISIBLE);
        videoItemLayout.setVisibility(View.INVISIBLE);
        videoCallLayout.setVisibility(View.INVISIBLE);
    }
    //endregion

    //region Internal - 학습 단어 표출 업데이트 메소드
    @UiThread(propagation = UiThread.Propagation.REUSE)
    void internalUpdateLessonWord(String word) {
        passedWord = word;
        imm.hideSoftInputFromWindow(voiceMinorTopicText.getWindowToken(), 0);
        voiceMinorTopicText.setText(word);
        voiceMinorTopicText.setVisibility(View.VISIBLE);
        nextWordLayout.setVisibility(View.INVISIBLE);
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

            internalVideoScreenDisable();
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

            internalVideoScreenDisable();
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


    //통화 로그를 남기기위한 메소드 (부재중)
    void addCallLog(ContentResolver resolver) {
        ContentValues values = new ContentValues();
        values.put(CallLog.Calls.NUMBER, "NurseTree");
        values.put(CallLog.Calls.TYPE, 3); // 1:수신, 2:발신, 3:부재중전화
        values.put(CallLog.Calls.DATE, System.currentTimeMillis()); // 시간
        values.put(CallLog.Calls.DURATION, 0); // 통화시간
        values.put(CallLog.Calls.NEW, 1);
        resolver.insert(Uri.parse("content://call_log/calls"), values);
    }


    private void setIntegerArrayPref(Context context) {

        Gson gson = new Gson();

        List<Integer> mSelectedList = interestIntegerList;
        String jsonString = gson.toJson(mSelectedList);
        SharedPreferences sp = context.getSharedPreferences("interestList", Context.MODE_PRIVATE);

        //Save to SharedPreferences
        sp.edit().putString("interestList", jsonString).apply();

    }

    private ArrayList<Integer> getIntegerArrayPref(Context context) {
        Gson gson = new Gson();

        String empty_list = gson.toJson(interestIntegerList);

        SharedPreferences sp = context.getSharedPreferences("interestList", Context.MODE_PRIVATE);

        return gson.fromJson(sp.getString("interestList", empty_list),
                new TypeToken<ArrayList<Integer>>() {
                }.getType());
    }
}