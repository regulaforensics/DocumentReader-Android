package com.regula.documentreader;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/**
 * Created by Sergey Yakimchik on 10.09.21.
 * Copyright (c) 2021 Regula. All rights reserved.
 */

class ScenarioAdapter extends ArrayAdapter<String> {

    private int selectedPosition;

    public ScenarioAdapter(@NonNull Context context, int resource, @NonNull List<String> objects) {
        super(context, resource, objects);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = super.getView(position, convertView, parent);

        if(position == selectedPosition){
            view.setBackgroundColor(Color.LTGRAY);
        } else {
            view.setBackgroundColor(Color.TRANSPARENT);
        }
        return view;
    }

    public void setSelectedPosition(int position) {
        selectedPosition = position;
    }
}
