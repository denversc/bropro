package com.denversc.bropro

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val driver = BrotherP300BTDriver()
    private val repository = LabelRepository(application)

    private val _status = MutableStateFlow("Ready to print")
    val status: StateFlow<String> = _status.asStateFlow()

    private val _history = MutableStateFlow<List<Label>>(emptyList())
    val history: StateFlow<List<Label>> = _history.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        _history.value = repository.getLabels()
    }

    fun printLabel(text: String) {
        if (text.isBlank()) {
            _status.value = "Cannot print empty label"
            return
        }

        viewModelScope.launch {
            _status.value = "Printing..."
            val result = driver.printLabel(text)

            result.fold(
                onSuccess = {
                    _status.value = "Printed successfully!"
                    saveLabel(text)
                },
                onFailure = { error ->
                    _status.value = "Print failed: ${error.message}"
                }
            )
        }
    }

    private fun saveLabel(text: String) {
        repository.saveLabel(text)
        loadHistory()
    }

    fun deleteLabel(id: String) {
        repository.deleteLabel(id)
        loadHistory()
    }
}
