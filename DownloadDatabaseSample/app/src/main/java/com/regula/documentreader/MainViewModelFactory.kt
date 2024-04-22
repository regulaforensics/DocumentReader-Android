package com.regula.documentreader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.regula.documentreader.api.DocumentReader

class MainViewModelFactory(private val documentReader: DocumentReader): ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(documentReader) as T
    }
}