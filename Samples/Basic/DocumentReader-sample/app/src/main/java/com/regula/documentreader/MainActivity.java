package com.regula.documentreader;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.regula.common.utils.CameraUtil;
import com.regula.documentreader.api.DocumentReader;
import com.regula.documentreader.api.params.DocReaderConfig;
import com.regula.documentreader.util.LicenseUtil;

public class MainActivity extends BaseActivity {
    private static final int PERMISSIONS_REQUEST_READ_PHONE_STATE = 33;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (LicenseUtil.readFileFromAssets("Regula", "regula.license", this) == null)
            showDialog(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.READ_PHONE_STATE},
                    PERMISSIONS_REQUEST_READ_PHONE_STATE);
        }
    }

    protected void initializeReader() {
        showDialog("Initializing");

        byte[] license = LicenseUtil.readFileFromAssets("Regula", "regula.license", this);

        DocReaderConfig config = new DocReaderConfig(license);
        config.setLicenseUpdate(true);

        //Initializing the reader
        DocumentReader.Instance().initializeReader(MainActivity.this, config, initCompletion);
    }

    @Override
    protected void onPrepareDbCompleted() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            initializeReader();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.READ_PHONE_STATE},
                    PERMISSIONS_REQUEST_READ_PHONE_STATE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_READ_PHONE_STATE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeReader();
            } else {
                Toast.makeText(MainActivity.this, "Permission is required to init Document Reader", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setTitle("Error");
        builder.setMessage("license in assets is missed!");
        builder.setPositiveButton(getString(R.string.strAccessibilityCloseButton), (dialog, which) -> finish());
        builder.setCancelable(false);
        builder.show();
    }
}
