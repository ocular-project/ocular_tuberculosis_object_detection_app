package com.ug.air.ocular_tuberculosis.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ug.air.ocular_tuberculosis.R;
import com.ug.air.ocular_tuberculosis.models.Current;
import com.ug.air.ocular_tuberculosis.models.Result;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Functions {

    public static SharedPreferences sharedPreferences;
    public static SharedPreferences.Editor editor;
    public static final String MODEL = "model";
    public static final String MODELS = "models";

    public static String generate_uuid(){
        UUID uuid = UUID.randomUUID();
        String shortUUID = uuid.toString().replaceAll("-", "");
        shortUUID = shortUUID.substring(0, 7);
        shortUUID = shortUUID.toUpperCase();
        return shortUUID;
    }

    public static String get_device_name(){
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;

        String deviceName = manufacturer + " " + model;

        return deviceName;
    }

    public static AlertDialog showDialog(Context context, String message) {
        // Inflate the layout
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.progress_layout, null);

        // Find the TextView and update its text
        TextView textView = dialogView.findViewById(R.id.text);
        textView.setText(message);

        // Build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(dialogView);
        builder.setCancelable(false); // Prevent dialog from being dismissed

        AlertDialog dialog = builder.create();
        dialog.show();
        return dialog;
    }

    public static ArrayList<Current> loadModels(Context context) {
        sharedPreferences = context.getSharedPreferences(MODEL, Context.MODE_PRIVATE);

        Gson gson = new Gson();
        String json = sharedPreferences.getString(MODELS, null);
        ArrayList<Current> modelArrayList = new ArrayList<>();

        Type type = new TypeToken<ArrayList<Current>>() {}.getType();
        modelArrayList = gson.fromJson(json, type);
        if(modelArrayList == null){
            modelArrayList = new ArrayList<>();
        }
        return modelArrayList;
    }

    public static Current loadFirstModel(Context context) {
        sharedPreferences = context.getSharedPreferences(MODEL, Context.MODE_PRIVATE);

        Gson gson = new Gson();
        String json = sharedPreferences.getString(MODELS, null);

        if (json == null || json.isEmpty()) {
            return null;
        }

        Type type = new TypeToken<ArrayList<Current>>() {}.getType();
        ArrayList<Current> modelArrayList = gson.fromJson(json, type);

        if (modelArrayList != null && !modelArrayList.isEmpty()) {
            for(Current model: modelArrayList) {
                if (model.getVersion() == 1){
                    if (model.getDownload() != null && model.getDownload().isEmpty()) {
                        // download is empty
                        return model;
                    }
                }
            }
        }

        return null;
    }

    public static boolean isInternetAvailable(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) return false;

        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) return false;

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    public static Result checkData(Context context, List<Current> currentList){
        sharedPreferences = context.getSharedPreferences(MODEL, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        Gson gson = new Gson();
        String json = sharedPreferences.getString(MODELS, null);
        ArrayList<Current> currentArrayList = new ArrayList<>();

        Type type = new TypeToken<ArrayList<Current>>() {}.getType();
        currentArrayList = gson.fromJson(json, type);
        if(currentArrayList == null){
            currentArrayList = new ArrayList<>();

            for (Current current : currentList) {
                if (current.getDisease().equals("Tuberculosis")){
                    Log.d("Chodrine Ocular", "checkData: " + current);
                    current.setDownload("download");
                    current.setFilename("");
                    currentArrayList.add(current);
                }
            }

            String updatedJson = gson.toJson(currentArrayList);
            editor.putString(MODELS, updatedJson);
            editor.apply();

            return new Result(true, currentArrayList);
        }
        else {
            boolean dataUpdated = false;

            for (Current current : currentList) {
                boolean exists = false;
                if (current.getDisease().equals("Tuberculosis")){
                    for (Current existing : currentArrayList) {
                        if (Objects.equals(existing.getModel_file(), current.getModel_file())) {
                            exists = true;
                            // Check if version has changed
                            if (!Objects.equals(existing.getVersion(), current.getVersion())) {
                                existing.setVersion(current.getVersion());
                                existing.setModel_reference(current.getModel_reference());
                                existing.setAccess_url(current.getAccess_url());
                                existing.setDownload("update");
                                dataUpdated = true;
                            }
                            break;
                        }
                        Log.d("Model results 1", "checkData: " + current.getModel_name());
                    }
                    if (!exists) {
                        current.setDownload("download");
                        current.setFilename("");
                        currentArrayList.add(current);
                        dataUpdated = true;
                        Log.d("Model results 2", "checkData: " + current.getModel_name());
                    }
                }
            }

            if (dataUpdated) {
                String updatedJson = gson.toJson(currentArrayList);
                editor.putString(MODELS, updatedJson);
                editor.apply();

            }

            return new Result(dataUpdated, currentArrayList);
        }
    }

    public static ArrayList<Current> updateModel(Context context, Current model, String name) {
        sharedPreferences = context.getSharedPreferences(MODEL, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        Gson gson = new Gson();
        String json = sharedPreferences.getString(MODELS, null);
        ArrayList<Current> modelArrayList = new ArrayList<>();

        Type type = new TypeToken<ArrayList<Current>>() {}.getType();
        modelArrayList = gson.fromJson(json, type);
        if(modelArrayList == null) {
            modelArrayList = new ArrayList<>();
            Log.d("Getting model", "updateModel: nothing");
        }

        else {
            for (Current model1: modelArrayList){
                if (model1.getModel_reference().equals(model.getModel_reference())){
                    model1.setDownload("");
                    model1.setFilename(name);
                    break;
                }
            }

            String updatedJson = gson.toJson(modelArrayList);
            editor.putString(MODELS, updatedJson);
            editor.apply();

        }

        return modelArrayList;
    }

//    public static String getModelName(Context context, String model_type){
//        sharedPreferences = context.getSharedPreferences(MODEL, Context.MODE_PRIVATE);
//        editor = sharedPreferences.edit();
//
//        String name = "";
//
//        Gson gson = new Gson();
//        String json = sharedPreferences.getString(MODELS, null);
//        ArrayList<Current> modelArrayList = new ArrayList<>();
//
//        Type type = new TypeToken<ArrayList<Current>>() {}.getType();
//        modelArrayList = gson.fromJson(json, type);
//        if(modelArrayList == null) {
//            modelArrayList = new ArrayList<>();
//        }
//        else {
//            for (Current model1: modelArrayList){
//                if (model1.getCurrent_model_type().equals(model_type)){
//                    name = model1.getFilename();
//                    break;
//                }
//            }
//        }
//
//        return name;
//    }

}
