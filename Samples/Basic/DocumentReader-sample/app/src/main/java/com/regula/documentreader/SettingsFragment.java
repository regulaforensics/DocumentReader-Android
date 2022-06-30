package com.regula.documentreader;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.fragment.app.Fragment;

public class SettingsFragment extends Fragment {
    private RadioButton customRfidRb;
    private RadioButton defaultRfidRb;

    private RadioButton customCameraRb;
    private RadioButton customCamera2Rb;
    private RadioButton customCameraRegRb;
    private RadioButton defaultCameraRb;

    private volatile MainFragment.MainCallbacks mCallbacks;
    public static boolean isCustomRfidSelected;
    public static int RFID_RESULT = 100;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_settings, container, false);
        customRfidRb = root.findViewById(R.id.customRfidReaderRb);
        defaultRfidRb = root.findViewById(R.id.defaultRfidReaderRb);

        defaultCameraRb = root.findViewById(R.id.defaultScannerRb);
        customCameraRb = root.findViewById(R.id.customCameraRb);
        customCamera2Rb = root.findViewById(R.id.customCamera2Rb);
        customCameraRegRb = root.findViewById(R.id.customRegCameraRb);

        checkCameraByMode(((BaseActivity) getActivity()).activeCameraMode);
        checkRfidByMode(((BaseActivity) getActivity()).rfidMode);
        setupRfid();
        setupCameraRbListeners();
        return root;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mCallbacks = (MainFragment.MainCallbacks) getActivity();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    public void setupRfid() {
        defaultRfidRb.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked)
                ((BaseActivity) getActivity()).rfidMode = RfidMode.DEFAULT;
        });
        customRfidRb.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked)
                ((BaseActivity) getActivity()).rfidMode = RfidMode.CUSTOM;
        });
    }

    public void setupCameraRbListeners() {
        defaultCameraRb.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked)
                ((BaseActivity) getActivity()).activeCameraMode = CameraMode.SDK_SHOW_SCANNER;
        });
        customCameraRb.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked)
                ((BaseActivity) getActivity()).activeCameraMode = CameraMode.SHOW_CUSTOM_CAMERA_ACTIVITY;
        });
        customCamera2Rb.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked)
                ((BaseActivity) getActivity()).activeCameraMode = CameraMode.SHOW_CUSTOM_CAMERA_ACTIVITY2;
        });
        customCameraRegRb.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked)
                ((BaseActivity) getActivity()).activeCameraMode = CameraMode.SHOW_CAMERA_ACTIVITY;
        });
    }

    private void checkCameraByMode(int camMode) {
        switch (camMode) {
            case CameraMode.SDK_SHOW_SCANNER:
                defaultCameraRb.setChecked(true);
                break;
            case CameraMode.SHOW_CAMERA_ACTIVITY:
                customCameraRegRb.setChecked(true);
                break;
            case CameraMode.SHOW_CUSTOM_CAMERA_ACTIVITY:
                customCameraRb.setChecked(true);
                break;
            case CameraMode.SHOW_CUSTOM_CAMERA_ACTIVITY2:
                customCamera2Rb.setChecked(true);
                break;
        }
    }

    private void checkRfidByMode(int rfidMode) {
        switch (rfidMode) {
            case RfidMode.DEFAULT:
                defaultRfidRb.setChecked(true);
                break;
            case RfidMode.CUSTOM:
                customRfidRb.setChecked(true);
                break;
        }
    }

    public static class RfidMode {
        public static final int DEFAULT = 0;
        public static final int CUSTOM = 1;
    }

    private static class CameraMode {
        public static final int SDK_SHOW_SCANNER = 0;
        public static final int SHOW_CAMERA_ACTIVITY = 1;
        public static final int SHOW_CUSTOM_CAMERA_ACTIVITY = 2;
        public static final int SHOW_CUSTOM_CAMERA_ACTIVITY2 = 3;
    }
}
