package com.forwiz.nursetree.common;

import android.content.SharedPreferences;

import com.forwiz.nursetree.ApplicationClass;
import com.forwiz.nursetree.OpenTokConfig;
import com.forwiz.nursetree.R;
import com.forwiz.nursetree.statistics.UserInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.RequestBody;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by gcarrot on 2017. 4. 20..
 */

public class ServerApiManager {

    private static final String serverUrl =  "https://api-test.nursetree.net";
    /**
     * Server Api Type
     */
    public enum ServerApi {
        AUTH_B64_DECODE,
        AUTH_B64_ENCODE,
        DEVICE_UPDATE,
        JOIN_VALID_EMAIL,
        JOIN_VALID_REFERRER,
        AUTH_JOIN,
        TOKEN_REQUEST,
        TOKEN_VALID,
        LIFECYCLE_STARTUP,
        GOAL,
        GOAL_MODIFY,
        DIAGNOSTIC_PUSH,
        DIAGNOSTIC_TESTCALL,
        MATCH,
        MATCH_ACCEPT,
        MATCH_REQUEST,
        MATCH_CANCEL,
        MATCH_RECEIVED,
        RECOVERY_REQUEST,
        SESSION_CURRENT,
        SESSION_HISTORY,
        SESSION_RATING,
        SESSION_REPORT,
        SESSION_TRANSIT,
        TOPIC_CATEGORY,
        TOPIC_CATEGORY_CATEGORYSEQ,
        TOPIC_FORWARD,
        WORD,
        WORD_INTEREST,
        WORD_INTEREST_PRIORITY,
        WORD_LEVEL_JUDGEMENT,
        WORD_LTM_AVAILABLE,
        WORD_LTM_FORWARD,
        WORD_LTM_MARK,
        TAB,
        TAB_ADD,
        TAB_MODIFY,
        RESERVATION,
        RESERVATION_TIMESTATUS_LEARNER,
        RESERVATION_TIMESTATUS_TEACHER,
        RESERVATION_REGIST_LEARNER,
        RESERVATION_REGIST_TEACHER,
        RESERVATION_CANCEL,
        USER,
        USER_UPDATE_AVAILABLE,
        USER_UPDATE_PASSWORD,
        USER_UPDATE_PUSHKEY,
        IAP_WEB_HISTORY,
        CARE_FEEDBACK,
        IAP_PLAYSTORE_PAYLOAD,
        IAP_PLAYSTORE_BUY,
        CALLCENTER,
    }

    /**
     * Server에 해당 API를 POST형식으로 요청한다. Token이 없는 경우 사용.
     *
     * @param serverApi Server에 요청할 API 종류
     * @param params    Server요청시 사용할 파라미터
     * @return OkHttp에서 사용할 Request
     */
    public static Request getRequestPostNoToken(ServerApi serverApi, HashMap<String, Object> params) {
        return getRequestPost(serverApi, params, null);
    }

    /**
     * Server에 해당 API를 POST형식으로 요청한다.
     *
     * @param serverApi Server에 요청할 API 종류
     * @param params    Server요청시 사용할 파라미터
     * @return OkHttp에서 사용할 Request
     */
    public static Request getRequestPost(ServerApi serverApi, HashMap<String, Object> params) {
        return getRequestPost(serverApi, params, OpenTokConfig.TOKEN);
    }

    /**
     * Server에 해당 API를 POST형식으로 요청한다. Token이 없는 경우 사용.
     *
     * @param serverApi Server에 요청할 API 종류
     * @return OkHttp에서 사용할 Request
     */
    public static Request getRequestPost(ServerApi serverApi) {
        if(OpenTokConfig.TOKEN.isEmpty()){
            SharedPreferences tokenPrefs = ApplicationClass.getAppContext().getSharedPreferences("token", MODE_PRIVATE);
            OpenTokConfig.TOKEN = tokenPrefs.getString("preferToken", "");
        }
        return getRequestPost(serverApi, null, OpenTokConfig.TOKEN);
    }

    /**
     * Server에 해당 API를 GET형식으로 요청한다. Token이 없는 경우 사용.
     *
     * @param serverApi Server에 요청할 API 종류
     * @param params    Server요청시 사용할 파라미터
     * @return OkHttp에서 사용할 Request
     */
    public static Request getRequestGet(ServerApi serverApi, HashMap<String, Object> params) {
        String urlString = getUrlStringWithServerApi(serverApi);
        HttpUrl.Builder urlBuilder = HttpUrl.parse(urlString).newBuilder();
        if (params != null) {
            for (HashMap.Entry<String, Object> param : params.entrySet()) {
                urlBuilder.addQueryParameter(param.getKey(), (String) param.getValue());
            }
        }

        return new Request.Builder()
                .url(urlBuilder.build())
                .header("Authorization", OpenTokConfig.TOKEN)
                .get()
                .build();
    }

    /**
     * Server에 해당 API를 GET형식으로 요청한다. Token이 없는 경우 사용.
     *
     * @param serverApi Server에 요청할 API 종류
     * @return OkHttp에서 사용할 Request
     */
    public static Request getRequestGet(ServerApi serverApi) {
        return getRequestGet(serverApi, null);
    }


    public static Request getRequestGetAddAddress(ServerApi serverApi, String addAddress, HashMap<String, Object> params) {


        String urlString = getUrlStringWithServerApi(serverApi, addAddress);

        HttpUrl.Builder urlBuilder = HttpUrl.parse(urlString).newBuilder();

        if (params != null) {
            for (HashMap.Entry<String, Object> param : params.entrySet()) {
                urlBuilder.addQueryParameter(param.getKey(), (String) param.getValue());
            }
        }
        return new Request.Builder()
                .url(urlBuilder.build())
                .header("Authorization", OpenTokConfig.TOKEN)
                .get()
                .build();
    }


