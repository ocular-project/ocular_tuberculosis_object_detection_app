package com.ug.air.ocular_tuberculosis.models;

import java.util.ArrayList;

public class Image {

    String slideName;
    String status;
    ArrayList<Urls> images;

    public Image(String slideName, String status, ArrayList<Urls> images) {
        this.slideName = slideName;
        this.status = status;
        this.images = images;
    }

    public String getSlideName() {
        return slideName;
    }

    public Image setSlideName(String slideName) {
        this.slideName = slideName;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public Image setStatus(String status) {
        this.status = status;
        return this;
    }

    public ArrayList<Urls> getImages() {
        return images;
    }

    public Image setImages(ArrayList<Urls> images) {
        this.images = images;
        return this;
    }
}
