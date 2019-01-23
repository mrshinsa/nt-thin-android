package com.forwiz.nursetree.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by YunHo on 2017-09-18.
 */

public class TopicCategoryWordObject {

    int categorySeq;
    String createDate;
    int ltmPhase;
    int seq;
    String status;
    String title;
    ObjectWordInterest interestWord;

    public TopicCategoryWordObject(JSONObject jsonObject) {
        try {
            setCategorySeq(jsonObject.getInt("categorySeq"));
            setCreateDate(jsonObject.getString("createDate"));
            setLtmPhase(jsonObject.getInt("ltmPhase"));
            setStatus(jsonObject.getString("ltmStatus"));
            setTitle(jsonObject.getString("title"));
            setSeq(jsonObject.getInt("wordSeq"));
            setInterestWord(new ObjectWordInterest(jsonObject.getJSONObject("interest")));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    public TopicCategoryWordObject(){
        categorySeq = 0;
        createDate = "";
        ltmPhase = 0;
        seq = 0;
        status = "";
        title = "";
        interestWord = new ObjectWordInterest();
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

    public int getLtmPhase() {
        return ltmPhase;
    }

    public void setLtmPhase(int ltmPhase) {
        this.ltmPhase = ltmPhase;
    }

    public int getSeq() {
        return seq;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ObjectWordInterest getInterestWord() {
        return interestWord;
    }

    public void setInterestWord(ObjectWordInterest interestWord) {
        this.interestWord = interestWord;
    }
}
