package com.regula.documentreader;

import static com.regula.documentreader.BaseActivity.DO_RFID;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.regula.documentreader.api.DocumentReader;
import com.regula.documentreader.api.enums.eGraphicFieldType;
import com.regula.documentreader.api.enums.eRPRM_Lights;
import com.regula.documentreader.api.enums.eRPRM_ResultType;
import com.regula.documentreader.api.enums.eVisualFieldType;
import com.regula.documentreader.api.results.DocumentReaderGraphicField;
import com.regula.documentreader.api.results.DocumentReaderResults;
import com.regula.documentreader.util.BluetoothPermissionHelper;

public class MainFragment extends Fragment {

    private TextView nameTv;
    private TextView showScanner;
    private TextView recognizeImage;
    private TextView recognizePdf;
    private TextView scanFingerprint;

    private ImageView portraitIv;
    private ImageView docImageIv;
    private ImageView uvImageView;
    private ImageView irImageView;

    private CheckBox doRfidCb;

    private ListView scenarioLv;
    private volatile MainCallbacks mCallbacks;
    public static int RFID_RESULT = 100;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_main, container, false);

        nameTv = root.findViewById(R.id.nameTv);
        showScanner = root.findViewById(R.id.showScannerLink);
        recognizeImage = root.findViewById(R.id.recognizeImageLink);
        recognizePdf = root.findViewById(R.id.recognizePdfLink);
        scanFingerprint = root.findViewById(R.id.fingerScannerLink);

        portraitIv = root.findViewById(R.id.portraitIv);
        docImageIv = root.findViewById(R.id.documentImageIv);
        uvImageView = root.findViewById(R.id.uvImageView);
        irImageView = root.findViewById(R.id.irImageView);

        scenarioLv = root.findViewById(R.id.scenariosList);

        doRfidCb = root.findViewById(R.id.doRfidCb);

        initView();
        return root;
    }

    @Override
    public void onResume() {//used to show scenarios after fragments transaction
        super.onResume();
        if (getActivity() != null && DocumentReader.Instance().isReady())
            ((BaseActivity) getActivity()).setScenarios();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mCallbacks = (MainCallbacks) getActivity();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }


    private void initView() {
        recognizePdf.setOnClickListener(v -> mCallbacks.recognizePdf());
        scanFingerprint.setOnClickListener(v -> {
            if (!this.getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
                Toast.makeText(getActivity(), "ble not supported", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!this.getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                Toast.makeText(getActivity(), "ble not supported", Toast.LENGTH_SHORT).show();
                return;
            }

            BluetoothPermissionHelper.requestBlePermissions(this.getActivity(), PackageManager.PERMISSION_GRANTED);
            if (BluetoothPermissionHelper.isPermissionsGranted(getActivity())) {
                Intent intent = new Intent(getActivity(), FingerScannerActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(getActivity(), "ble permission is not granted", Toast.LENGTH_SHORT).show();
            }
        });
        recognizeImage.setOnClickListener(view -> {
            if (!DocumentReader.Instance().isReady())
                return;

            clearResults();
            mCallbacks.recognizeImage();
            //checking for image browsing permissions
        });

        showScanner.setOnClickListener(view -> {
            clearResults();
            launchCamera(((BaseActivity) getActivity()).activeCameraMode);
        });

        scenarioLv.setOnItemClickListener((adapterView, view, i, l) -> {
            ScenarioAdapter adapter = (ScenarioAdapter) adapterView.getAdapter();
            mCallbacks.scenarioLv(adapter.getItem(i));

            adapter.setSelectedPosition(i);
            adapter.notifyDataSetChanged();
        });
    }

    public void displayResults(DocumentReaderResults results) {
        if (results != null) {
            String name = results.getTextFieldValueByType(eVisualFieldType.FT_SURNAME_AND_GIVEN_NAMES);
            if (name != null) {
                nameTv.setText(name);
            }

            Bitmap portrait = results.getGraphicFieldImageByType(eGraphicFieldType.GF_PORTRAIT);
            if (portrait != null) {
                portraitIv.setImageBitmap(portrait);
            }

            Bitmap documentImage = results.getGraphicFieldImageByType(eGraphicFieldType.GF_DOCUMENT_IMAGE);
            docImageIv.setImageBitmap(documentImage);
            DocumentReaderGraphicField uvDocumentReaderGraphicField = results.getGraphicFieldByType(eGraphicFieldType.GF_DOCUMENT_IMAGE,
                    eRPRM_ResultType.RPRM_RESULT_TYPE_RAW_IMAGE, 0, eRPRM_Lights.RPRM_LIGHT_UV);

            if (uvDocumentReaderGraphicField != null) {
                uvImageView.setVisibility(View.VISIBLE);
                uvImageView.setImageBitmap(resizeBitmap(uvDocumentReaderGraphicField.getBitmap()));
            }

            DocumentReaderGraphicField irDocumentReaderGraphicField = results.getGraphicFieldByType(eGraphicFieldType.GF_DOCUMENT_IMAGE,
                    eRPRM_ResultType.RPRM_RESULT_TYPE_RAW_IMAGE, 0, eRPRM_Lights.RPRM_Light_IR_Full);

            if (irDocumentReaderGraphicField != null) {
                irImageView.setVisibility(View.VISIBLE);
                irImageView.setImageBitmap(resizeBitmap(irDocumentReaderGraphicField.getBitmap()));
            }
        }
    }

    private Bitmap resizeBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            double aspectRatio = (double) bitmap.getWidth() / (double) bitmap.getHeight();
            return Bitmap.createScaledBitmap(bitmap, (int) (480 * aspectRatio), 480, false);
        }
        return null;
    }

    private void clearResults() {
        nameTv.setText("");
        portraitIv.setImageResource(R.drawable.portrait);
        docImageIv.setImageResource(R.drawable.id);
    }

    public void setAdapter(ScenarioAdapter adapter) {
        scenarioLv.setAdapter(adapter);
    }

    public void setDoRfid(boolean rfidAvailable, SharedPreferences sharedPreferences) {
        boolean doRfid = sharedPreferences.getBoolean(DO_RFID, false);
        doRfidCb.setChecked(doRfid);
        mCallbacks.setDoRFID(doRfid);

        if (rfidAvailable) {
            doRfidCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                    sharedPreferences.edit().putBoolean(DO_RFID, checked).apply();
                    mCallbacks.setDoRFID(checked);
                }
            });
        } else {
            doRfidCb.setVisibility(View.GONE);
        }
    }

    private void launchCamera(int activeCameraMode) {
        switch (activeCameraMode) {
            case CameraMode.SDK_SHOW_SCANNER:
                mCallbacks.showScanner();
                break;
            case CameraMode.SHOW_CAMERA_ACTIVITY:
                mCallbacks.showCameraActivity();
                break;
            case CameraMode.SHOW_CUSTOM_CAMERA_ACTIVITY:
                mCallbacks.showCustomCameraActivity();
                break;

            case CameraMode.SHOW_CUSTOM_CAMERA_ACTIVITY2:
                mCallbacks.showCustomCamera2Activity();
                break;
        }
    }

    private static class CameraMode {
        public static final int SDK_SHOW_SCANNER = 0;
        public static final int SHOW_CAMERA_ACTIVITY = 1;
        public static final int SHOW_CUSTOM_CAMERA_ACTIVITY = 2;
        public static final int SHOW_CUSTOM_CAMERA_ACTIVITY2 = 3;
    }

    interface MainCallbacks {

        void showCameraActivity();

        void showCustomCameraActivity();

        void showCustomCamera2Activity();

        void scenarioLv(String item);

        void showScanner();

        void recognizeImage();

        void recognizePdf();

        void setDoRFID(boolean checked);
    }
}