package com.example.unityblemanager;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class PermissionActivity extends Activity {
    public static final String EXTRA_PERMS = "perms";
    public static final String EXTRA_REQ   = "reqCode";

    public static final int MULTIPLE_PERMISSIONS = 100;
    private String[] mNeededPermissions = new String[]{
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.BLUETOOTH_CONNECT,
    };

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        CheckPermissions();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        finish();
    }

    private void CheckPermissions(){

        ArrayList<String> lToRequestPermissions = new ArrayList<String>();
        for(int i = 0; i < mNeededPermissions.length; ++i){
            if (ContextCompat.checkSelfPermission(this, mNeededPermissions[i]) != PackageManager.PERMISSION_GRANTED){
                lToRequestPermissions.add(mNeededPermissions[i]);
            }
        }

        if (lToRequestPermissions.size() > 0){
            String[] lPermissionsList = new String[lToRequestPermissions.size()];
            lToRequestPermissions.toArray(lPermissionsList);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(lPermissionsList, MULTIPLE_PERMISSIONS);
            }
        }
    }
}
