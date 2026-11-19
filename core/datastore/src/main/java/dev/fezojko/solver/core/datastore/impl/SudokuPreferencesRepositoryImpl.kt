package dev.fezojko.solver.core.datastore.impl

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import dev.fezojko.solver.core.datastore.enumPreferencesKey
import dev.fezojko.solver.core.datastore.`interface`.SudokuPreferencesRepository
import dev.fezojko.solver.core.datastore.model.HandwrittenRecognizerModel
import dev.fezojko.solver.core.datastore.model.SudokuImageProcessingType
import dev.fezojko.solver.core.datastore.preferenceEnumFlow
import dev.fezojko.solver.core.datastore.updateEnumPreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class SudokuPreferencesRepositoryImpl(private val context: Context): SudokuPreferencesRepository {
    private val Context.dataStore by preferencesDataStore(name = "sudoku_preferences")

    private object PreferencesKeys {
        val IMAGE_PROCESSING_TYPE = enumPreferencesKey("image_processing_type")
        val HANDWRITTEN_RECOGNIZER_MODEL = enumPreferencesKey("handwritten_recognizer_model")
    }

    override val imageProcessingTypeFlow: Flow<SudokuImageProcessingType> =
        preferenceEnumFlow(context.dataStore, PreferencesKeys.IMAGE_PROCESSING_TYPE, SudokuImageProcessingType.defaultValue)

    override val handwrittenRecognizerModelFlow: Flow<HandwrittenRecognizerModel> =
        preferenceEnumFlow(context.dataStore, PreferencesKeys.HANDWRITTEN_RECOGNIZER_MODEL, HandwrittenRecognizerModel.defaultValue)

    override suspend fun updateImageProcessingType(type: SudokuImageProcessingType) =
        updateEnumPreference(context.dataStore, PreferencesKeys.IMAGE_PROCESSING_TYPE, type)

    override suspend fun updateHandwrittenRecognizerModel(model: HandwrittenRecognizerModel) =
        updateEnumPreference(context.dataStore, PreferencesKeys.HANDWRITTEN_RECOGNIZER_MODEL, model)

    override suspend fun getImageProcessingType(): SudokuImageProcessingType = imageProcessingTypeFlow.first()

    override suspend fun getHandwrittenRecognizerModel(): HandwrittenRecognizerModel = handwrittenRecognizerModelFlow.first()
}

