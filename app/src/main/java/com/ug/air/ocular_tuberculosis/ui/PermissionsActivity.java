package com.ug.air.ocular_tuberculosis.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.ug.air.ocular_tuberculosis.R;
import com.ug.air.ocular_tuberculosis.utils.MultiplePermissions;

public class PermissionsActivity extends AppCompatActivity {

    Button btn_allow;
    MultiplePermissions multiplePermissions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_permissions);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btn_allow = findViewById(R.id.btnAllow);

        if (allPermissionsGranted()) {
            navigateToHomeActivity();
            return; // Stop onCreate execution if navigation occurs
        }

        multiplePermissions = new MultiplePermissions(this);

        btn_allow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermissions();
            }
        });
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            Dexter.withActivity(this)
                    .withPermissions(Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES)
                    .withListener(multiplePermissions)
                    .check();
        } else { // Android 12 and below
            Dexter.withActivity(this)
                    // WRITE_EXTERNAL_STORAGE is usually not needed for Media Store access on modern devices,
                    // but kept here to match your original logic for older APIs.
                    .withPermissions(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .withListener(multiplePermissions)
                    .check();
        }
        // IMPORTANT: Navigation MUST NOT happen here. It happens in the listener's callback!
    }

    /**
     * Consolidated check for all required permissions to be used in onCreate/onResume.
     */
    private boolean allPermissionsGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    public void showPermissionGranted(String permissionName){
        switch (permissionName){
            case Manifest.permission.CAMERA:
            case Manifest.permission.READ_MEDIA_IMAGES:
            case Manifest.permission.READ_EXTERNAL_STORAGE:
            case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                break;
        }
    }

    public void showPermissionsDenied(String permissionName){
        switch (permissionName){
            case Manifest.permission.CAMERA:
                Toast.makeText(this, "Camera permissions Denied", Toast.LENGTH_SHORT).show();
                break;
            case Manifest.permission.READ_MEDIA_IMAGES:
            case Manifest.permission.READ_EXTERNAL_STORAGE:
            case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                Toast.makeText(this, "Storage permissions Denied", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    public void showPermissionRational(final PermissionToken token){
        new AlertDialog.Builder(this).setTitle("We need this permission")
                .setMessage("Please allow this permission for do something magic")
                .setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        token.continuePermissionRequest();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        token.cancelPermissionRequest();
                        dialog.dismiss();
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        token.cancelPermissionRequest();
                    }
                }).show();
    }

    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (allPermissionsGranted()) {
            navigateToHomeActivity();
        }
    }

    private void navigateToHomeActivity() {
        Intent homeIntent = new Intent(PermissionsActivity.this, HomeActivity.class);
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(homeIntent);
        finish();
    }
}