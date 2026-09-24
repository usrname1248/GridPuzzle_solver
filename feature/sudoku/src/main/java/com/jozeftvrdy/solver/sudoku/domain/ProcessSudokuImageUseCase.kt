package com.jozeftvrdy.solver.sudoku.domain

import android.graphics.Bitmap
import com.jozeftvrdy.solver.core.imageprocessing.ImageProcessor.cropEdges
import com.jozeftvrdy.solver.core.imageprocessing.ImageProcessor.cropFromCenter
import com.jozeftvrdy.solver.core.imageprocessing.ImageProcessor.padWithWhite
import com.jozeftvrdy.solver.core.imageprocessing.ImageProcessor.splitIntoImagesAndDo
import com.jozeftvrdy.solver.core.imageprocessing.ImageProcessor.tileHorizontally
import com.jozeftvrdy.solver.core.imageprocessing.ImageProcessor.toBlackAndWhite
import com.jozeftvrdy.solver.core.imageprocessing.TextRecognizerProcessor
import com.jozeftvrdy.solver.sudoku.model.SudokuPosition
import com.jozeftvrdy.solver.sudoku.util.DefaultDispatcherProvider
import com.jozeftvrdy.solver.sudoku.util.DispatcherProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class ProcessSudokuImageUseCase(
    private val textRecognizerProcessor: TextRecognizerProcessor,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) {
    suspend operator fun invoke(
        capturedPicture: Bitmap
    ) : Map<SudokuPosition, Int> = withContext(dispatchers.default) {
            val croppedPicture = capturedPicture.width.times(0.9f).roundToInt().let { newWidth ->
                capturedPicture.cropFromCenter(
                    newWidth,
                    newWidth
                )
            }
            croppedPicture.splitIntoImagesAndDo(
                xCount = 9,
                yCount = 9,
            ) { splitImage, x, y ->
                async {

                    textRecognizerProcessor.recognizeText(
                        splitImage
                            .cropEdges(0.08f)
                            .toBlackAndWhite()
                            .padWithWhite(1.05f)
                            .tileHorizontally(4)
                            .padWithWhite(2f)
                    )
                        .getOrNull()
                        ?.let { stringValue ->
                            val numbersWithCount = stringValue.filter { char ->
                                (char.code >=  '1'.code && char.code <= '9'.code)
                            }.groupBy { char ->
                                val number = char.code - '1'.code + 1
                                number
                            }.mapValues { it.value.size }

                            numbersWithCount.maxByOrNull { pair ->
                                pair.value
                            }?.let {
                                SudokuPosition(x + 1, y + 1) to it.key
                            }
                        }
                }
            }.map { it.result }
                .awaitAll()
                .filterNotNull()
                .toMap()
        }
    }