package com.regula.documentreader;

import static android.graphics.BitmapFactory.decodeStream;

import android.Manifest;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.regula.documentreader.api.DocumentReader;
import com.regula.documentreader.api.completions.IDocumentReaderCompletion;
import com.regula.documentreader.api.completions.IDocumentReaderPrepareCompletion;
import com.regula.documentreader.api.enums.DocReaderAction;
import com.regula.documentreader.api.enums.eGraphicFieldType;
import com.regula.documentreader.api.enums.eVisualFieldType;
import com.regula.documentreader.api.errors.DocumentReaderException;
import com.regula.documentreader.api.params.DocReaderConfig;
import com.regula.documentreader.api.results.DocumentReaderResults;
import com.regula.documentreader.api.results.DocumentReaderScenario;
import com.regula.documentreader.api.results.DocumentReaderTextField;
import com.regula.documentreader.custom.Camera2Activity;
import com.regula.documentreader.custom.CameraActivity;
import com.regula.documentreader.custom.CustomRegActivity;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_BROWSE_PICTURE = 11;
    private static final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 22;
    private static final int PERMISSIONS_REQUEST_READ_PHONE_STATE = 33;
    private static final String MY_SHARED_PREFS = "MySharedPrefs";
    private static final String DO_RFID = "doRfid";

    private TextView nameTv;
    private TextView showScanner;
    private TextView recognizeImage;

    private ImageView portraitIv;
    private ImageView docImageIv;

    private CheckBox doRfidCb;

    private ListView scenarioLv;

    private SharedPreferences sharedPreferences;
    private boolean doRfid;
    private AlertDialog loadingDialog;
    private Button btnAddition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        nameTv = findViewById(R.id.nameTv);
        showScanner = findViewById(R.id.showScannerLink);
        recognizeImage = findViewById(R.id.recognizeImageLink);

        portraitIv = findViewById(R.id.portraitIv);
        docImageIv = findViewById(R.id.documentImageIv);

        scenarioLv = findViewById(R.id.scenariosList);
        btnAddition = findViewById(R.id.btnAddition);

        doRfidCb = findViewById(R.id.doRfidCb);

        sharedPreferences = getSharedPreferences(MY_SHARED_PREFS, MODE_PRIVATE);

        initView();
        prepareDatabase();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            //Image browsing intent processed successfully
            if (requestCode == REQUEST_BROWSE_PICTURE){
                ArrayList<Uri> imageUris = new ArrayList<>();
                ClipData clipData = data.getClipData();

                if (clipData == null) {
                    Uri imageUri = data.getData();
                    if (imageUri != null) {
                        imageUris.add(imageUri);
                    }
                } else {
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        imageUris.add(clipData.getItemAt(i).getUri());
                    }
                }

                if (imageUris.size() > 0) {
                    loadingDialog = showDialog("Processing image");
                    if (imageUris.size() == 1) {
                        DocumentReader.Instance().recognizeImage(getBitmap(imageUris.get(0), 1920, 1080), completion);
                    } else {
                        Bitmap[] bitmaps = new Bitmap[imageUris.size()];
                        for (int i = 0; i < bitmaps.length; i++) {
                            bitmaps[i] = getBitmap(imageUris.get(i), 1920, 1080);
                        }
                        DocumentReader.Instance().recognizeImages(bitmaps, completion);
                    }
                }
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
            case  PERMISSIONS_REQUEST_READ_PHONE_STATE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    prepareDatabase();
                } else {
                    Toast.makeText(MainActivity.this, "Permission is required to init Document Reader",Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void initView() {
        recognizeImage.setOnClickListener(view -> {
            if (!DocumentReader.Instance().isReady())
                return;

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
        });

        showScanner.setOnClickListener(view -> {
            if (!DocumentReader.Instance().isReady())
                return;

            clearResults();

            //starting video processing
            DocumentReader.Instance().showScanner(MainActivity.this, completion);
        });

        scenarioLv.setOnItemClickListener((adapterView, view, i, l) -> {
            if (!DocumentReader.Instance().isReady())
                return;

            ScenarioAdapter adapter = (ScenarioAdapter) adapterView.getAdapter();

            //setting selected scenario to DocumentReader params
            DocumentReader.Instance().processParams().scenario = adapter.getItem(i);
            adapter.setSelectedPosition(i);
            adapter.notifyDataSetChanged();

        });
        btnAddition.setOnClickListener(view -> {
            // Manifest.permission.READ_PHONE_STATE is required if you are using a license by device Id
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                prepareDatabase();
            } else {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.READ_PHONE_STATE},
                        PERMISSIONS_REQUEST_READ_PHONE_STATE);
            }
        });
    }

    private void initializeReader(AlertDialog initDialog) {
        DocReaderConfig config = new DocReaderConfig(LicenseUtil.getLicense(this));
        config.setLicenseUpdate(true);

        //Initializing the reader
        DocumentReader.Instance().initializeReader(MainActivity.this, config, (success, error) -> {
            if (initDialog.isShowing()) {
                initDialog.dismiss();
            }

            if (!success) { //Initialization was not successful
                // If initialization is performed using a license by device Id
                if(error != null && error.toString().contains("device")) {
                    errorDialog(error.toString());
                    btnAddition.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(MainActivity.this, "Init failed:" + error, Toast.LENGTH_LONG).show();
                }
                return;
            }

            setupCustomization();
            setupFunctionality();

            //initialization successful
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
            ArrayList<String> scenarios = new ArrayList<>();
            for (DocumentReaderScenario scenario : DocumentReader.Instance().availableScenarios) {
                scenarios.add(scenario.name);
            }

            //setting default scenario
            DocumentReader.Instance().processParams().scenario = scenarios.get(0);

            final ScenarioAdapter adapter = new ScenarioAdapter(MainActivity.this, android.R.layout.simple_list_item_1, scenarios);
            scenarioLv.setAdapter(adapter);

        });
    }

    private void setupCustomization() {
        DocumentReader.Instance().customization().edit().setShowHelpAnimation(false).apply();
    }

    private void setupFunctionality() {
        DocumentReader.Instance().functionality().edit().setShowCameraSwitchButton(true).apply();
    }

    //DocumentReader processing callback
    private final IDocumentReaderCompletion completion = new IDocumentReaderCompletion() {
        @Override
        public void onCompleted(int action, DocumentReaderResults results, DocumentReaderException error) {
            //processing is finished, all results are ready
            if (action == DocReaderAction.COMPLETE) {
                if(loadingDialog!=null && loadingDialog.isShowing()){
                    loadingDialog.dismiss();
                }

                //Checking, if nfc chip reading should be performed
                if (doRfid && results!=null && results.chipPage != 0) {
                    //starting chip reading
                    DocumentReader.Instance().startRFIDReader(MainActivity.this, new IDocumentReaderCompletion() {
                        @Override
                        public void onCompleted(int rfidAction, DocumentReaderResults results, DocumentReaderException error) {
                            if (rfidAction == DocReaderAction.COMPLETE || rfidAction == DocReaderAction.CANCEL) {
                                displayResults(results);
                            }
                        }
                    });
                } else {
                    displayResults(results);
                }
            } else if (action == DocReaderAction.TIMEOUT) {
                Toast.makeText(MainActivity.this, "Timeout",Toast.LENGTH_LONG).show();
                displayResults(results);
            } else  {
                //something happened before all results were ready
                if(action==DocReaderAction.CANCEL){
                    Toast.makeText(MainActivity.this, "Scanning was cancelled",Toast.LENGTH_LONG).show();
                } else if(action == DocReaderAction.ERROR){
                    Toast.makeText(MainActivity.this, "Error:" + error, Toast.LENGTH_LONG).show();
                }
            }
        }
    };

    private AlertDialog showDialog(String msg) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
        View dialogView = getLayoutInflater().inflate(R.layout.simple_dialog, null);
        dialog.setTitle(msg);
        dialog.setView(dialogView);
        dialog.setCancelable(false);
        return dialog.show();
    }

    private void errorDialog(String msg) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
        dialog.setTitle("Init Failed");
        dialog.setMessage(msg);
        dialog.setPositiveButton("Ok", null);
        dialog.setNeutralButton("Copy Id", (dialogInterface, i) -> {
            String id = msg.substring(msg.lastIndexOf(" ") + 2, msg.length()-1);
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Copied Text", id);
            clipboard.setPrimaryClip(clip);
        });
        dialog.setCancelable(false);
        dialog.show();
    }

    //show received results on the UI
    private void displayResults(DocumentReaderResults results){
        if(results!=null) {
            String name = results.getTextFieldValueByType(eVisualFieldType.FT_SURNAME_AND_GIVEN_NAMES);
            if (name != null){
                nameTv.setText(name);
            }

            // through all text fields
            if(results.textResult != null) {
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

    public void clickOnShowCameraActivity(View view) {
        if (!DocumentReader.Instance().isReady())
            return;

        Intent cameraIntent = new Intent();
        cameraIntent.setClass(MainActivity.this, CameraActivity.class);
        startActivity(cameraIntent);
    }

    private void prepareDatabase() {
        if (!DocumentReader.Instance().isReady()) {
            final AlertDialog initDialog = showDialog("Initializing");

            //preparing database files, it will be downloaded from network only one time and stored on user device
            DocumentReader.Instance().prepareDatabase(MainActivity.this, "Full", new IDocumentReaderPrepareCompletion() {
                @Override
                public void onPrepareProgressChanged(int progress) {
                    initDialog.setTitle("Downloading database: " + progress + "%");
                }

                @Override
                public void onPrepareCompleted(boolean status, DocumentReaderException error) {
                    if (status) {
                        initDialog.setTitle("Initializing");
                        initializeReader(initDialog);
                    } else {
                        initDialog.dismiss();
                        Toast.makeText(MainActivity.this, "Prepare DB failed:" + error, Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

    public void clickOnShowCustomCameraActivity(View view) {
        if (!DocumentReader.Instance().isReady())
            return;

        Intent cameraIntent = new Intent();
        cameraIntent.setClass(MainActivity.this, CustomRegActivity.class);
        startActivity(cameraIntent);
    }

    public void clickOnShowCustomCamera2Activity(View view) {
        if (!DocumentReader.Instance().isReady())
            return;

        Intent cameraIntent = new Intent();
        cameraIntent.setClass(MainActivity.this, Camera2Activity.class);
        startActivity(cameraIntent);
    }
}
