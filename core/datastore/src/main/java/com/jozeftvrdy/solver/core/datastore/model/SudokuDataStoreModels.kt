package com.jozeftvrdy.solver.core.datastore.model

import androidx.annotation.Keep

@Keep
enum class SudokuImageProcessingType {
    FastestLeastReliable,
    Average,
    SlowestMostReliable;

    companion object {
        val defaultValue: SudokuImageProcessingType
            get() = Average
    }
}

enum class HandwrittenLibrary {
    LiteRT,
    TensorflowLite,
}

@Keep
enum class HandwrittenRecognizerModel(
    val assetName: String,
    val library: HandwrittenLibrary,
) {
    Model1(
        assetName = "mnist1.tflite",
        library = HandwrittenLibrary.TensorflowLite,
    ),
    Model2(
        assetName = "mnist2.tflite",
        library = HandwrittenLibrary.TensorflowLite,
    ),
    Model3(
        assetName = "mnist3.tflite",
        library = HandwrittenLibrary.TensorflowLite,
    ),
    Model4(
        assetName = "mnist4.tflite",
        library = HandwrittenLibrary.LiteRT,
    );

    companion object {
        val defaultValue: HandwrittenRecognizerModel
            get() = Model2
    }
}