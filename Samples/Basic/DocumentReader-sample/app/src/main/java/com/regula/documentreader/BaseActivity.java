package com.regula.documentreader;

import static android.graphics.BitmapFactory.decodeStream;

import static com.regula.documentreader.MainFragment.RFID_RESULT;
import static com.regula.documentreader.SettingsFragment.RfidMode.CUSTOM;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.regula.documentreader.api.DocumentReader;
import com.regula.documentreader.api.completions.IDocumentReaderCompletion;
import com.regula.documentreader.api.completions.IDocumentReaderInitCompletion;
import com.regula.documentreader.api.completions.IDocumentReaderPrepareCompletion;
import com.regula.documentreader.api.completions.IRfidPKDCertificateCompletion;
import com.regula.documentreader.api.completions.IRfidReaderRequest;
import com.regula.documentreader.api.completions.IRfidTASignatureCompletion;
import com.regula.documentreader.api.enums.DocReaderAction;
import com.regula.documentreader.api.errors.DocumentReaderException;
import com.regula.documentreader.api.params.rfid.PKDCertificate;
import com.regula.documentreader.api.params.rfid.authorization.PAResourcesIssuer;
import com.regula.documentreader.api.params.rfid.authorization.TAChallenge;
import com.regula.documentreader.api.results.DocumentReaderResults;
import com.regula.documentreader.api.results.DocumentReaderScenario;
import com.regula.documentreader.custom.Camera2Activity;
import com.regula.documentreader.custom.CameraActivity;
import com.regula.documentreader.custom.CustomRegActivity;
import com.regula.documentreader.custom.CustomRfidActivity;
import com.regula.documentreader.util.CertificatesUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public abstract class BaseActivity extends AppCompatActivity implements MainFragment.MainCallbacks {

    public static final int REQUEST_BROWSE_PICTURE = 11;
    public static final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 22;
    private static final String MY_SHARED_PREFS = "MySharedPrefs";
    public static final String DO_RFID = "doRfid";
    private static final String TAG_UI_FRAGMENT = "ui_fragment";
    private static final String TAG_SETTINGS_FRAGMENT = "settings_fragment";

    protected SharedPreferences sharedPreferences;
    private boolean doRfid;
    private AlertDialog loadingDialog;
    protected MainFragment mainFragment;
    protected SettingsFragment settingsFragment;
    protected FrameLayout fragmentContainer;

    public int activeCameraMode;
    public int rfidMode;
    public static DocumentReaderResults documentReaderResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fragmentContainer = findViewById(R.id.fragmentContainer);

        mainFragment = (MainFragment) findFragmentByTag(TAG_UI_FRAGMENT);
        if (mainFragment == null) {
            mainFragment = new MainFragment();
            replaceFragment(mainFragment,false);
        }
        sharedPreferences = getSharedPreferences(MY_SHARED_PREFS, MODE_PRIVATE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_actions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            settingsFragment = (SettingsFragment) findFragmentByTag(TAG_SETTINGS_FRAGMENT);
            if (settingsFragment == null) {
                settingsFragment = new SettingsFragment();
                replaceFragment(settingsFragment,true);
            }
        }
        return false;
    }

    @Override
    public void showCameraActivity() {
        if (!DocumentReader.Instance().isReady())
            return;

        Intent cameraIntent = new Intent();
        cameraIntent.setClass(BaseActivity.this, CameraActivity.class);
        startActivity(cameraIntent);
    }

    @Override
    public void showCustomCameraActivity() {
        if (!DocumentReader.Instance().isReady())
            return;

        Intent cameraIntent = new Intent();
        cameraIntent.setClass(BaseActivity.this, CustomRegActivity.class);
        startActivity(cameraIntent);
    }

    @Override
    public void showCustomCamera2Activity() {
        if (!DocumentReader.Instance().isReady())
            return;

        Intent cameraIntent = new Intent();
        cameraIntent.setClass(BaseActivity.this, Camera2Activity.class);
        startActivity(cameraIntent);
    }

    @Override
    public void scenarioLv(String item) {
        if (!DocumentReader.Instance().isReady())
            return;

        //setting selected scenario to DocumentReader params
        DocumentReader.Instance().processParams().scenario = item;
    }

    @Override
    public void showScanner() {
        if (!DocumentReader.Instance().isReady())
            return;
        DocumentReader.Instance().showScanner(BaseActivity.this, completion);
    }

    @Override
    public void recognizeImage() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        } else {
            //start image browsing
            createImageBrowsingRequest();
        }
    }

    @Override
    public void recognizePdf() {
        if (!DocumentReader.Instance().isReady())
            return;
        
        showDialog("Processing pdf");
        Executors.newSingleThreadExecutor().execute(() -> {
            InputStream is;
            byte[] buffer = null;
            try {
                is = getAssets().open("Regula/test.pdf");
                int size = is.available();
                buffer = new byte[size];
                is.read(buffer);
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            byte[] finalBuffer = buffer;
            runOnUiThread(() -> DocumentReader.Instance().recognizeImage(finalBuffer, completion));
        });
    }

    @Override
    public void setDoRFID(boolean checked) {
        doRfid = checked;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RFID_RESULT) {
            if (documentReaderResults != null && rfidMode == CUSTOM)
                mainFragment.displayResults(documentReaderResults);
        }

        if (resultCode == RESULT_OK) {
            //Image browsing intent processed successfully
            if (requestCode == REQUEST_BROWSE_PICTURE) {
                if (data.getData() != null) {
                    Uri selectedImage = data.getData();
                    Bitmap bmp = getBitmap(selectedImage, 1920, 1080);

                    showDialog("Processing image");

                    DocumentReader.Instance().recognizeImage(bmp, completion);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!DocumentReader.Instance().isReady()) {
            showDialog("Initializing");

            //preparing database files, it will be downloaded from network only one time and stored on user device
            DocumentReader.Instance().prepareDatabase(BaseActivity.this, "FullAuth", new IDocumentReaderPrepareCompletion() {
                @Override
                public void onPrepareProgressChanged(int progress) {
                    setTitleDialog("Downloading database: " + progress + "%");
                }

                @Override
                public void onPrepareCompleted(boolean status, DocumentReaderException error) {
                    if (status) {
                        onPrepareDbCompleted();
                    } else {
                        dismissDialog();
                        Toast.makeText(BaseActivity.this, "Prepare DB failed:" + error, Toast.LENGTH_LONG).show();
                    }
                }
            });
        } else {
            successfulInit();
        }
    }

    protected abstract void initializeReader();

    protected abstract void onPrepareDbCompleted();


    protected final IDocumentReaderInitCompletion initCompletion = (result, error) -> {
        dismissDialog();

        if (!result) { //Initialization was not successful
            Toast.makeText(BaseActivity.this, "Init failed:" + error, Toast.LENGTH_LONG).show();
            return;
        }
        successfulInit();

    };

    protected void successfulInit() {
        setupCustomization();
        setupFunctionality();

        mainFragment.setDoRfid(DocumentReader.Instance().isRFIDAvailableForUse(), sharedPreferences);
        //getting current processing scenario and loading available scenarios to ListView
        setScenarios();
    }


    private void setupCustomization() {
        DocumentReader.Instance().customization().edit().setShowHelpAnimation(false).apply();
    }

    private void setupFunctionality() {
        DocumentReader.Instance().functionality().edit()
                .setUseAuthenticator(true)
                .setShowCameraSwitchButton(true)
                .apply();
    }

    private final IDocumentReaderCompletion completion = new IDocumentReaderCompletion() {
        @Override
        public void onCompleted(int action, DocumentReaderResults results, DocumentReaderException error) {
            //processing is finished, all results are ready
            if (action == DocReaderAction.COMPLETE) {
                dismissDialog();

                //Checking, if nfc chip reading should be performed
                if (doRfid && results != null && results.chipPage != 0) {
                    //starting chip reading
                    if (rfidMode == CUSTOM) {
                        Intent rfidIntent = new Intent(BaseActivity.this, CustomRfidActivity.class);
                        startActivityForResult(rfidIntent, RFID_RESULT);
                    } else if (rfidMode == SettingsFragment.RfidMode.DEFAULT) {
                        DocumentReader.Instance().startRFIDReader(BaseActivity.this, new IDocumentReaderCompletion() {
                            @Override
                            public void onCompleted(int rfidAction, DocumentReaderResults results, DocumentReaderException error) {
                                if (rfidAction == DocReaderAction.COMPLETE || rfidAction == DocReaderAction.CANCEL) {
                                    mainFragment.displayResults(results);
                                }
                            }
                        }, new IRfidReaderRequest() {
                            @Override
                            public void onRequestPACertificates(@Nullable byte[] bytes, @Nullable PAResourcesIssuer paResourcesIssuer, @NonNull IRfidPKDCertificateCompletion rfidPKDCertificateCompletion) {
                                rfidPKDCertificateCompletion.onCertificatesReceived(CertificatesUtil.getRfidCertificates(getAssets(), "Regula/certificates").toArray(new PKDCertificate[0]));
                            }

                            @Override
                            public void onRequestTACertificates(@Nullable String s, @NonNull IRfidPKDCertificateCompletion rfidPKDCertificateCompletion) {
                                rfidPKDCertificateCompletion.onCertificatesReceived(CertificatesUtil.getRfidTACertificates(getAssets()).toArray(new PKDCertificate[0]));
                            }

                            @Override
                            public void onRequestTASignature(@Nullable TAChallenge taChallenge, @NonNull IRfidTASignatureCompletion signatureCompletion) {
                                signatureCompletion.onSignatureReceived(null);
                            }
                        });
                    }

                    addRfidCertificates();
                } else {
                    mainFragment.displayResults(results);
                }
            } else {
                //something happened before all results were ready
                if (action == DocReaderAction.CANCEL) {
                    Toast.makeText(BaseActivity.this, "Scanning was cancelled", Toast.LENGTH_LONG).show();
                } else if (action == DocReaderAction.ERROR) {
                    Toast.makeText(BaseActivity.this, "Error:" + error, Toast.LENGTH_LONG).show();
                }
            }
        }
    };

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
                    Toast.makeText(this, "Permission required, to browse images", Toast.LENGTH_LONG).show();
                }
            }
            break;
        }
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
        ContentResolver resolver = this.getContentResolver();
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

    @Override
    protected void onPause() {
        super.onPause();

        if (loadingDialog != null) {
            loadingDialog.dismiss();
            loadingDialog = null;
        }
    }


    protected void setTitleDialog(String msg) {
        if (loadingDialog != null) {
            loadingDialog.setTitle(msg);
        } else {
            showDialog(msg);
        }
    }

    protected void dismissDialog() {
        if (loadingDialog != null) {
            loadingDialog.dismiss();
        }
    }

    protected void showDialog(String msg) {
        dismissDialog();
        AlertDialog.Builder builderDialog = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.simple_dialog, null);
        builderDialog.setTitle(msg);
        builderDialog.setView(dialogView);
        builderDialog.setCancelable(false);
        loadingDialog = builderDialog.show();
    }


    public Fragment findFragmentByTag(String tag) {
        FragmentManager fm = getSupportFragmentManager();
        return fm.findFragmentByTag(tag);
    }

    private void replaceFragment(Fragment fragment, boolean addFragmentInBackstack) {
        String backStateName = fragment.getClass().getName();
        String fragmentTag = backStateName;

        FragmentManager manager = getSupportFragmentManager();
        boolean fragmentPopped = manager.popBackStackImmediate(backStateName, 0);

        if (!fragmentPopped && manager.findFragmentByTag(fragmentTag) == null) {
            FragmentTransaction ft = manager.beginTransaction();
            ft.replace(R.id.fragmentContainer, fragment, fragmentTag);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);

            if (addFragmentInBackstack)
                ft.addToBackStack(backStateName);

            ft.commit();
        }
    }

    public void setScenarios() {
        ArrayList<String> scenarios = new ArrayList<>();
        for (DocumentReaderScenario scenario : DocumentReader.Instance().availableScenarios) {
            scenarios.add(scenario.name);
        }

        //setting default scenario
        DocumentReader.Instance().processParams().scenario = scenarios.get(0);

        final ScenarioAdapter adapter = new ScenarioAdapter(BaseActivity.this, android.R.layout.simple_list_item_1, scenarios);
        mainFragment.setAdapter(adapter);
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0)
            getSupportFragmentManager().popBackStack();
        else
            super.onBackPressed();
    }

    private void addRfidCertificates() {
        List<PKDCertificate> certificates = new ArrayList<>();
        certificates.addAll(CertificatesUtil.getRfidCertificates(getAssets(), "Regula/certificates"));
        if (certificates.size() > 0) {
            DocumentReader.Instance().addPKDCertificates(certificates);
        }
    }
}