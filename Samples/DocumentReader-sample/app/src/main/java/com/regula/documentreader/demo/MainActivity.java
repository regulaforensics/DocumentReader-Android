package com.regula.documentreader.demo;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.regula.documentreader.api.DocumentReader;
import com.regula.documentreader.api.params.InputParams;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    public static final String PREFERENCES = "preferences";
    public static final String SELECTED_CAMERA_ID = "cameraId";

    private static final int PERMISSIONS_REQUEST_CAMERA = 1;
    private static final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 2;
    private static final int REQUEST_BROWSE_PICTURE = 3;
    private static final int PERMISSIONS_REQUEST_CAMERA_SETTINGS = 4;

    private static boolean sIsInitialized;
    private ImageButton mCameraBtn, mFolderBtn, mAboutBtn, mSettingBtn;
    private SharedPreferences mPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCameraBtn = (ImageButton) findViewById(R.id.cameraBtn);
        mFolderBtn = (ImageButton) findViewById(R.id.folderBtn);
        mAboutBtn = (ImageButton) findViewById(R.id.aboutBtn);
        mSettingBtn = (ImageButton) findViewById(R.id.settingBtn);

        mPreferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!sIsInitialized) {
            try {
                InputStream licInput = getResources().openRawResource(R.raw.regula);
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                int i;
                try {
                    i = licInput.read();
                    while (i != -1) {
                        byteArrayOutputStream.write(i);
                        i = licInput.read();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                byte[] license = byteArrayOutputStream.toByteArray();
                sIsInitialized = DocumentReader.Instance().setLicense(MainActivity.this, license);
                licInput.close();
                byteArrayOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (sIsInitialized) {
            mCameraBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this,
                            Manifest.permission.CAMERA)
                            != PackageManager.PERMISSION_GRANTED) {

                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.CAMERA},
                                PERMISSIONS_REQUEST_CAMERA);
                    } else {
                        int camId = mPreferences.getInt(SELECTED_CAMERA_ID,-1);
                        DocumentReader.Instance().showScanner(camId, new
                                DocumentReader.DocumentReaderCallback() {
                                    @Override
                                    public void onCompleted() {
                                        Intent intent = new Intent(MainActivity.this,ResultsActivity.class);
                                        MainActivity.this.startActivity(intent);
                                    }
                                });
                    }
                }
            });

            mAboutBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, AboutActivity.class);
                    MainActivity.this.startActivity(intent);
                }
            });

            mFolderBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        if (ContextCompat.checkSelfPermission(MainActivity.this,
                                Manifest.permission.READ_EXTERNAL_STORAGE)
                                != PackageManager.PERMISSION_GRANTED) {

                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                    PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                        } else {
                            startPictureChoosing();
                        }
                    } else {
                        startPictureChoosing();
                    }
                }
            });

            mSettingBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this,
                            Manifest.permission.CAMERA)
                            != PackageManager.PERMISSION_GRANTED) {

                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.CAMERA},
                                PERMISSIONS_REQUEST_CAMERA_SETTINGS);
                    } else {
                        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                        MainActivity.this.startActivity(intent);
                    }
                }
            });
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle(R.string.strError);
            builder.setMessage(R.string.strLicenseInvalid);
            builder.setPositiveButton(R.string.strOK, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    System.exit(0);
                }
            });
            builder.show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_BROWSE_PICTURE){
                if (data.getData() != null) {
                    Uri selectedImage = data.getData();
                    Bitmap bmp = getBitmap(selectedImage);
                    InputParams params = new InputParams();
                    params.processParam.mrz = true;
                    params.processParam.ocr = true;
                    params.imageInputParam.height=bmp.getHeight();
                    params.imageInputParam.width=bmp.getWidth();
                    params.imageInputParam.type= ImageFormat.JPEG;
                    DocumentReader.Instance().process(bmp,params);

                    Intent intent = new Intent(MainActivity.this,ResultsActivity.class);
                    MainActivity.this.startActivity(intent);
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
                    startPictureChoosing();
                } else {
                    Toast.makeText(MainActivity.this, R.string.browse_permission_required,Toast.LENGTH_LONG).show();
                }
            } break;
            case PERMISSIONS_REQUEST_CAMERA:{
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
                    MainActivity.this.startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, R.string.camera_permission_required,Toast.LENGTH_LONG).show();
                }
            } break;
            case PERMISSIONS_REQUEST_CAMERA_SETTINGS:{
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                    MainActivity.this.startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, R.string.camera_permission_required,Toast.LENGTH_LONG).show();
                }
            } break;
        }
    }

    private void startPictureChoosing() {
        Intent intent = new Intent();
        intent.setType("image/*");
        if (Build.VERSION.SDK_INT >= 18) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_BROWSE_PICTURE);
    }

    private Bitmap getBitmap(Uri selectedImage) {
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
        options.inSampleSize = calculateInSampleSize(options, 1280, 720);
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeStream(is, null, options);
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}
