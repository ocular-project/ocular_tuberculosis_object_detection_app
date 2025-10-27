package com.ug.air.ocular_tuberculosis.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.downloader.Error;
import com.downloader.OnDownloadListener;
import com.downloader.PRDownloader;
import com.downloader.Status;
import com.ug.air.ocular_tuberculosis.R;
import com.ug.air.ocular_tuberculosis.adapters.MalariaAdapter;
import com.ug.air.ocular_tuberculosis.apis.ApiClient;
import com.ug.air.ocular_tuberculosis.apis.JsonPlaceHolder;
import com.ug.air.ocular_tuberculosis.databinding.ActivityModelsBinding;
import com.ug.air.ocular_tuberculosis.models.Current;
import com.ug.air.ocular_tuberculosis.models.Download;
import com.ug.air.ocular_tuberculosis.models.Result;
import com.ug.air.ocular_tuberculosis.utils.Functions;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ModelsActivity extends AppCompatActivity {

    ActivityModelsBinding binding;
    ArrayList<Current> malariaArrayList;
    MalariaAdapter malariaAdapter;
    JsonPlaceHolder jsonPlaceHolder;
    SharedPreferences sharedPreferences;
    AlertDialog dialog;
    Current model;
    String token;
    List<Current> currentsArrayList = new ArrayList<>();
    int downloadID;
    AlertDialog dialog1;
    ProgressBar progressBar;
    TextView text;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityModelsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(true);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupRecyclerView();
        setupClickListeners();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        jsonPlaceHolder = ApiClient.getClient().create(JsonPlaceHolder.class);
    }

    private void setupRecyclerView() {
        binding.recyclerview.setLayoutManager(new LinearLayoutManager(this));
        malariaArrayList = Functions.loadModels(this);
        malariaAdapter = new MalariaAdapter(malariaArrayList, this);
        binding.recyclerview.setAdapter(malariaAdapter);
    }

    private void setupClickListeners() {
        binding.next.setOnClickListener(view ->
                startActivity(new Intent(this, PermissionsActivity.class))
        );

        malariaAdapter.setOnItemClickListener(position -> {
            if (malariaArrayList != null && !malariaArrayList.isEmpty() && position < malariaArrayList.size()){
                model = malariaArrayList.get(position);
                if (Functions.isInternetAvailable(ModelsActivity.this)){
                    startDownload(model.getModel_reference(),
                            model.getAccess_url(),
                            model.getFilename());
                }
                else {
                    Toast.makeText(this, "Please connect to the internet", Toast.LENGTH_SHORT).show();
                }
            }

        });

        binding.check.setOnClickListener( view -> {
            checkModels();
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(ModelsActivity.this, HomeActivity.class));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_manu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.checker){
            checkModels();
            return true;
        }
        else {
            return super.onOptionsItemSelected(item);
        }

    }

    public void checkModels(){
        if (Functions.isInternetAvailable(ModelsActivity.this)) {
            dialog = Functions.showDialog(this, "Checking, please wait...");
            Call<Download> call = jsonPlaceHolder.getModels("steve");
            call.enqueue(new Callback<Download>() {
                @Override
                public void onResponse(Call<Download> call, Response<Download> response) {
                    if (response.isSuccessful()){
                        Download download = response.body();
                        if (download != null) {
                            currentsArrayList = download.getCurrentList();
                            if (currentsArrayList != null) {
                                int value = currentsArrayList.size();
                                dialog.dismiss();
                                if (value == 0){
                                    Toast.makeText(ModelsActivity.this, "There are no models uploaded to server yet", Toast.LENGTH_SHORT).show();
                                }
                                else {
                                    Log.d("Model results", "onResponse: " + currentsArrayList);
                                    Result result = Functions.checkData(ModelsActivity.this, currentsArrayList);
                                    dialog.dismiss();
                                    if (result.isDataUpdated()){
//                                    modelAdapter.notifyDataSetChanged();
                                        malariaArrayList.clear();
                                        malariaArrayList.addAll(result.getCurrentArrayList());
                                        malariaAdapter.updateModelList(result.getCurrentArrayList());
                                        Toast.makeText(ModelsActivity.this, "There are models that require updating", Toast.LENGTH_SHORT).show();
                                    }
                                    else {
                                        Toast.makeText(ModelsActivity.this, "All models are up to date", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                            else {
                                dialog.dismiss();
                                Toast.makeText(ModelsActivity.this, "Currently there are no models", Toast.LENGTH_SHORT).show();
                            }
                        }
                        else {
                            dialog.dismiss();
                            Toast.makeText(ModelsActivity.this, "Currently there are no models", Toast.LENGTH_SHORT).show();
                        }
                    }
                    else {
                        dialog.dismiss();
                        int code = response.code();
                        Toast.makeText(ModelsActivity.this, "Request failed: " + code, Toast.LENGTH_SHORT).show();
//                    if(code == 400 || code == 404 || code == 403 || code == 401){
//                        Toast.makeText(ModelsActivity.this, getDetail(response), Toast.LENGTH_SHORT).show();
//                    }
//                    else{
//                        Toast.makeText(ModelsActivity.this, "Request failed: " + code, Toast.LENGTH_SHORT).show();
//                    }
                    }
                }

                @Override
                public void onFailure(Call<Download> call, Throwable t) {
                    dialog.dismiss();
                    Toast.makeText(ModelsActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "Please connect to the internet", Toast.LENGTH_SHORT).show();
        }

    }

    private void startDownload(String modelReference, String accessUrl, String filename) {
        binding.layout.setVisibility(View.VISIBLE);
        binding.progress.setIndeterminate(true);
        binding.text.setText("Preparing download...");

        // First get file size
        new Thread(() -> {
            try {
                URL fileUrl = new URL(accessUrl);
                HttpURLConnection connection = (HttpURLConnection) fileUrl.openConnection();
                long totalBytes = connection.getContentLength();


                if (totalBytes <= 0) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Invalid file size" + totalBytes, Toast.LENGTH_SHORT).show();
                        binding.layout.setVisibility(View.GONE);
                    });
                    return;
                }

                // Check existing download status
                if (Status.RUNNING == PRDownloader.getStatus(downloadID)) {
                    PRDownloader.pause(downloadID);
                    return;
                }

                if (Status.PAUSED == PRDownloader.getStatus(downloadID)) {
                    PRDownloader.resume(downloadID);
                    return;
                }

                // Start the download
                runOnUiThread(() -> {
                    String path = getFilesDir().getAbsolutePath();
                    String name = modelReference + ".tflite";
                    downloadID = PRDownloader.download(accessUrl, path, name)
                            .build()
                            .setOnStartOrResumeListener(() -> {
                                binding.progress.setIndeterminate(false);
                                binding.text.setText("Download started");
                                Log.d("Download", "Download started/resumed");
                            })
                            .setOnPauseListener(() -> {
                                Log.d("Download", "Download paused");
                            })
                            .setOnCancelListener(() -> {
                                downloadID = 0;
                                binding.layout.setVisibility(View.GONE);
                                Log.d("Download", "Download cancelled");
                            })
                            .setOnProgressListener(progress -> {
                                long progressPercent = progress.currentBytes * 100 / totalBytes;
                                binding.progress.setProgress((int) progressPercent);
                                binding.text.setText("Downloaded: " + getProgressDisplayLine(progress.currentBytes, totalBytes));
                            })
                            .start(new OnDownloadListener() {
                                @Override
                                public void onDownloadComplete() {
                                    binding.layout.setVisibility(View.GONE);
                                    downloadID = 0;

                                    // Update model list
                                    malariaArrayList = Functions.updateModel(ModelsActivity.this, model, name);
                                    malariaAdapter.updateModelList(malariaArrayList);
//
                                    // Delete old file if exists
                                    if (!filename.isEmpty()) {
                                        File oldFile = new File(getDataDir(), filename);
                                        if (oldFile.exists()) {
                                            oldFile.delete();
                                        }
                                    }
//
                                    Toast.makeText(ModelsActivity.this,
                                            "Download completed successfully",
                                            Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onError(Error error) {
                                    binding.layout.setVisibility(View.GONE);
                                    downloadID = 0;
                                    Toast.makeText(ModelsActivity.this,
                                            "Download failed: " + error.getServerErrorMessage(),
                                            Toast.LENGTH_SHORT).show();
                                    Log.d("Download", "onError: " + error.getServerErrorMessage());
                                }


                            });
                });

            } catch (IOException e) {
                runOnUiThread(() -> {
                    binding.layout.setVisibility(View.GONE);
                    Toast.makeText(ModelsActivity.this,
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    public String getProgressDisplayLine(long currentBytes, long totalBytes) {
        return getBytesToMBString(currentBytes) + "/" + getBytesToMBString(totalBytes);
    }

    public String getBytesToMBString(long bytes) {
        return String.format(Locale.ENGLISH, "%.2fMb", bytes / (1024.00 * 1024.00));
    }
}