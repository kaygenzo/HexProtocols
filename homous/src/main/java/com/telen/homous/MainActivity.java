package com.telen.homous;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import com.crashlytics.android.Crashlytics;
import com.telen.sdk.demo.commonui.FirestoreManager;

import javax.inject.Inject;

import io.fabric.sdk.android.Fabric;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 0;
    private static final String TAG = MainActivity.class.getSimpleName();

    @Inject FirestoreManager firestoreManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DaggerApplicationWrapper.getComponent(this).inject(this);

        if(firestoreManager!=null)
            firestoreManager.init();

        Fabric.with(this, new Crashlytics());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_DENIED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_DENIED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_WIFI_STATE
            }, REQUEST_CODE);
        } else {
            permissionsGranted();
        }
    }

    private void permissionsGranted() {
        BluetoothAdapter.getDefaultAdapter().enable();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE:
                for (int permissionResult: grantResults) {
                    if (permissionResult == PackageManager.PERMISSION_DENIED)
                        return;
                }
                permissionsGranted();
        }
    }

    @Override
    protected void onDestroy() {
        if(firestoreManager!=null)
            firestoreManager.destroy();
        super.onDestroy();
    }
}
