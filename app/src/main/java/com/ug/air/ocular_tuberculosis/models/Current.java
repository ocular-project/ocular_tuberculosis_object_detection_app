package com.ug.air.ocular_tuberculosis.models;

public class Current {

    String model_type, model_file, download, filename, model_name, access_url, disease, model_reference;
    int version;

    public Current(String model_type, String model_file, String download, String filename, String model_name, String access_url, String disease, String model_reference, int version) {
        this.model_type = model_type;
        this.model_file = model_file;
        this.download = download;
        this.filename = filename;
        this.model_name = model_name;
        this.access_url = access_url;
        this.disease = disease;
        this.model_reference = model_reference;
        this.version = version;
    }

    public String getModel_type() {
        return model_type;
    }

    public Current setModel_type(String model_type) {
        this.model_type = model_type;
        return this;
    }

    public String getModel_file() {
        return model_file;
    }

    public Current setModel_file(String model_file) {
        this.model_file = model_file;
        return this;
    }

    public String getDownload() {
        return download;
    }

    public Current setDownload(String download) {
        this.download = download;
        return this;
    }

    public String getFilename() {
        return filename;
    }

    public Current setFilename(String filename) {
        this.filename = filename;
        return this;
    }

    public String getModel_name() {
        return model_name;
    }

    public Current setModel_name(String model_name) {
        this.model_name = model_name;
        return this;
    }

    public String getAccess_url() {
        return access_url;
    }

    public Current setAccess_url(String access_url) {
        this.access_url = access_url;
        return this;
    }

    public String getDisease() {
        return disease;
    }

    public Current setDisease(String disease) {
        this.disease = disease;
        return this;
    }

    public int getVersion() {
        return version;
    }

    public Current setVersion(int version) {
        this.version = version;
        return this;
    }

    public String getModel_reference() {
        return model_reference;
    }

    public Current setModel_reference(String model_reference) {
        this.model_reference = model_reference;
        return this;
    }
}
