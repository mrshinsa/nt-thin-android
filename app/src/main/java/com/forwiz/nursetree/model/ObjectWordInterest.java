package com.forwiz.nursetree.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by YunHo on 2018-08-13.
 */

public class ObjectWordInterest {
    String createDate;
    String group;
    String iconId;
    String iconUrl;
    int index;
    int interestSeq;
    boolean internal;
    String title;

    public ObjectWordInterest(JSONObject jsonObject) {
        try {
            setCreateDate(jsonObject.getString("createDate"));
            setGroup(jsonObject.getString("group"));
            setIconId(jsonObject.getString("iconId"));
            setIconUrl(jsonObject.getString("iconUrl"));
            setIndex(jsonObject.getInt("index"));
            setInterestSeq(jsonObject.getInt("interestSeq"));
            setInternal(jsonObject.getBoolean("internal"));
            setTitle(jsonObject.getString("title"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
    public ObjectWordInterest(){
         createDate = "";
         group = "";
         iconId = "0";
         iconUrl = "";
         index = 0;
         interestSeq = 0;
         internal = false;
         title = "Free Talk";
    }

    public String getCreateDate() {
        return createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getIconId() {
        return iconId;
    }

    public void setIconId(String iconId) {
        this.iconId = iconId;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getInterestSeq() {
        return interestSeq;
    }

    public void setInterestSeq(int interestSeq) {
        this.interestSeq = interestSeq;
    }

    public boolean isInternal() {
        return internal;
    }

    public void setInternal(boolean internal) {
        this.internal = internal;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
