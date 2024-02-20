package com.regula.documentreader

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.regula.documentreader.api.DocumentReader

class MainViewModelFactory(private val documentReader: DocumentReader, private val sharedPreferences: SharedPreferences): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(
            documentReader = documentReader,
            sharedPreferences = sharedPreferences) as T
    }
}