package com.forwiz.nursetree.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by YunHo on 2018-08-13.
 */

public class ObjectLevelWord {
    int categorySeq = 0;
    String createDate = "";
    String description = "";
    String dictName= "";
    int interestSeq = 0;
    int level = 0;
    String title= "";
    int wordSeq = 0;

    public ObjectLevelWord(JSONObject jsonObject) {
        try {
            setCategorySeq(jsonObject.getInt("categorySeq"));
            setCreateDate(jsonObject.getString("createDate"));
            setDescription(jsonObject.getString("description"));
            setDictName(jsonObject.getString("dictName"));
            setInterestSeq(jsonObject.getInt("interestSeq"));
            setLevel(jsonObject.getInt("level"));
            setTitle(jsonObject.getString("title"));
            setWordSeq(jsonObject.getInt("wordSeq"));
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDictName() {
        return dictName;
    }

    public void setDictName(String dictName) {
        this.dictName = dictName;
    }

    public int getInterestSeq() {
        return interestSeq;
    }

    public void setInterestSeq(int interestSeq) {
        this.interestSeq = interestSeq;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getWordSeq() {
        return wordSeq;
    }

    public void setWordSeq(int wordSeq) {
        this.wordSeq = wordSeq;
    }
}
