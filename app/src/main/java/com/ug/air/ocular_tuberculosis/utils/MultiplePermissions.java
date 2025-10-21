package com.ug.air.ocular_tuberculosis.utils;

import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.ug.air.ocular_tuberculosis.ui.PermissionsActivity;

import java.util.List;

public class MultiplePermissions implements MultiplePermissionsListener {

    private final PermissionsActivity permissionsActivity;

    public MultiplePermissions(PermissionsActivity permissionsActivity) {
        this.permissionsActivity = permissionsActivity;
    }
    @Override
    public void onPermissionsChecked(MultiplePermissionsReport report) {
        for (PermissionGrantedResponse response : report.getGrantedPermissionResponses()){
            permissionsActivity.showPermissionGranted(response.getPermissionName());
        }

        for (PermissionDeniedResponse response : report.getDeniedPermissionResponses()){
            permissionsActivity.showPermissionsDenied(response.getPermissionName());
        }
    }

    @Override
    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken token) {
        permissionsActivity.showPermissionRational(token);
    }
}