    private static Request getRequestPost(ServerApi serverApi, HashMap<String, Object> params, String token) {
        String urlString = getUrlStringWithServerApi(serverApi);
        RequestBody body = getRequestBody(params);

        Request.Builder requestBuilder = new Request.Builder();
        if (token != null) {
            requestBuilder.header("Authorization", OpenTokConfig.TOKEN);
        }

        return requestBuilder
                .url(urlString)
                .post(body)
                .build();
    }

    private static RequestBody getRequestBody(HashMap<String, Object> params) {
        FormBody.Builder formBody = new FormBody.Builder();
        if (params != null) {
            for (Map.Entry<String, Object> param : params.entrySet()) {
                String key = param.getKey();
                Object value = param.getValue();
                if (value instanceof ArrayList) {
                    ArrayList<String> arrayParams = (ArrayList<String>) value;
                    for (int i = 0; i < arrayParams.size(); i++) {
                        String arrayParam = arrayParams.get(i);
                        String arrayParamKey = key + "[]";
                        formBody.add(arrayParamKey, arrayParam);
                    }
                } else {
                    formBody.add(key, (String) value.toString());
                }
            }
        }
        RequestBody body = formBody.build();
        return body;
    }

    private static String getUrlStringWithServerApi(ServerApi serverApi) {
        return serverUrl + getStringWithServerApi(serverApi);
    }

    private static String getUrlStringWithServerApi(ServerApi serverApi, String addAddress) {
        return serverUrl + getStringWithServerApi(serverApi) + "/" + addAddress;
    }

    private static String getStringWithServerApi(ServerApi serverApi) {
        switch (serverApi) {
            case AUTH_B64_DECODE:
                return "/api/auth/b64/decode";
            case AUTH_B64_ENCODE:
                return "/api/auth/b64/encode";
            case DEVICE_UPDATE:
                return "/api/device/update";
            case JOIN_VALID_EMAIL:
                return "/api/join/valid/email";
            case JOIN_VALID_REFERRER:
                return "/api/join/valid/referrer";
            case AUTH_JOIN:
                return "/api/join/";
            case TOKEN_VALID:
                return "/api/token/valid";
            case TOKEN_REQUEST:
                return "/api/token/request";
            case LIFECYCLE_STARTUP:
                return "/api/lifecycle/startup";
            case GOAL:
                return "/api/goal/";
            case GOAL_MODIFY:
                return "/api/goal/modify";
            case DIAGNOSTIC_PUSH:
                return "/api/diagnostic/push";
            case DIAGNOSTIC_TESTCALL:
                return "/api/diagnostic/testcall";
            case MATCH:
                return "/api/match/";
            case MATCH_ACCEPT:
                return "/api/match/accept";
            case MATCH_REQUEST:
                return "/api/match/request";
            case MATCH_CANCEL:
                return "/api/match/cancel";
            case MATCH_RECEIVED:
                return "/api/match/received";
            case RECOVERY_REQUEST:
                return "/cs/recovery/request";
            case SESSION_CURRENT:
                return "/api/session/current";
            case SESSION_HISTORY:
                return "/api/session/history";
            case SESSION_RATING:
                return "/api/session/rating/";
            case SESSION_REPORT:
                return "/api/session/report/";
            case SESSION_TRANSIT:
                return "/api/session/transit/";
            case TOPIC_CATEGORY:
                return "/api/topic/category";
            case TOPIC_CATEGORY_CATEGORYSEQ:
                return "/api/topic/category";
            case TOPIC_FORWARD:
                return "/api/topic/forward";
            case WORD:
                return "/api/word/";
            case WORD_INTEREST:
                return "/api/word/interest";
            case WORD_INTEREST_PRIORITY:
                return "/api/word/interest/priority";
            case WORD_LEVEL_JUDGEMENT:
                return "/api/word/level/judgement";
            case WORD_LTM_AVAILABLE:
                return "/api/word/ltm/available";
            case WORD_LTM_FORWARD:
                return "/api/word/ltm/forward";
            case WORD_LTM_MARK:
                return "/api/word/ltm/mark";
            case TAB:
                return "/api/tab/";
            case TAB_ADD:
                return "/api/tab/add";
            case TAB_MODIFY:
                return "/api/tab/modify";
            case RESERVATION:
                return "/api/reservation/";
            case RESERVATION_TIMESTATUS_LEARNER:
                return "/api/reservation/timestatus/learner";
            case RESERVATION_TIMESTATUS_TEACHER:
                return "/api/reservation/timestatus/teacher";
            case RESERVATION_REGIST_LEARNER:
                return "/api/reservation/regist/learner";
            case RESERVATION_REGIST_TEACHER:
                return "/api/reservation/regist/teacher";
            case RESERVATION_CANCEL:
                return "/api/reservation/cancel";
            case USER:
                return "/api/user/";
            case USER_UPDATE_AVAILABLE:
                return "/api/user/update/available";
            case USER_UPDATE_PASSWORD:
                return "/api/user/update/password";
            case USER_UPDATE_PUSHKEY:
                return "/api/user/update/pushkey";
            case IAP_WEB_HISTORY:
                return "/api/iap/web/history";
            case CARE_FEEDBACK:
                return "/api/care/feedback";
            case IAP_PLAYSTORE_PAYLOAD:
                return "/api/iap/playstore/payload";
            case IAP_PLAYSTORE_BUY:
                return "/api/iap/playstore/buy";
            case CALLCENTER:
                return "/api/callcenter/";
            default:
                return null;
        }
    }
}
