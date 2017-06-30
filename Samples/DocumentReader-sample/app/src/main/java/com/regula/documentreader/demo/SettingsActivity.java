package com.regula.documentreader.demo;

import android.app.Activity;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.HashMap;

@SuppressWarnings("deprecation")
public class SettingsActivity extends Activity {
    private RadioGroup camerasGroup;
    private TextView horizontalAngleTv,verticalAngleTv;
    private SharedPreferences prefs;
    private HashMap<Integer,String> camerasHorAngle, camerasVerAngle;

    View.OnClickListener rbListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            horizontalAngleTv.setText(String.format(getString(R.string.camera_hor_angle), camerasHorAngle.get(v.getId())));
            verticalAngleTv.setText(String.format(getString(R.string.camera_ver_angle), camerasVerAngle.get(v.getId())));

            prefs.edit().putInt(MainActivity.SELECTED_CAMERA_ID, v.getId()).apply();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences(MainActivity.PREFERENCES, MODE_PRIVATE);

        camerasGroup = (RadioGroup) findViewById(R.id.camerasRG);
        horizontalAngleTv = (TextView) findViewById(R.id.horizontalAngleTv);
        verticalAngleTv = (TextView) findViewById(R.id.verticalAngleTv);

        camerasHorAngle = new HashMap<>();
        camerasVerAngle = new HashMap<>();
    }

    @Override
    protected void onResume() {
        super.onResume();

        GetCameraList();
    }

    private void GetCameraList() {
        camerasGroup.removeAllViews();
        int selectedCamera = prefs.getInt(MainActivity.SELECTED_CAMERA_ID, -1);
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);

            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                Camera camera = Camera.open(i);
                Camera.Parameters parameters = camera.getParameters();
                camera.release();

                String horAngle = String.valueOf(parameters.getHorizontalViewAngle());
                String verAngle = String.valueOf(parameters.getVerticalViewAngle());

                RadioButton rdbtn = new RadioButton(this);
                rdbtn.setId(i);
                rdbtn.setText(getString(R.string.camera) + " " + rdbtn.getId());
                rdbtn.setOnClickListener(rbListener);

                if (i == selectedCamera) {
                    rdbtn.setChecked(true);

                    horizontalAngleTv.setText(String.format(getString(R.string.camera_hor_angle), horAngle));
                    verticalAngleTv.setText(String.format(getString(R.string.camera_ver_angle), verAngle));
                }

                camerasGroup.addView(rdbtn);

                camerasVerAngle.put(i, verAngle);
                camerasHorAngle.put(i, horAngle);
            }
        }

        if(camerasGroup.getChildCount()==1 || selectedCamera==-1){
            RadioButton button = (RadioButton) camerasGroup.getChildAt(0);
            button.performClick();
        }
    }
}
