package com.regula.documentreader.util;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.regula.documentreader.R;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Created by Sergey Yakimchik on 10.09.21.
 * Copyright (c) 2021 Regula. All rights reserved.
 */

public class LicenseUtil {

    public static byte[] readFileFromAssets(String assetPackageName, String fileName, Context context) {
        InputStream licInput;
        try {
            if (context.getAssets().list(assetPackageName) == null
                    || !Arrays.asList(context.getAssets().list(assetPackageName)).contains(fileName)) {
                Log.e("FileUtil", "asset: " + fileName + " is absent");
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            licInput = context.getAssets().open(assetPackageName + "/" + fileName);
            int available = licInput != null ? licInput.available() : 0;
            if (available == 0)
                return null;
            final byte[] license = new byte[available];
            licInput.read(license);
            licInput.close();
            return license;
        } catch (Exception e) {
            e.getMessage();
        }
        return null;
    }
}
