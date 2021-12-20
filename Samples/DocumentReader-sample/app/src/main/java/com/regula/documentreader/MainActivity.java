package com.regula.documentreader;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.regula.documentreader.api.DocumentReader;
import com.regula.documentreader.api.usb.RegDeviceService;
import com.regula.documentreader.api.completions.IDocumentReaderCompletion;
import com.regula.documentreader.api.completions.IDocumentReaderInitCompletion;
import com.regula.documentreader.api.completions.IDocumentReaderPrepareCompletion;
import com.regula.documentreader.api.enums.DocReaderAction;
import com.regula.documentreader.api.enums.eGraphicFieldType;
import com.regula.documentreader.api.enums.eRFID_Password_Type;
import com.regula.documentreader.api.enums.eVisualFieldType;
import com.regula.documentreader.api.errors.DocumentReaderException;
import com.regula.documentreader.api.results.DocumentReaderResults;
import com.regula.documentreader.api.results.DocumentReaderScenario;
import com.regula.documentreader.api.results.DocumentReaderTextField;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static android.graphics.BitmapFactory.decodeStream;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_BROWSE_PICTURE = 11;
    private static final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 22;
    private static final String MY_SHARED_PREFS = "MySharedPrefs";
    private static final String DO_RFID = "doRfid";
    private static final String CONTINUOUS_MODE = "continuousMode";
    private static final int CUSTOM_DEVICE_REQUEST_CODE = 125;
    private static final int CUSTOM_RFID_REQUEST_CODE = 126;

    private TextView nameTv;
    private TextView showScanner;
    private TextView recognizeImage;
    private TextView customActivityLink;

    private ImageView portraitIv;
    private ImageView docImageIv;

    private CheckBox doRfidCb;
    private CheckBox continuousModeCb;

    private ListView scenarioLv;

    private SharedPreferences sharedPreferences;
    private boolean doRfid;
    private AlertDialog loadingDialog;

    private int selectedPosition;

    private RegDeviceService mRegulaService;

    private Handler HANDLER = new Handler(Looper.getMainLooper());

    public static DocumentReaderResults documentReaderResults;
    private boolean isStartScan = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        nameTv = findViewById(R.id.nameTv);
        showScanner = findViewById(R.id.showScannerLink);
        recognizeImage = findViewById(R.id.recognizeImageLink);
        customActivityLink = findViewById(R.id.customActivityLink);

        portraitIv = findViewById(R.id.portraitIv);
        docImageIv = findViewById(R.id.documentImageIv);

        scenarioLv = findViewById(R.id.scenariosList);

        doRfidCb = findViewById(R.id.doRfidCb);
        continuousModeCb = findViewById(R.id.continuousModeCb);

        sharedPreferences = getSharedPreferences(MY_SHARED_PREFS, MODE_PRIVATE);

        initDocumentReader();
    }

    private void initDocumentReader() {
        final AlertDialog initDialog = showDialog("Initializing");

        //Reading the license from raw resource file
        try {
            InputStream licInput = getResources().openRawResource(R.raw.regula);
            int available = licInput.available();
            final byte[] license = new byte[available];
            //noinspection ResultOfMethodCallIgnored
            licInput.read(license);

            //preparing database files, it will be downloaded from network only one time and stored on user device
            DocumentReader.Instance().prepareDatabase(MainActivity.this, "Full", new IDocumentReaderPrepareCompletion() {
                @Override
                public void onPrepareProgressChanged(int progress) {
                    initDialog.setTitle("Downloading database: " + progress + "%");
                }

                @Override
                public void onPrepareCompleted(boolean status, DocumentReaderException error) {

                    //Initializing the reader
                    DocumentReader.Instance().initializeReader(MainActivity.this, license, new IDocumentReaderInitCompletion() {
                        @Override
                        public void onInitCompleted(boolean success, DocumentReaderException error) {
                            if (initDialog.isShowing()) {
                                initDialog.dismiss();
                            }

                            DocumentReader.Instance().customization().edit().setShowHelpAnimation(false).apply();
                            // turn on to use Regula Device instead of standard camera. Set up to false if you want to use native camera
                            DocumentReader.Instance().functionality().edit().setUseRegulaDevice(true).apply();
                            DocumentReader.Instance().functionality().edit().setRegDeviceContinuesMode(sharedPreferences.getBoolean(CONTINUOUS_MODE, true)).apply();


                            try {
                                DocumentReader.Instance().processParams().customParams = new JSONObject("{\"boundsParam\":{\"checkVariants\":\"basicOne\"}}");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            //initialization successful
                            if (success) {
                                showScanner.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        clearResults();

                                        //starting video processing
                                        DocumentReader.Instance().showScanner(MainActivity.this, completion);
                                    }
                                });

                                recognizeImage.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        clearResults();
                                        //checking for image browsing permissions
                                        if (ContextCompat.checkSelfPermission(MainActivity.this,
                                                Manifest.permission.READ_EXTERNAL_STORAGE)
                                                != PackageManager.PERMISSION_GRANTED) {

                                            ActivityCompat.requestPermissions(MainActivity.this,
                                                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                                    PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                                        } else {
                                            //start image browsing
                                            createImageBrowsingRequest();
                                        }
                                    }
                                });

                                customActivityLink.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        clearResults();
                                        Intent intent = new Intent(MainActivity.this, CustomDeviceActivity.class);
                                        startActivityForResult(intent, CUSTOM_DEVICE_REQUEST_CODE);
                                    }
                                });

                                continuousModeCb.setChecked(sharedPreferences.getBoolean(CONTINUOUS_MODE, true));
                                continuousModeCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                    @Override
                                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                        DocumentReader.Instance().functionality().edit().setRegDeviceContinuesMode(isChecked).apply();
                                        if (mRegulaService != null)
                                            mRegulaService.setContinuousMode(isChecked);
                                        sharedPreferences.edit().putBoolean(CONTINUOUS_MODE, isChecked).apply();
                                    }
                                });

                                if (DocumentReader.Instance().isRFIDAvailableForUse()) {
                                    //reading shared preferences
                                    doRfid = sharedPreferences.getBoolean(DO_RFID, false);
                                    doRfidCb.setChecked(doRfid);
                                    doRfidCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                        @Override
                                        public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                                            doRfid = checked;
                                            sharedPreferences.edit().putBoolean(DO_RFID, checked).apply();
                                        }
                                    });
                                } else {
                                    doRfidCb.setVisibility(View.GONE);
                                }

                                //getting current processing scenario and loading available scenarios to ListView
                                String currentScenario = DocumentReader.Instance().processParams().scenario;
                                ArrayList<String> scenarios = new ArrayList<>();
                                for (DocumentReaderScenario scenario : DocumentReader.Instance().availableScenarios) {
                                    scenarios.add(scenario.name);
                                }

                                //setting default scenario
                                if (currentScenario == null || currentScenario.isEmpty()) {
                                    currentScenario = scenarios.get(0);
                                    DocumentReader.Instance().processParams().scenario = currentScenario;
                                }

                                final ScenarioAdapter adapter = new ScenarioAdapter(MainActivity.this, android.R.layout.simple_list_item_1, scenarios);
                                selectedPosition = 0;
                                try {
                                    selectedPosition = adapter.getPosition(currentScenario);
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                                scenarioLv.setAdapter(adapter);

                                scenarioLv.setSelection(selectedPosition);

                                scenarioLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                    @Override
                                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                                        //setting selected scenario to DocumentReader params
                                        DocumentReader.Instance().processParams().scenario = adapter.getItem(i);
                                        selectedPosition = i;
                                        adapter.notifyDataSetChanged();

                                    }
                                });

                                //start
                                Intent intent = new Intent(MainActivity.this, RegDeviceService.class);
                                startService(intent);

                                successfulInit();
                            }
                            //Initialization was not successful
                            else {
                                Toast.makeText(MainActivity.this, "Init failed:" + error, Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
            });

            licInput.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        successfulInit();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if(loadingDialog!=null){
            loadingDialog.dismiss();
            loadingDialog = null;
        }

        if (mRegulaService != null) {
            mRegulaService.setCallback(null);
            unbindService(mConnection);
        }
    }

    private void successfulInit() {
        if (DocumentReader.Instance().getDocumentReaderIsReady()) {
            Intent intent = new Intent(MainActivity.this, RegDeviceService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            //Image browsing intent processed successfully
            if (requestCode == REQUEST_BROWSE_PICTURE){
                if (data.getData() != null) {
                    Uri selectedImage = data.getData();
                    Bitmap bmp = getBitmap(selectedImage, 1920, 1080);

                    loadingDialog = showDialog("Processing image");

                    DocumentReader.Instance().recognizeImage(bmp, completion);
                }
            } else if (requestCode == CUSTOM_DEVICE_REQUEST_CODE) {
                completeRecognition(documentReaderResults);
                documentReaderResults = null;
            } else if (requestCode == CUSTOM_RFID_REQUEST_CODE) {
                displayResults(documentReaderResults);
                documentReaderResults = null;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //access to gallery is allowed
                    createImageBrowsingRequest();
                } else {
                    Toast.makeText(MainActivity.this, "Permission required, to browse images",Toast.LENGTH_LONG).show();
                }
            } break;
        }
    }

    //DocumentReader processing callback
    private IDocumentReaderCompletion completion = new IDocumentReaderCompletion() {
        @Override
        public void onCompleted(int action, DocumentReaderResults results, DocumentReaderException error) {
            //processing is finished, all results are ready
            if (action == DocReaderAction.COMPLETE) {
                completeRecognition(results);
            } else {
                //something happened before all results were ready
                if(action==DocReaderAction.CANCEL){
                    Toast.makeText(MainActivity.this, "Scanning was cancelled",Toast.LENGTH_LONG).show();
                } else if(action == DocReaderAction.ERROR){
                    Toast.makeText(MainActivity.this, "Error:" + error, Toast.LENGTH_LONG).show();
                }
            }
            isStartScan = false;
        }
    };

    private void completeRecognition(DocumentReaderResults results) {
        if(loadingDialog!=null && loadingDialog.isShowing()){
            loadingDialog.dismiss();
        }

        //Checking, if nfc chip reading should be performed
        if (doRfid && results!=null && results.chipPage != 0) {
            //setting the chip's access key - mrz on car access number
            String accessKey = null;
            if ((accessKey = results.getTextFieldValueByType(eVisualFieldType.FT_MRZ_STRINGS)) != null && !accessKey.isEmpty()) {
                accessKey = results.getTextFieldValueByType(eVisualFieldType.FT_MRZ_STRINGS)
                        .replace("^", "").replace("\n","");
                DocumentReader.Instance().rfidScenario().setMrz(accessKey);
                DocumentReader.Instance().rfidScenario().setPacePasswordType(eRFID_Password_Type.PPT_MRZ);
            } else if ((accessKey = results.getTextFieldValueByType(eVisualFieldType.FT_CARD_ACCESS_NUMBER)) != null && !accessKey.isEmpty()) {
                DocumentReader.Instance().rfidScenario().setPassword(accessKey);
                DocumentReader.Instance().rfidScenario().setPacePasswordType(eRFID_Password_Type.PPT_CAN);
            }

            //starting chip reading
            if (DocumentReader.Instance().functionality().isUseRegulaDevice()) {
                Intent intent = new Intent(MainActivity.this, CustomRfidActivity.class);
                startActivityForResult(intent, CUSTOM_RFID_REQUEST_CODE);
            } else {
                DocumentReader.Instance().startRFIDReader(MainActivity.this, new IDocumentReaderCompletion() {
                    @Override
                    public void onCompleted(int rfidAction, DocumentReaderResults results, DocumentReaderException error) {
                        if (rfidAction == DocReaderAction.COMPLETE || rfidAction == DocReaderAction.CANCEL) {
                            displayResults(results);
                        }
                    }
                });
            }
        } else {
            displayResults(results);
        }
    }

    private AlertDialog showDialog(String msg) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
        View dialogView = getLayoutInflater().inflate(R.layout.simple_dialog, null);
        dialog.setTitle(msg);
        dialog.setView(dialogView);
        dialog.setCancelable(false);
        return dialog.show();
    }

    //show received results on the UI
    private void displayResults(DocumentReaderResults results){
        if(results!=null) {
            String name = results.getTextFieldValueByType(eVisualFieldType.FT_SURNAME_AND_GIVEN_NAMES);
            if (name != null){
                nameTv.setText(name);
            }

            // through all text fields
            if(results.textResult != null && results.textResult.fields != null) {
                for (DocumentReaderTextField textField : results.textResult.fields) {
                    String value = results.getTextFieldValueByType(textField.fieldType, textField.lcid);
                    Log.d("MainActivity", value + "\n");
                }
            }

            Bitmap portrait = results.getGraphicFieldImageByType(eGraphicFieldType.GF_PORTRAIT);
            if(portrait!=null){
                portraitIv.setImageBitmap(portrait);
            }

            Bitmap documentImage = results.getGraphicFieldImageByType(eGraphicFieldType.GF_DOCUMENT_IMAGE);
            if(documentImage!=null){
                double aspectRatio = (double) documentImage.getWidth() / (double) documentImage.getHeight();
                documentImage = Bitmap.createScaledBitmap(documentImage, (int)(480 * aspectRatio), 480, false);
                docImageIv.setImageBitmap(documentImage);
            }
        }
    }

    private void clearResults(){
        nameTv.setText("");
        portraitIv.setImageResource(R.drawable.portrait);
        docImageIv.setImageResource(R.drawable.id);
    }

    // creates and starts image browsing intent
    // results will be handled in onActivityResult method
    private void createImageBrowsingRequest() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_BROWSE_PICTURE);
    }

    // loads bitmap from uri
    private Bitmap getBitmap(Uri selectedImage, int targetWidth, int targetHeight) {
        ContentResolver resolver = MainActivity.this.getContentResolver();
        InputStream is = null;
        try {
            is = resolver.openInputStream(selectedImage);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, options);

        //Re-reading the input stream to move it's pointer to start
        try {
            is = resolver.openInputStream(selectedImage);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, targetWidth, targetHeight);
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return decodeStream(is, null, options);
    }

    // see https://developer.android.com/topic/performance/graphics/load-bitmap.html
    private int calculateInSampleSize(BitmapFactory.Options options, int bitmapWidth, int bitmapHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > bitmapHeight || width > bitmapWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > bitmapHeight
                    && (halfWidth / inSampleSize) > bitmapWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    class ScenarioAdapter extends ArrayAdapter<String>{

        public ScenarioAdapter(@NonNull Context context, int resource, @NonNull List<String> objects) {
            super(context, resource, objects);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View view = super.getView(position, convertView, parent);

            if(position == selectedPosition){
                view.setBackgroundColor(Color.LTGRAY);
            } else {
                view.setBackgroundColor(Color.TRANSPARENT);
            }
            return view;
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mRegulaService = ((RegDeviceService.LocalBinder)service).getService();

            mRegulaService.setContinuousMode(DocumentReader.Instance().functionality().isRegDeviceContinuesMode());
            mRegulaService.setCallback(regDeviceCallback);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mRegulaService = null;
        }
    };

    private RegDeviceService.RegDeviceServiceCallback regDeviceCallback = new RegDeviceService.RegDeviceServiceCallback() {

        @Override
        public void onFrame(byte[] frame) {
            if (DocumentReader.Instance().functionality().isRegDeviceContinuesMode() || isStartScan)
                return;

            final Bitmap bitmap = BitmapFactory.decodeByteArray(frame, 0, frame.length);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    loadingDialog = showDialog("Processing image");
                }
            });

            isStartScan = true;
            HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    DocumentReader.Instance().recognizeImage(bitmap, completion);
                }
            });
        }

        @Override
        public void onDeviceConnected() {
            super.onDeviceConnected();
            Log.d("MainActivity", "Device connected");
        }

        @Override
        public void onDeviceDisconnected() {
            super.onDeviceDisconnected();
            Log.d("MainActivity", "Device disconnected");
        }

        @Override
        public void onDocumentInserted() {
            super.onDocumentInserted();
            Log.d("MainActivity", "Document inserted");
        }

        @Override
        public void onDocumentRemoved() {
            super.onDocumentRemoved();
            Log.d("MainActivity", "Document removed");
        }
    };
}
