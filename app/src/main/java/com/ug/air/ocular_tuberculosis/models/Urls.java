package com.ug.air.ocular_tuberculosis.models;

public class Urls {

    String original, analysed;
    int trop;
    int wbc;
    long inferenceTime;
    Boolean isBothCategory = false;
    Boolean isAnalysedView = false;

    public Urls(String original, String analysed, int trop, int wbc, long inferenceTime, Boolean isBothCategory, Boolean isAnalysedView) {
        this.original = original;
        this.analysed = analysed;
        this.trop = trop;
        this.wbc = wbc;
        this.isBothCategory = isBothCategory;
        this.isAnalysedView = isAnalysedView;
    }

    public String getOriginal() {
        return original;
    }

    public Urls setOriginal(String original) {
        this.original = original;
        return this;
    }

    public String getAnalysed() {
        return analysed;
    }

    public Urls setAnalysed(String analysed) {
        this.analysed = analysed;
        return this;
    }

    public int getTrop() {
        return trop;
    }

    public Urls setTrop(int trop) {
        this.trop = trop;
        return this;
    }

    public int getWbc() {
        return wbc;
    }

    public Urls setWbc(int wbc) {
        this.wbc = wbc;
        return this;
    }

    public Boolean getBothCategory() {
        return isBothCategory;
    }

    public Urls setBothCategory(Boolean bothCategory) {
        isBothCategory = bothCategory;
        return this;
    }

    public Boolean getAnalysedView() {
        return isAnalysedView;
    }

    public Urls setAnalysedView(Boolean analysedView) {
        isAnalysedView = analysedView;
        return this;
    }

    public long getInferenceTime() {
        return inferenceTime;
    }

    public Urls setInferenceTime(long inferenceTime) {
        this.inferenceTime = inferenceTime;
        return this;
    }
}
