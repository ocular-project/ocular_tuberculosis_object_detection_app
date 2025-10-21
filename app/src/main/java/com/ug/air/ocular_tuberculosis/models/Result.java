package com.ug.air.ocular_tuberculosis.models;

import java.util.ArrayList;

public class Result {

    boolean dataUpdated;
    ArrayList<Current> currentArrayList;

    public Result() {
    }

    public Result(boolean dataUpdated, ArrayList<Current> currentArrayList) {
        this.dataUpdated = dataUpdated;
        this.currentArrayList = currentArrayList;
    }

    public boolean isDataUpdated() {
        return dataUpdated;
    }

    public ArrayList<Current> getCurrentArrayList() {
        return currentArrayList;
    }
}
