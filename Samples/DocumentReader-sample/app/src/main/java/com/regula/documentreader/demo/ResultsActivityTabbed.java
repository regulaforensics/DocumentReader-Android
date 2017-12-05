package com.regula.documentreader.demo;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;

import com.regula.documentreader.api.enums.eGraphicFieldType;
import com.regula.documentreader.api.enums.eRPRM_FieldVerificationResult;
import com.regula.documentreader.api.enums.eVisualFieldType;
import com.regula.documentreader.api.results.DocumentReaderResults;

import java.util.ArrayList;
import java.util.List;

public class ResultsActivityTabbed extends AppCompatActivity {
    public static DocumentReaderResults documentReaderResults;

    private static final String DEBUG = "ResultsActivity";
    private static final String LIVENESS="Liveness";
    private static final String MATCH = "Match";

    private TextView nameSurnameTv, sexAgeTv;
    private ImageView overallStatusIv, portraitIv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results_tabbed);

        nameSurnameTv = (TextView) findViewById(R.id.nameSurnameTv);
        sexAgeTv = (TextView) findViewById(R.id.sexAgeTv);

        overallStatusIv = (ImageView) findViewById(R.id.overallResultIv);
        portraitIv = (ImageView) findViewById(R.id.portraitIv);

        ViewPager viewPager = (ViewPager) findViewById(R.id.container);
        setupViewPager(viewPager);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(documentReaderResults!=null){
            String nameSurname = ResultsActivityTabbed.documentReaderResults.getTextFieldValueByType(eVisualFieldType.ft_Surname_And_Given_Names);
            if(nameSurname!=null) {
                nameSurnameTv.setText(nameSurname);
            }

            String sex= ResultsActivityTabbed.documentReaderResults.getTextFieldValueByType(eVisualFieldType.ft_Sex);
            if(sex!=null) {
                if (sex.equals("M"))
                    sex = getString(R.string.strMale);
                else if (sex.equals("F"))
                    sex = getString(R.string.strFemale);
                else
                    sex = null;
            }
            String age = ResultsActivityTabbed.documentReaderResults.getTextFieldValueByType(eVisualFieldType.ft_Age);
            String sexAge = sex != null ? sex + "," + age : age;
            if (sexAge != null) {
                sexAgeTv.setText(sexAge);
            }

            Bitmap portrait = ResultsActivityTabbed.documentReaderResults.getGraphicFieldImageByType(eGraphicFieldType.gf_Portrait);
            if(portrait!=null){
                portraitIv.setImageBitmap(portrait);
            }

            if(documentReaderResults.textResult!=null){
                if(documentReaderResults.textResult.status == eRPRM_FieldVerificationResult.RCF_Verified){
                    overallStatusIv.setImageResource(R.drawable.ok) ;
                } else if(documentReaderResults.textResult.status == eRPRM_FieldVerificationResult.RCF_Not_Verified ){
                    overallStatusIv.setImageResource(R.drawable.fail) ;
                } else {
                    overallStatusIv.setImageResource(R.drawable.undefined);
                }
            }
        }
    }

    private void setupViewPager(ViewPager viewPager){
        SectionsPageAdapter adapter = new SectionsPageAdapter(getSupportFragmentManager());
        adapter.addFragment(new OverallResultFragment(), getString(R.string.strOverall));
        adapter.addFragment(new TextResultsFragment(), getString(R.string.strTextResult));
        adapter.addFragment(new GraphicResultsFragment(), getString(R.string.strGraphicResult));

        if(documentReaderResults!=null && documentReaderResults.rfidSessionData !=null){
            adapter.addFragment( new RfidBinaryDataFragment(), getString(R.string.strRfidBinaryData));
        }

        viewPager.setAdapter(adapter);
    }


    private class SectionsPageAdapter extends FragmentPagerAdapter{
        private final List<Fragment> fragmentList = new ArrayList<>();
        private final List<String> titleList = new ArrayList<>();

        SectionsPageAdapter(FragmentManager fm) {
            super(fm);
        }

        void addFragment(Fragment fragment, String title){
            fragmentList.add(fragment);
            titleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return titleList.get(position);
        }

        @Override
        public Fragment getItem(int position) {
            return fragmentList.get(position);
        }

        @Override
        public int getCount() {
            return fragmentList.size();
        }
    }
}
