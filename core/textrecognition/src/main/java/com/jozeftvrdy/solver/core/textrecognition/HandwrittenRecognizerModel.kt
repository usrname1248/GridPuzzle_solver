package com.jozeftvrdy.solver.core.textrecognition

enum class HandwrittenLibrary {
    LiteRT,
    TensorflowLite,
}

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
}