package com.regula.documentreader.demo;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Calendar;
import java.util.List;

public class AboutActivity extends Activity {

    private TextView copyrightTv, privacyTv;
    private ImageView facebook, linkedin, twitter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_about);

        copyrightTv= (TextView) findViewById(R.id.copyrightTv);
        privacyTv = (TextView) findViewById(R.id.privacyTv);

        facebook = (ImageView) findViewById(R.id.fbBtn);
        linkedin= (ImageView) findViewById(R.id.lnBtn);
        twitter =  (ImageView) findViewById(R.id.twBtn);
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            Calendar calendar = Calendar.getInstance();
            String year = String.valueOf(calendar.get(Calendar.YEAR));
            copyrightTv.setText(String.format(getString(R.string.strCopyright), year));
        } catch (Exception e) {
            e.printStackTrace();
        }

        privacyTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://regulaforensics.com/en/company/privacy/"));
                startActivity(intent);
            }
        });

        facebook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("fb://page/356561294452774"));
                final PackageManager packageManager = AboutActivity.this.getPackageManager();
                final List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                if (list.isEmpty()) {
                    intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/regulaforensics"));
                }
                startActivity(intent);
            }
        });

        linkedin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("linkedin://company/1653568"));
                final PackageManager packageManager = AboutActivity.this.getPackageManager();
                final List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                if (list.isEmpty()) {
                    intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.linkedin.com/company/1653568"));
                }
                startActivity(intent);
            }
        });

        twitter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("twitter://user?user_id=regulaforensics"));
                final PackageManager packageManager = AboutActivity.this.getPackageManager();
                final List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                if (list.isEmpty()) {
                    intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/regulaforensics"));
                }
                startActivity(intent);
            }
        });
    }
}
