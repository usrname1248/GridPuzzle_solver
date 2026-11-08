package com.jozeftvrdy.solver.gridpuzzle.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jozeftvrdy.solver.core.datastore.`interface`.SudokuPreferencesRepository
import com.jozeftvrdy.solver.core.datastore.model.HandwrittenRecognizerModel
import com.jozeftvrdy.solver.core.datastore.model.SudokuImageProcessingType
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

class SettingsViewModel(
    val sudokuPreferencesRepository: SudokuPreferencesRepository
): ViewModel() {
    private val _errorChannel: Channel<SettingsUiError.OneTimeError> = Channel()
    val oneTimeErrorFlow: Flow<SettingsUiError.OneTimeError> = _errorChannel.receiveAsFlow()


    val sudokuImageProcessingTypeFlow: StateFlow<SudokuImageProcessingType?> = sudokuPreferencesRepository.imageProcessingTypeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val handwrittenRecognizerModelFlow: StateFlow<HandwrittenRecognizerModel?> = sudokuPreferencesRepository.handwrittenRecognizerModelFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun onImageProcessingTypeChange(type: SudokuImageProcessingType) {
        viewModelScope.launch {
            try {
                sudokuPreferencesRepository.updateImageProcessingType(type)
            } catch (e: IOException) {

            }
        }
    }

    fun onHandwrittenRecognizerModelChange(model: HandwrittenRecognizerModel) {
        viewModelScope.launch {
            try {
                sudokuPreferencesRepository.updateHandwrittenRecognizerModel(model)
            } catch (e: IOException) {

            }
        }
    }
}