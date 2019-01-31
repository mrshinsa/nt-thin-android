/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.forwiz.nursetree;

import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.provider.CallLog;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.forwiz.nursetree.common.ServerApiManager;
import com.forwiz.nursetree.statistics.StaticsUtility;
import com.forwiz.nursetree.statistics.UserInfo;
import com.forwiz.nursetree.util.PowerWakelock;
import com.forwiz.nursetree.view.TalkChatActivity_;
import com.forwiz.nursetree.view.TalkChatDialogActivity_;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
//import com.forwiz.nursetree.R;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    public static final String KEY_PUSH_UID = "push_uid";
    public static final String KEY_PUSH_TYPE = "push_type";

    public static final String KEY_ROOM_NUMBER = "room_number";
    public static final String KEY_CALL_TYPE = "call_type";
    public static final String KEY_CALL_SERVER = "call_server";
    public static final String KEY_CALL_HOST = "call_host";
    public static final String KEY_EXPECT_END_DATE = "exp_end_date";

    public static final String KEY_TBOX_TOKEN = "tbox_token";
    public static final String KEY_TBOX_APIKEY = "tbox_apikey";

    public static final String KEY_P2P_RELAY_ADDR = "p2p_relay_addr";
    public static final String KEY_P2P_RELAY_FORCE = "p2p_relay_force";
    public static final String KEY_CARE_TEAM = "ext_type";

    public static final String KEY_TOPIC_SEQ = "topic_seq";
    public static final String KEY_TOPIC_TITLE = "topic_title";
    public static final String KEY_TCG_SEQ = "topic_ct_seq";
    public static final String KEY_TCG_TITLE = "topic_ct_title";
    public static final String KEY_TCG_ICON = "topic_ct_iconid";
    public static final String KEY_NEED_SETUP = "need_setup";
    public static final String KEY_NEED_PAID = "need_paid";

    public static final String KEY_RESERV_MSG = "reservation_msg";
    public static final String KEY_RESERV_COUNT = "reservation_count";

    public static final String KEY_NOTICE_MSG = "notice_msg";

    Context context;

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages
        // are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data
        // messages are the type
        // traditionally used with GCM. Notification messages are only received here in
        // onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated
        // notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages
        // containing both notification
        // and data payloads are treated as notification messages. The Firebase console always
        // sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            if (/* Check if data needs to be processed by long running job */ true) {
                // For long-running tasks (10 seconds or more) use Firebase Job Dispatcher.
                scheduleJob();
            } else {
                // Handle message within 10 seconds
                handleNow();
            }

        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.


        String pushUid, pushType, callType;
        String lastRoomNumber;

        Log.d(TAG, "Any firebaseMessage received");
        context = this.getBaseContext();
