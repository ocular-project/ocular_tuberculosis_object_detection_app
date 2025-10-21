package com.ug.air.ocular_tuberculosis.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Download {

    @SerializedName("current_models")
    List<Current> currentList;

    public Download(List<Current> currentList) {
        this.currentList = currentList;
    }

    public List<Current> getCurrentList() {
        return currentList;
    }
}
