package dev.fezojko.solver.core.textrecognition

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.annotation.FloatRange
import androidx.core.graphics.scale
import com.google.ai.edge.litert.Accelerator
import com.google.ai.edge.litert.CompiledModel
import com.google.ai.edge.litert.Environment
import dev.fezojko.solver.core.datastore.model.HandwrittenLibrary
import dev.fezojko.solver.core.datastore.model.HandwrittenRecognizerModel
import kotlinx.coroutines.suspendCancellableCoroutine
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.coroutines.resume

class HandwrittenNumbersRecognizer(
    private val appContext: Context
) {

    val accelerator by lazy {
        getAccelerator(appContext)
    }

    suspend fun recognize(
        bitmap: Bitmap,
        model: HandwrittenRecognizerModel,
        @FloatRange(to = 1.0)
        requiredProbability: Float? = null
    ): Result<Int?> {
        val calculation: (Float) -> Float =  {
            255 - it
        }
        return when (model.library) {
            HandwrittenLibrary.LiteRT -> recognizeWithLiteRt(
                bitmap,
                model.assetName,
                requiredProbability,
                calculation = calculation,
            )

            HandwrittenLibrary.TensorflowLite -> recognizeWithTensorflow(
                bitmap,
                model.assetName,
                requiredProbability,
                calculation = calculation,
            )
        }
    }

    suspend fun recognizeWithLiteRt(
        bitmap: Bitmap,
        assetName: String,
        requiredProbability: Float?,
        calculation: (Float) -> Float
    ): Result<Int?> = suspendCancellableCoroutine { continuation ->
        try {
            // 1. Create the LiteRT Environment
            val options = CompiledModel.Options(accelerator)

            CompiledModel.create(
                appContext.assets,
                assetName,
                options,
                null
            ).use { compiledModel ->

                // 1. Resize bitmap to exact training dimensions (28x28)
                val modelWidth = 28
                val modelHeight = 28
                val scaledBitmap = bitmap.scale(modelWidth, modelHeight, filter = true)


                // Convert to grayscale and normalize to 0..1
                val inputFloatArray = convertBitmapToFloatArray(scaledBitmap, calculation)

                // 2. Execution
                val inputBuffers = compiledModel.createInputBuffers()
                val outputBuffers = compiledModel.createOutputBuffers()

                inputBuffers[0].writeFloat(inputFloatArray)
                compiledModel.run(inputBuffers, outputBuffers)

                val outputFloatArray = outputBuffers[0].readFloat()

                // Cleanup buffers
                inputBuffers.forEach { it.close() }
                outputBuffers.forEach { it.close() }

                // 3. Postprocessing
                val predictedDigitWithProbability = findResult(outputFloatArray)
                val predictedDigit = predictedDigitWithProbability?.let {
                    if (requiredProbability != null && requiredProbability < it.second) {
                        null
                    } else {
                        it.first
                    }
                }

                compiledModel.close()

                if (continuation.isActive) {
                    continuation.resume(Result.success(predictedDigit))
                }
            }
        } catch (e: Exception) {
            Log.e("DigitsClassifier", "Classification error", e)
            if (continuation.isActive) {
                continuation.resume(Result.failure(e))
            }
        }
    }

    private fun convertBitmapToFloatArray(bitmap: Bitmap, calculation: (Float) -> Float): FloatArray {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // The original sample used ImageProcessor without grayscale conversion,
        // implying it sent 3 channels (RGB) to the model.
        // It also used NormalizeOp(0f, 1f), which effectively keeps the 0-255 range
        // when converting from the default Bitmap uint8 values to Float32.

        // Target: [1, 28, 28, 3]
        val output = FloatArray(width * height * 3)

        for (i in pixels.indices) {
            val pixel = pixels[i]

            // Extract RGB (ignore alpha)
            val r = Color.red(pixel).toFloat()
            val g = Color.green(pixel).toFloat()
            val b = Color.blue(pixel).toFloat()

            // Don't divide by 255.0f to match original 0..255 range behavior
            val baseIndex = i * 3
            output[baseIndex] = calculation(r)
            output[baseIndex + 1] = calculation(g)
            output[baseIndex + 2] = calculation(b)
        }
        return output
    }

    /**
     * Finds the index and value of the maximum element in a non-empty float array.
     */
    private fun findResult(array: FloatArray): Pair<Int, Float>? {
        if (array.isEmpty()) return null

        var maxIndex = 0
        var maxValue = array[0]

        for (i in array.indices) {
            if (array[i] > maxValue) {
                maxValue = array[i]
                maxIndex = i
            }
        }

        return maxIndex to maxValue
    }

    private fun loadModelFile(context: Context, modelPath: String): MappedByteBuffer {
        val fileDescriptor: AssetFileDescriptor = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    suspend fun recognizeWithTensorflow(
        bitmap: Bitmap,
        assetName: String,
        requiredProbability: Float?,
        calculation: (Float) -> Float
    ): Result<Int?> = suspendCancellableCoroutine { continuation ->
        try {
            Interpreter(loadModelFile(appContext, assetName)).use { interpreter ->
                // 1. Resize bitmap to exact training dimensions (28x28)
                val modelWidth = 28
                val modelHeight = 28
                val scaledBitmap = bitmap.scale(modelWidth, modelHeight, filter = true)

                // 2. Allocate direct ByteBuffer matching DigitsClassifier.java convertBitmapToByteBuffer
                val byteBuffer = ByteBuffer.allocateDirect(modelWidth * modelHeight * 4)
                byteBuffer.order(ByteOrder.nativeOrder())
                byteBuffer.rewind()

                val intValues = IntArray(modelWidth * modelHeight)
                scaledBitmap.getPixels(intValues, 0, scaledBitmap.width, 0, 0, scaledBitmap.width, scaledBitmap.height)

                for (pixelValue in intValues) {
                    val red = (pixelValue shr 16) and 0xFF
                    val green = (pixelValue shr 8) and 0xFF
                    val blue = pixelValue and 0xFF
                    val grayscale = (red + green + blue) / 3.0f
                    val normalizedPixelValue = calculation(grayscale)
                    byteBuffer.putFloat(normalizedPixelValue)
                }

                // 3. Output classes buffer matching DigitsClassifier.java (outputClasses = new float[1][10])
                val outputClasses = Array(1) { FloatArray(10) }

                // 4. Run inference
                interpreter.run(byteBuffer, outputClasses)

                // 5. Find maxIndex (matching DigitsClassifier.java classifyDigits)
                var maxIndex = -1
                var maxScore = -Float.MAX_VALUE
                for (i in 0 until 10) {
                    val score = outputClasses[0][i]
                    if (score > maxScore) {
                        maxIndex = i
                        maxScore = score
                    }
                }

                val resultValue = when {
                    maxIndex == -1 -> null
                    requiredProbability != null && maxScore < requiredProbability -> null
                    else -> maxIndex
                }

                if (continuation.isActive) {
                    continuation.resume(Result.success(resultValue))
                }
            }
        } catch (e: Exception) {
            Log.e("DigitsClassifier", "Classification error", e)
            if (continuation.isActive) {
                continuation.resume(Result.failure(e))
            }
        }
    }

    private fun getAccelerator(appContext: Context): Accelerator {
        val environment = Environment.create(appContext)

        val availableAccelerators: Set<Accelerator> = environment.getAvailableAccelerators()
        val acceleratorToUse = when {
//            availableAccelerators.contains(Accelerator.GPU) -> Accelerator.GPU
//            availableAccelerators.contains(Accelerator.NPU) -> Accelerator.NPU
            availableAccelerators.contains(Accelerator.CPU) -> Accelerator.CPU
            else -> Accelerator.NONE
        }
        return acceleratorToUse
    }
}