//        am = com.forwiz.nursetree.util.ActivityManager.getInstance();

        SharedPreferences tokenPrefs = getSharedPreferences("token", MODE_PRIVATE);
        OpenTokConfig.TOKEN = tokenPrefs.getString("preferToken", "");

        SharedPreferences userTypePrefs = getSharedPreferences("userType", MODE_PRIVATE);
        UserInfo.myUserType = userTypePrefs.getString("preferUserType", "");

        SharedPreferences lastRoomNumberPrefs = getSharedPreferences("lastRoomNumber", MODE_PRIVATE);
        lastRoomNumber = lastRoomNumberPrefs.getString("roomNumber", "");

        // User Token 미검출시 로직처리 종료
        if (TextUtils.isEmpty(OpenTokConfig.TOKEN)) {
            Log.w(TAG, "[onMessageReceived] Cannot found User-Token, Processing will terminated");
//            CallTracker.track(999);
//            return;
        }
        if (UserInfo.myUserType.isEmpty()) {
            UserInfo.myUserType = "normal";
        }

        // Data 가 없을 경우
        if (remoteMessage.getData() == null || remoteMessage.getData().isEmpty()) {
            Log.w(TAG, "[onMessageReceived] Cannot found Data Payload, Processing will terminated");
//            CallTracker.track(998);
            return;
        }
        //데이터가 없는 푸시라 notice는 여기까지만 타고 rerutn 된다.

        // Push 수신 통지 전송
        pushUid = remoteMessage.getData().get(KEY_PUSH_UID);

        Log.d(TAG, "firebaseMessage" + remoteMessage.getFrom() + "push type : " + remoteMessage.getData().get(KEY_PUSH_TYPE));

        // Push Notification Type
        pushType = remoteMessage.getData().get(KEY_PUSH_TYPE);
        callType = remoteMessage.getData().get(KEY_CALL_TYPE);
        if (pushType == null) {
            pushType = "";
        }

        switch (pushType) {
            case "call":
//                createChatActivity(remoteMessage, pushUid, callType, lastRoomNumber);
                createChatActivity2(remoteMessage, pushUid, callType, lastRoomNumber);
                break;
            case "cancel":
                cancelChatActivity(remoteMessage);
                break;
//            case "reservation":
////                if (remoteMessage.getNotification() != null) {
////                    String reservationMsg = getResources().getString(getResources().getIdentifier(remoteMessage.getNotification().getBodyLocalizationKey(), "string", getPackageName()));
////                    String reservationCount = remoteMessage.getNotification().getBodyLocalizationArgs()[0].toString();
////                    reservationStateDialog(reservationMsg, reservationCount);
////                }
////                break;

            //notice 타입 푸시 수신시 푸시 notification 을 띄워줌 (TODO)
//            case "notice":
//                if (remoteMessage.getNotification() != null) {
//                    String noticeMsg = remoteMessage.getNotification().getBody().toString();
//                    if (isAppOnForeground(context)) {
//                        noticeDialog(noticeMsg);
//                    } else {
//                        sendNotification(noticeMsg);
//                    }
//                }
//                break;
//            case "plan":
//                if (remoteMessage.getNotification() != null) {
//                    String noticeMsg = remoteMessage.getNotification().getBody().toString();
//                    if (isAppOnForeground(context)) {
//                        planDialog(noticeMsg);
//                    } else {
//                        sendNotification(noticeMsg);
//                    }
//                }
//                break;
        }

    }

    private void createChatActivity2(RemoteMessage remoteMessage, String pushUid, String callType, String lastRoomNumber) {

        OpenTokConfig.API_KEY = remoteMessage.getData().get(KEY_TBOX_APIKEY);
        OpenTokConfig.SESSION_ID = remoteMessage.getData().get(KEY_ROOM_NUMBER);
        OpenTokConfig.TOKEN = remoteMessage.getData().get(KEY_TBOX_TOKEN);


        getSharedPreferences("token", MODE_PRIVATE).edit().putString("preferToken", OpenTokConfig.TOKEN).commit();



        Intent intent = new Intent(getBaseContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);


    }
    // [END receive_message]


    // [START on_new_token]

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        sendRegistrationToServer(token);
    }
    // [END on_new_token]

    /**
     * Schedule a job using FirebaseJobDispatcher.
     */
    private void scheduleJob() {
        // [START dispatch_job]
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(this));
        Job myJob = dispatcher.newJobBuilder()
                .setService(MyJobService.class)
                .setTag("my-job-tag")
                .build();
        dispatcher.schedule(myJob);
        // [END dispatch_job]
    }

    /**
     * Handle time allotted to BroadcastReceivers.
     */
    private void handleNow() {
        Log.d(TAG, "Short lived task is done.");
    }

    /**
     * Persist token to third-party servers.
     * <p>
     * Modify this method to associate the user's FCM InstanceID token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token) {
        // TODO: Implement this method to send token to your app server.
    }


    private void sendPushReceivedTracking(final String pushUid) {
        Log.d(TAG, "[sendPushReceivedTracking] Start send tracking of pushUid: " + pushUid);

        HashMap<String, Object> params = new HashMap<>();
        params.put("push_uid", (pushUid != null) ? pushUid : "null");
        params.put("result", "success");

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
                        Log.d(TAG, "[sendPushReceivedTracking] Success send tracking of pushUid: " + pushUid);
                    } else {
                        //none_user status 일 때
                        Log.d(TAG, "[sendPushReceivedTracking] Failure send tracking of pushUid: " + pushUid + ", reason: " + result);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void createChatActivity(RemoteMessage remoteMessage, String pushUid, String callType, String lastRoomNumber) {
        if (!callType.equals("tokbox")) {
            return;
        }

        if (!TextUtils.isEmpty(pushUid)) {
            sendPushReceivedTracking(pushUid);
        }

        if (!UserInfo.myPushState) {
            Date expEndDate = null;
            String expEndDateString = remoteMessage.getData().get(KEY_EXPECT_END_DATE);
            if (expEndDateString != null && !expEndDateString.isEmpty()) {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                try {
                    expEndDate = format.parse(expEndDateString);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                if (expEndDate == null) {
                    format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
                    try {
                        expEndDate = format.parse(expEndDateString);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
            String roomNumber = remoteMessage.getData().get(KEY_ROOM_NUMBER);
            String callServer = remoteMessage.getData().get(KEY_CALL_SERVER);
            String callHost = remoteMessage.getData().get(KEY_CALL_HOST);
            String tboxToken = remoteMessage.getData().get(KEY_TBOX_TOKEN);
            String tboxApiKey = remoteMessage.getData().get(KEY_TBOX_APIKEY);

            String topicSeq = remoteMessage.getData().get(KEY_TOPIC_SEQ);
            String topicTitle = remoteMessage.getData().get(KEY_TOPIC_TITLE);
            String categorySeq = remoteMessage.getData().get(KEY_TCG_SEQ);
            String categoryTitle = remoteMessage.getData().get(KEY_TCG_TITLE);
            String categoryIconId = remoteMessage.getData().get(KEY_TCG_ICON);
            if (categoryIconId.startsWith("icon_")) {
                categoryIconId = categoryIconId.replaceAll("icon_", "");
            } else {
                categoryIconId = "0";
            }
            boolean isFirstInterestCall = Boolean.parseBoolean(remoteMessage.getData().get(KEY_NEED_SETUP));
            boolean needPaidCall = false;
            if (remoteMessage.getData().get(KEY_NEED_PAID) != null) {
                needPaidCall = Boolean.parseBoolean(remoteMessage.getData().get(KEY_NEED_PAID));
            }

//            CallTracker.updateSessionId(roomNumber);

            if (!UserInfo.myUserType.isEmpty()) {
                if (UserInfo.myUserType.equals("callcenter")) {
//                    CallTracker.track(104);
                } else {
//                    CallTracker.track(108);
                }
            }

            int permissionCheck = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_CALL_LOG);
            if (permissionCheck != PackageManager.PERMISSION_DENIED) {
                addCallLog(getBaseContext().getContentResolver());
            }

            // P2P Relay - 이상현
            String p2pRelayAddr = remoteMessage.getData().get(KEY_P2P_RELAY_ADDR);
            String p2pRelayForce = remoteMessage.getData().get(KEY_P2P_RELAY_FORCE);


            String extType = remoteMessage.getData().get(KEY_CARE_TEAM);

            StaticsUtility.CALL_SERVER = callServer;
            StaticsUtility.ISHOST = Boolean.parseBoolean(callHost);
            StaticsUtility.INBOUND = roomNumber;

            StaticsUtility.P2P_RELAY = (p2pRelayAddr != null) ? p2pRelayAddr : "";
            StaticsUtility.IS_P2P_RELAY_FORCE = (p2pRelayForce != null && Boolean.parseBoolean(p2pRelayForce));

            if (extType != null && !extType.equals("none")) {
                StaticsUtility.IS_CARE_TEAM = true;
            } else {
                StaticsUtility.IS_CARE_TEAM = false;
            }

            StaticsUtility.CALLER_ID = OpenTokConfig.TOKEN;
            StaticsUtility.NEED_INTEREST_TEST = isFirstInterestCall;

            ActivityManager actMng = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
            if (actMng != null) {
                List<ActivityManager.RunningAppProcessInfo> list = actMng.getRunningAppProcesses();
                String packageName = "";

                if (list != null) {
                    for (ActivityManager.RunningAppProcessInfo rap : list) {
                        System.out.println("packageName = " + packageName + ", importance = " + rap.importance);
                    }
                }
            }

            boolean isNewRoom = true;

            if (roomNumber.equals(lastRoomNumber)) {
                ActivityManager currentActivity = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                if (currentActivity != null) {
                    ComponentName cn = currentActivity.getRunningTasks(1).get(0).topActivity;

                    String chatDialogClassName = TalkChatDialogActivity_.class.getName();

                    if (cn.getClassName().equals(chatDialogClassName)) {
                        isNewRoom = false;
                    }
                }
            }

            if (isNewRoom) {
                startCallActivity(topicSeq, topicTitle, categorySeq, categoryTitle, categoryIconId, tboxToken, tboxApiKey, pushUid, expEndDate, needPaidCall);
            }

            SharedPreferences currentRoomNumberPrefs = getSharedPreferences("lastRoomNumber", MODE_PRIVATE);
            SharedPreferences.Editor editor = currentRoomNumberPrefs.edit();
            editor.putString("roomNumber", roomNumber);
            editor.apply();
        }
    }

    void startCallActivity(String topicSeq,
                           String topicTitle,
                           String categorySeq,
                           String categoryTitle,
                           String categoryIconId,
                           String tboxToken,
                           String tboxApiKey,
                           String pushUid,
                           Date expEndDate,
                           boolean needPaidCall) {
        PowerWakelock.acquireCpuWakeLock(this);
        if (!StaticsUtility.IS_CARE_TEAM && categorySeq.equals("0")) {
            categoryTitle = "Free Talk";
            topicTitle = "Free Talk";
            Log.d(TAG, "Free Talk");
        }

        TalkChatDialogActivity_.intent(getApplicationContext())
                .topicSeq(topicSeq)
                .topicTitle(topicTitle)
                .categorySeq(categorySeq)
                .categoryTitle(categoryTitle)
                .categoryIconId(categoryIconId)
                .tboxToken(tboxToken)
                .tboxApiKey(tboxApiKey)
                .pushUid(pushUid)
                .expEndDate(expEndDate)
                .needPaidCall(needPaidCall)
                .flags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
                        | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        | Intent.FLAG_ACTIVITY_NEW_TASK)
                .start();
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


    void cancelChatActivity(RemoteMessage remoteMessage) {
        String cancelRoomNumber = remoteMessage.getData().get(KEY_ROOM_NUMBER);
        if (!UserInfo.myUserType.isEmpty()) {
            if (UserInfo.myUserType.equals("callcenter")) {
//                CallTracker.track(159);
            } else {
//                CallTracker.track(160);
            }
        }
        ActivityManager currentAM = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        Intent intent = new Intent("cancel");

        if (currentAM != null) {
            ComponentName cn = currentAM.getRunningTasks(1).get(0).topActivity;
            String nowTopClassName = cn.getClassName();

            if (nowTopClassName.equals(TalkChatActivity_.class.getName())) {
                intent = new Intent("android.intent.action.TalkChat.cancel");
            }
            intent.putExtra("cancel_room_number", cancelRoomNumber);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

            UserInfo.myPushState = false;
        }
    }

//    void planDialog(String planMsg) {
//        Intent purchaseIntent = new Intent(context, PurchaseDialogView.class);
//        purchaseIntent.putExtra("purchase_msg", planMsg);
//        startActivity(purchaseIntent);
//    }
//
//    void noticeDialog(String noticeMsg) {
//        Intent intent = new Intent(context, NoticeDialogView.class);
//        intent.putExtra(KEY_NOTICE_MSG, noticeMsg);
//        startActivity(intent);
//    }

//    void reservationStateDialog(String reservationMsg, String reservationCount) {
//        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
//        Intent intent = new Intent("myReservation");
//
//        if (am != null) {
//            ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
//            String nowTopClassName = cn.getClassName();
//
//            if (nowTopClassName.equals(FirstSignInTABActivity_.class.getName())) {
//                intent = new Intent("android.intent.action.TeacherTab.myReservation");
//            } else if (nowTopClassName.equals(GoalTimeActivity_.class.getName())) {
//                intent = new Intent("android.intent.action.GoalTime.myReservation");
//            } else if (nowTopClassName.equals(InAppBillingActivity_.class.getName())) {
//                intent = new Intent("android.intent.action.InApp.myReservation");
//            } else if (nowTopClassName.equals(ChangePasswordActivity_.class.getName())) {
//                intent = new Intent("android.intent.action.ChangePass.myReservation");
//            }
//        }
//
//        intent.putExtra(KEY_RESERV_MSG, reservationMsg);
//        intent.putExtra(KEY_RESERV_COUNT, reservationCount);
//        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
//    }

    private boolean isAppOnForeground(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            return false;
        }
        final String packageName = context.getPackageName();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }
}
