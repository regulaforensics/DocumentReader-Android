package com.regula.documentreader.demo;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.regula.documentreader.api.results.DocumentReaderResults;

import java.util.ArrayList;
import java.util.List;

public class ResultsActivityTabbed extends AppCompatActivity {
    public static DocumentReaderResults documentReaderResults;
    TextView title;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results_tabbed);

        title = (TextView) findViewById(R.id.docTypeTv);

        ViewPager viewPager = (ViewPager) findViewById(R.id.container);
        setupViewPager(viewPager);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(documentReaderResults!=null && documentReaderResults.documentType!=null){
            title.setText(documentReaderResults.documentType.name);
        } else {
            title.setText(R.string.strUnknownDocType);
        }
    }

    private void setupViewPager(ViewPager viewPager){
        SectionsPageAdapter adapter = new SectionsPageAdapter(getSupportFragmentManager());
        adapter.addFragment(new TextResultsFragment(), getString(R.string.strTextResult));
        adapter.addFragment(new GraphicResultsFragment(), getString(R.string.strGraphicResult));
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
