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
    public static byte[] getLicense(Context context) {
        if (context == null)
            return null;

        InputStream licInput = context.getResources().openRawResource(R.raw.regula);
        int available;
        try {
            available = licInput.available();
        } catch (IOException e) {
            return null;
        }
        byte[] license = new byte[available];
        try {
            licInput.read(license);
        } catch (IOException e) {
            return null;
        }

        return license;
    }
}
