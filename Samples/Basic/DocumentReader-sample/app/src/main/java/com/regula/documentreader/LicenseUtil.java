package com.regula.documentreader;

import android.app.Activity;

import androidx.annotation.NonNull;

import java.io.InputStream;

/**
 * Created by Sergey Yakimchik on 10.09.21.
 * Copyright (c) 2021 Regula. All rights reserved.
 */

class LicenseUtil {

    /**
     * Reading the license from raw resource file
     * @param activity current activity
     * @return license byte array
     */
    public static byte[] getLicense(@NonNull Activity activity) {
        try {
            InputStream licInput = activity.getResources().openRawResource(R.raw.regula);
            int available = licInput.available();
            final byte[] license = new byte[available];
            //noinspection ResultOfMethodCallIgnored
            licInput.read(license);

            licInput.close();

            return license;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }
}
