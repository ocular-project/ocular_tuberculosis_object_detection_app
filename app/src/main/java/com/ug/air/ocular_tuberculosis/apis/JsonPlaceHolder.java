package com.ug.air.ocular_tuberculosis.apis;

import com.ug.air.ocular_tuberculosis.models.Download;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface JsonPlaceHolder {

    @FormUrlEncoded
    @POST("models/all")
    Call<Download> getModels(
            @Field("username") String username
    );

}
