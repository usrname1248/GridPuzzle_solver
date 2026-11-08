package com.jozeftvrdy.solver.core.datastore.`interface`

import com.jozeftvrdy.solver.core.datastore.model.HandwrittenRecognizerModel
import com.jozeftvrdy.solver.core.datastore.model.SudokuImageProcessingType
import kotlinx.coroutines.flow.Flow

interface SudokuPreferencesRepository {
    val imageProcessingTypeFlow: Flow<SudokuImageProcessingType>

    val handwrittenRecognizerModelFlow: Flow<HandwrittenRecognizerModel>

    suspend fun updateImageProcessingType(type: SudokuImageProcessingType)

    suspend fun updateHandwrittenRecognizerModel(model: HandwrittenRecognizerModel)

    suspend fun getImageProcessingType(): SudokuImageProcessingType

    suspend fun getHandwrittenRecognizerModel(): HandwrittenRecognizerModel
}