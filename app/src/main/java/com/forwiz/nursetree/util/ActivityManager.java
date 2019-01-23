package com.forwiz.nursetree.util;

/**
 * Created by forwiz8020 on 2016-10-06.
 */


import android.app.Activity;

import java.util.ArrayList;

public class ActivityManager {

    private static ActivityManager activityMananger = null;
    private ArrayList<Activity> activityList;

    private ActivityManager() {
        activityList = new ArrayList<>();
    }

    public static ActivityManager getInstance() {

        if( ActivityManager.activityMananger == null ) {
            activityMananger = new ActivityManager();
        }
        return activityMananger;
    }

    /**
     * 액티비티 리스트에 추가.
     * @param activity
     */
    public void addActivity(Activity activity) {
        activityList.add(activity);
    }

    /**
     * 액티비티 리스트에서 삭제.
     * @param activity
     * @return boolean
     */
    public boolean removeActivity(Activity activity) {
        return activityList.remove(activity);
    }

    /**
     * 모든 액티비티 종료.
     */
    public void finishAllActivity() {
        for (Activity activity : activityList) {
            activity.finish();
        }
    }
}
