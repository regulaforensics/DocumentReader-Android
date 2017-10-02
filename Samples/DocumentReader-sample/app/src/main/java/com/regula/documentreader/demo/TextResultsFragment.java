package com.regula.documentreader.demo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.regula.documentreader.api.results.DocumentReaderTextField;

import java.util.List;

public class TextResultsFragment extends Fragment {
    private ListView listView;
    private TextView noData;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View framgentView = inflater.inflate(R.layout.text_result_fragment,container, false);

        listView = (ListView) framgentView.findViewById(R.id.resultsLv);
        noData = (TextView) framgentView.findViewById(R.id.noDataTV);

        return framgentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        List<DocumentReaderTextField> textResults = null;
        if(ResultsActivityTabbed.documentReaderResults != null && ResultsActivityTabbed.documentReaderResults.textResult!=null){
            textResults = ResultsActivityTabbed.documentReaderResults.textResult.fields;
        }

        if(textResults!=null && textResults.size()>0){
            listView.setVisibility(View.VISIBLE);
            View listViewHeader = LayoutInflater.from(getContext()).inflate(R.layout.text_fields_header_layout, null);
            if(listView.getHeaderViewsCount()==0) {
                listView.addHeaderView(listViewHeader);
            }
            listView.setAdapter(new TextDataAdapter(getContext(), 0, textResults, listViewHeader));
        }else{
            noData.setVisibility(View.VISIBLE);
        }
    }
}
