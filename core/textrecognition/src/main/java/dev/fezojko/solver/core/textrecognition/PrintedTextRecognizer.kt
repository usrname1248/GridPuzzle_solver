package dev.fezojko.solver.core.textrecognition

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class PrintedTextRecognizer {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognizeText(bitmap: Bitmap, rotationDegrees: Int = 0): String? = suspendCancellableCoroutine { continuation ->
        val image = InputImage.fromBitmap(bitmap, rotationDegrees)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                if (continuation.isActive) {
                    continuation.resume(visionText.text)
                }
            }
            .addOnFailureListener { e ->
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
    }

    suspend fun recognizeMostProbableDigit(bitmap: Bitmap, rotationDegrees: Int = 0): Int? = recognizeText(
        bitmap = bitmap,
        rotationDegrees = rotationDegrees
    ).let { recognizedText ->
        val numbersWithCount = recognizedText?.filter { char ->
            (char.code >= '0'.code && char.code <= '9'.code)
        }?.groupBy { char ->
            val number = char.code - '0'.code
            number
        }?.mapValues { it.value.size }

        numbersWithCount?.maxByOrNull { pair ->
            pair.value
        }?.key
    }
}
