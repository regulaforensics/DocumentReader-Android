package com.regula.documentreader

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter

/**
 * Created by Sergey Yakimchik on 10.09.21.
 * Copyright (c) 2021 Regula. All rights reserved.
 */
internal class ScenarioAdapter constructor(context: Context, resource: Int, objects: List<String>) :
    ArrayAdapter<String?>(context, resource, objects) {
    private var selectedPosition: Int = 0
    public override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View = super.getView(position, convertView, parent)
        if (position == selectedPosition) {
            view.setBackgroundColor(Color.LTGRAY)
        } else {
            view.setBackgroundColor(Color.TRANSPARENT)
        }
        return view
    }

    fun setSelectedPosition(position: Int) {
        selectedPosition = position
    }
}