package com.forwiz.nursetree.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by YunHo on 2017-09-18.
 */

public class WordCategory {
    int categorySeq;
    String createDate;
    String iconId;
    String iconUrl;
    int index;
    String title;



    public WordCategory(JSONObject jsonObject) {
        try {
            setCategorySeq(jsonObject.getInt("categorySeq"));
            setCreateDate(jsonObject.getString("createDate"));
            setIconId(jsonObject.getString("iconId"));
            setIconUrl(jsonObject.getString("iconUrl"));
            setIndex(jsonObject.getInt("index"));
            setTitle(jsonObject.getString("title"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    public int getCategorySeq() {
        return categorySeq;
    }

    public void setCategorySeq(int categorySeq) {
        this.categorySeq = categorySeq;
    }

    public String getCreateDate() {
        return createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
