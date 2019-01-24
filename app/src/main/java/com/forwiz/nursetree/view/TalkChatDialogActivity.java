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
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.provider.CallLog;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.TextViewCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.forwiz.nursetree.R;
import com.forwiz.nursetree.common.ServerApiManager;
import com.forwiz.nursetree.statistics.StaticsUtility;
import com.forwiz.nursetree.statistics.UserInfo;
import com.forwiz.nursetree.util.ActivityManager;
import com.forwiz.nursetree.util.NTAlertDialog;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.res.StringRes;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by YunHo on 2017-03-29.
 */
@EActivity(R.layout.include_none_background)
public class TalkChatDialogActivity extends BaseAppCompatActivity {

    @StringRes(R.string.talkchat_dialog_content)
    String TALKTALK_DIALOG_CONTENT;

    @ViewById(R.id.talk_accept_layout)
    LinearLayout talkAcceptButton;

    @ViewById(R.id.talk_decline_layout)
    LinearLayout talkDeclineButton;

    @ViewById(R.id.my_user_type_text)
    TextView myUserTypeText;

    @ViewById(R.id.call_major_topic_text)
    TextView categoryTextView;
    @ViewById(R.id.call_topic_icon)
    ImageView categoryImageView;

    @Extra
    boolean isTestCall = false;

    @Extra
    String topicSeq;

    @Extra
    String topicTitle;

    @Extra
    String categorySeq;

    @Extra
    String categoryTitle;

    @Extra
    String categoryIconId;

    @Extra
    String tboxToken;

    @Extra
    String tboxApiKey;

    @Extra
    String pushUid;

    @Extra
    Date expEndDate;

    @Extra
    boolean needPaidCall;

    @ViewById(R.id.call_minor_topic_textview)
    TextView callMinorTopicText;

    int[] callIconList = {
            R.drawable.icon_voicecall_0_introdution, R.drawable.icon_voicecall_4_music_art, R.drawable.icon_voicecall_5_movies,
            R.drawable.icon_voicecall_9_work, R.drawable.icon_voicecall_11_business_english, R.drawable.icon_voicecall_8_dail_conversation,
            R.drawable.icon_voicecall_10_school, R.drawable.icon_voicecall_3_fashion, R.drawable.icon_voicecall_6_shopping,
            R.drawable.icon_voicecall_1_sports, R.drawable.icon_voicecall_2_travel, R.drawable.icon_voicecall_15_economy,
            R.drawable.icon_voicecall_14_society, R.drawable.icon_voicecall_7_food, R.drawable.icon_voicecall_13_health,
            R.drawable.icon_voicecall_16_nature, R.drawable.icon_voicecall_12_technology
    };

    Activity activity;
    Context context;

    boolean isLoading;
    Ringtone ringtone;
    Vibrator vibrator;

    boolean isEntered;

    ActivityManager am = ActivityManager.getInstance();

    @AfterViews
    void afterViews() {
        isLoading = false;
        context = this;
        activity = this;
        isEntered = true;

        if(topicTitle.equals("Test")){
            topicTitle = getString(R.string.nt_self_test_complete_title);
        }
        else if(StaticsUtility.IS_CARE_TEAM){
            topicTitle = "Care Center";
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        Log.d("talkchatDialog", "vibrator Start");
        vibrator = (Vibrator) getSystemService(context.VIBRATOR_SERVICE);
        long[] pattern = {0, 1000, 200, 1000, 200};
        if (vibrator != null)
            vibrator.vibrate(pattern, 1);

        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(callMinorTopicText, 12, 32, 2, TypedValue.COMPLEX_UNIT_DIP);
        updateButton();

        if(StaticsUtility.IS_CARE_TEAM){
//            categoryTitle = "This is Admin, We need your feedback";
            categoryTextView.setText(categoryTitle);
            categoryImageView.setImageResource(R.drawable.icon_voicecall_0_introdution);
            callMinorTopicText.setText(topicTitle);
        }
        else if(StaticsUtility.NEED_INTEREST_TEST)
        {
            categoryTextView.setText("Learning Interests");
            categoryImageView.setImageResource(R.drawable.icon_ai_interest);
            callMinorTopicText.setText("AI Learning");
        }
        else {
            int categoryIconIdx = 0;
            categoryTextView.setText((categoryTitle != null)? categoryTitle : "");
            if(categoryIconId != null && categoryIconId.startsWith("icon_")){
                categoryIconId = categoryIconId.replaceAll("icon_", "");
                
                try {
                    categoryIconIdx = Integer.parseInt(categoryIconId);
                } catch (Exception e) {
                    e.printStackTrace();
                    categoryIconIdx = 0;
                }
            }
            else{
                if(categoryIconId != null && categoryIconId.equals("")){
                    categoryIconIdx = 0;
                }
                else if(categoryIconId != null && categoryIconId.startsWith("unde")){
                    categoryIconIdx = 0;
                }
                else {
                    categoryIconIdx = Integer.parseInt(categoryIconId);
                }
            }
            categoryImageView.setImageResource(callIconList[categoryIconIdx]);
            callMinorTopicText.setText(topicTitle);
            if(topicTitle.equals("")){
                callMinorTopicText.setText(R.string.nt_common_freetalk_text);
            }
        }
        if (UserInfo.myUserType.equals("callcenter")) {
            myUserTypeText.setText(R.string.common_nursetree_student_text);
        } else {
            myUserTypeText.setText(R.string.common_nursetree_teacher_text);
        }

        am.addActivity(this);
        LocalBroadcastManager.getInstance(context).registerReceiver(mCancelReceiver,
                new IntentFilter("cancel"));
        if (pushUid != null && !pushUid.isEmpty())
            successEnterRingActivity();
    }

    @Background
    void successEnterRingActivity() {
        HashMap<String, Object> params = new HashMap<>();
        params.put("push_uid", (pushUid != null) ? pushUid : "null");
        params.put("result", "ringing");

        Request request = ServerApiManager.getRequestGet(ServerApiManager.ServerApi.MATCH_RECEIVED, params);
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
                        Log.d("enterDialog", "Success Enter Dialog Activity of pushUid: " + pushUid);
                    } else {
                        //none_user status 일 때
                        Log.d("enterDialog", "Fail Enter Dialog Activity of pushUid: " + pushUid + ", reason: " + result);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Background
    void successFinishRingActivity() {
        HashMap<String, Object> params = new HashMap<>();
        params.put("push_uid", (pushUid != null) ? pushUid : "null");
        params.put("result", "finished");

        Request request = ServerApiManager.getRequestGet(ServerApiManager.ServerApi.MATCH_RECEIVED, params);
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
                        Log.d("finishDialog", "Success Finish Dialog Activity of pushUid: " + pushUid);
                    } else {
                        //none_user status 일 때
                        Log.d("finishDialog", "Fail Finish Dialog Activity of pushUid: " + pushUid + ", reason: " + result);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private BroadcastReceiver mCancelReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context rcvContext, Intent intent) {
            //alert data here
            isLoading = true;
            updateButton();

            am.removeActivity(activity);
            finish();
        }
    };

    @Click(R.id.talk_accept_button)
    void acceptButtonAction() {
        isLoading = true;
        //통화 수락하는 동시에 다음통화 요청이 들어와서 버튼누르는순간 푸시를 못받는 상태로 수정
        UserInfo.myPushState = true;
        updateButton();

        StaticsUtility.CALLER_ID = UserInfo.myToken;
        if (topicTitle.equals("Free Talk"))
            topicTitle = "";

        if (isTestCall) {
            UserInfo.myPushState = false;
            setResult(RESULT_OK);
            finish();
        } else {
            acceptChat(StaticsUtility.INBOUND);
        }

    }

    @Click(R.id.talk_decline_button)
    void endButtonAction() {
        isLoading = true;
        updateButton();
        StaticsUtility.ALREADY_CALL = true;
        am.removeActivity(this);
        if (isTestCall) {
            setResult(RESULT_OK);
        }
        finish();
        UserInfo.myPushState = false;

        if (!isTestCall) {
            if (!UserInfo.myUserType.equals("callcenter")) {
                cancelChat(StaticsUtility.INBOUND);
//                CallTracker.track(302);
            } else {
                cancelChat(StaticsUtility.INBOUND);
//                CallTracker.track(301);
            }
        }

    }

    void updateButton() {
        talkAcceptButton.setEnabled(!isLoading);
        talkDeclineButton.setEnabled(!isLoading);
        if (isLoading) {
            talkAcceptButton.setAlpha(0.3f);
            talkDeclineButton.setAlpha(0.3f);
        } else {
            talkAcceptButton.setAlpha(1.0f);
            talkDeclineButton.setAlpha(1.0f);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("talkchatDialog", "ringTone Start");
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        if(ringtone == null)
            ringtone = RingtoneManager.getRingtone(context, notification);

        if(!ringtone.isPlaying() && isEntered) {
            ringtone.play();
        }

        Log.d("talkchatDialog", "onResume");
        if (vibrator == null) {
            vibrator = (Vibrator) getSystemService(context.VIBRATOR_SERVICE);
        }
        long[] pattern = {0, 1000, 200, 1000, 200};
        if (vibrator != null) {
            vibrator.vibrate(pattern, 1);
        }

        acceptButtonAction();

    }

    @Override
    protected void onStop() {
        super.onStop();
//        UserInfo.myPushState = false;
        Log.d("talkchatDialog", "onStop");
        if (ringtone != null) {
            ringtone.stop();
            ringtone = null;
        }
        if (vibrator.hasVibrator()) {
            vibrator.cancel();
        }
    }

    @Override
    protected void onDestroy() {
        UserInfo.myPushState = false;
        successFinishRingActivity();
        Log.d("talkchatDialog", "onDestroy");
        if (ringtone != null) {
            ringtone.stop();
            ringtone = null;
        }

        if (vibrator != null) {
            vibrator.cancel();
        }
        super.onDestroy();
    }


    @Background
    void acceptChat(String sessionSeq2) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("session_seq", sessionSeq2);

        Request request = ServerApiManager.getRequestGet(ServerApiManager.ServerApi.MATCH_ACCEPT, params);

        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                if (isSafeToShowDialog()) {
                    showFailConnectDialog();
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    String status = jsonObject.getString("status");
                    Log.d("acceptState", status);
                    switch (status) {
                        case "success":
                            chatAcceptSuccess();
                            break;
                        case "none_permission":
                            showNonePermission();
                            break;
                        case "none_session":
                            showNoneSessionDialog();
                            break;
                        default:
                            //none_user status 일 때
//                            showNoneUserDialog();
                            chatAcceptSuccess();
                            break;
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Background
    void cancelChat(String sessionSeq2) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("session_seq", sessionSeq2);

        Request request = ServerApiManager.getRequestGet(ServerApiManager.ServerApi.MATCH_CANCEL, params);

        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                if (isSafeToShowDialog()) {
                    showFailConnectDialog();
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {

                try {
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    String status = jsonObject.getString("status");
                    Log.d("cancelState", status);
                    switch (status) {
                        case "success":
                            chatCancelSuccess();
                            break;
                        case "none_permission":
                            showNonePermission();
                            break;
                        case "none_session":
                            showNoneSessionDialog();
                            break;
                        default:
//                            showNoneUserDialog();
                            chatCancelSuccess();
                            break;
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @UiThread
    void chatCancelSuccess() {
        Log.e("chatCancel", "success Cancel!");
        finish();
    }
    @UiThread
    void chatAcceptSuccess() {
        Log.e("chatAccept", "success Accept!");
        int permissionCheck = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_CALL_LOG);
        if (permissionCheck != PackageManager.PERMISSION_DENIED) {
            changeCallLogToReceive(getBaseContext().getContentResolver());
        }
        TalkChatActivity_.intent(this).API_KEY(tboxApiKey).TBOX_TOKEN(tboxToken).flags(Intent.FLAG_ACTIVITY_SINGLE_TOP).start();
        am.removeActivity(this);
        finish();
    }


    @UiThread
    void showNonePermission() {
        if (isSafeToShowDialog()) {
            NTAlertDialog.getComfirmDialog(context, R.string.nt_permission_title, R.string.nt_none_permission_msg, (DialogInterface dialog, int which) -> {
                dialog.dismiss();
                finish();
            }).setCancelable(false).show();
        }
    }

    @UiThread
    void showNoneSessionDialog() {
        if (isSafeToShowDialog()) {
            NTAlertDialog.getComfirmDialog(context, R.string.signin_text, R.string.nt_login_none_session_msg, (DialogInterface dialog, int which) -> {
                dialog.dismiss();
                finish();
            }).setCancelable(false).show();
        }
    }

    @UiThread
    void showNoneUserDialog() {
        if (isSafeToShowDialog())
            NTAlertDialog.getComfirmDialog(context, R.string.nt_token_title, R.string.nt_token_expire_msg, (DialogInterface dialog, int which) -> {
                dialog.dismiss();
                finish();
            }).setCancelable(false).show();
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return false;
        } else if ((keyCode == KeyEvent.KEYCODE_HOME)) {
            return false;
        }
        return super.onKeyDown(keyCode, event);
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
        values.put(CallLog.Calls.DURATION, 0); // 통화시간
        values.put(CallLog.Calls.NEW, 1);
        resolver.insert(Uri.parse("content://call_log/calls"), values);
    }

    @UiThread
    void showFailConnectDialog() {
        if (!activity.isFinishing())
            NTAlertDialog.getComfirmDialog(context, R.string.signin_text, R.string.nt_check_your_server_msg, (DialogInterface dialog, int which) -> {
                dialog.dismiss();
                finish();
            }).setCancelable(false).show();
    }
}
