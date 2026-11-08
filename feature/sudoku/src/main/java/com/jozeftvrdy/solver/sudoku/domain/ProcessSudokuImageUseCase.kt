package com.jozeftvrdy.solver.sudoku.domain

import android.graphics.Bitmap
import com.jozeftvrdy.solver.core.datastore.`interface`.SudokuPreferencesRepository
import com.jozeftvrdy.solver.core.datastore.model.HandwrittenRecognizerModel
import com.jozeftvrdy.solver.core.datastore.model.SudokuImageProcessingType
import com.jozeftvrdy.solver.core.imageprocessing.ImageContentExtractor.getCenterContentThat
import com.jozeftvrdy.solver.core.imageprocessing.ImageProcessor.addWhiteToBeSquare
import com.jozeftvrdy.solver.core.imageprocessing.ImageProcessor.cropEdges
import com.jozeftvrdy.solver.core.imageprocessing.ImageProcessor.cropToSquare
import com.jozeftvrdy.solver.core.imageprocessing.ImageProcessor.isDarkerThan
import com.jozeftvrdy.solver.core.imageprocessing.ImageProcessor.isGrayScale
import com.jozeftvrdy.solver.core.imageprocessing.ImageProcessor.joinHorizontally
import com.jozeftvrdy.solver.core.imageprocessing.ImageProcessor.padWithWhite
import com.jozeftvrdy.solver.core.imageprocessing.ImageProcessor.splitIntoImagesAndDo
import com.jozeftvrdy.solver.core.imageprocessing.ImageProcessor.tileHorizontallyWith
import com.jozeftvrdy.solver.core.imageprocessing.ImageProcessor.toBlackAndWhite
import com.jozeftvrdy.solver.core.imageprocessing.ImageProcessor.unifyHeight
import com.jozeftvrdy.solver.core.textrecognition.HandwrittenNumbersRecognizer
import com.jozeftvrdy.solver.core.textrecognition.PrintedTextRecognizer
import com.jozeftvrdy.solver.sudoku.model.SudokuInputTileType
import com.jozeftvrdy.solver.sudoku.model.SudokuPosition
import com.jozeftvrdy.solver.sudoku.model.SudokuTileValueInputModel
import com.jozeftvrdy.solver.sudoku.util.DefaultDispatcherProvider
import com.jozeftvrdy.solver.sudoku.util.DispatcherProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

class ProcessSudokuImageUseCase(
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider(),
    private val printedTextRecognizer: PrintedTextRecognizer,
    private val handwrittenTextRecognizer: HandwrittenNumbersRecognizer,
    private val preferences: SudokuPreferencesRepository,
) {
    suspend operator fun invoke(
        capturedPicture: Bitmap,
    ): List<SudokuTileValueInputModel> = invoke(
        capturedPicture = capturedPicture,
        handwrittenModel = preferences.getHandwrittenRecognizerModel(),
        type = preferences.getImageProcessingType()
    )

    suspend operator fun invoke(
        capturedPicture: Bitmap,
        handwrittenModel: HandwrittenRecognizerModel,
        type: SudokuImageProcessingType,
    ): List<SudokuTileValueInputModel> = when (type) {
        SudokuImageProcessingType.FastestLeastReliable -> ::recognizeHandwrittenFirst
        SudokuImageProcessingType.Average -> ::recognizeChunked
        SudokuImageProcessingType.SlowestMostReliable -> ::recognizeIndividually
    }.invoke(
        capturedPicture,
        handwrittenModel
    )


    private suspend fun recognizeIndividually(
        capturedPicture: Bitmap,
        handwrittenModel: HandwrittenRecognizerModel
    ): List<SudokuTileValueInputModel> = withContext(dispatchers.default.limitedParallelism(4)) {
        capturedPicture
            .cropToSquare()
            .cropEdges(0.05f)
            .toBlackAndWhite()
            .splitIntoImagesAndDo(xCount = 9, yCount = 9) { splitImage, x, y ->
                async {
                    val centeredContent = splitImage.getCenterContentThat {
                        it.isGrayScale(.1f) && it.isDarkerThan(71)
                    }

                    if (centeredContent == null) {
                        return@async null
                    }

                    printedTextRecognizer
                        .recognizeMostProbableDigit(
                            centeredContent
                                .toBlackAndWhite()
                                .tileHorizontallyWith("ABA", 2)
                                .padWithWhite(2.5f)
                        )
                        ?.takeIf { it > 0 }
                        ?.also {
                            return@async SudokuTileValueInputModel(
                                value = it,
                                SudokuPosition(x + 1, y + 1),
                                tileType = SudokuInputTileType.FixedValue
                            )
                        }

                    centeredContent
                        .addWhiteToBeSquare()
                        .padWithWhite(1.4f).let { modifiedCenteredContent ->
                            handwrittenTextRecognizer.recognize(
                                modifiedCenteredContent,
                                model = handwrittenModel,
                                requiredProbability = 0.9f,
                            ).getOrNull()?.takeIf { it > 0 }
                        }?.also {
                            return@async SudokuTileValueInputModel(
                                value = it,
                                SudokuPosition(x + 1, y + 1),
                                tileType = SudokuInputTileType.SolvedValue
                            )
                        }

                    return@async null
                }
            }
            .awaitAll()
            .filterNotNull()
    }

    private suspend fun recognizeChunked(
        capturedPicture: Bitmap,
        handwrittenModel: HandwrittenRecognizerModel,
    ): List<SudokuTileValueInputModel> = withContext(dispatchers.default.limitedParallelism(4)) {
        capturedPicture
            .cropToSquare()
            .cropEdges(0.05f) // we crop 5% of each side, we want just middle 90%
            .toBlackAndWhite()
            .splitIntoImagesAndDo(
                xCount = 9,
                yCount = 9,
                callback = ::Triple
            )
            .mapNotNull { imageData ->
                Triple(
                    imageData.first.getCenterContentThat {
                        it.isGrayScale(.1f) && it.isDarkerThan(71)
                    } ?: return@mapNotNull null,
                    imageData.second,
                    imageData.third,
                )
            }
            .chunked(3)
            .map { centeredContentTriples ->
                async {
                    run printedTextRecognition@{
                        val recognizedString = printedTextRecognizer.recognizeText(
                            centeredContentTriples.map {
                                it.first
                            }
                                .unifyHeight()
                                .joinHorizontally()
                                .tileHorizontallyWith("AA", 2)
                                .padWithWhite(2.5f)
                        ) ?: return@printedTextRecognition
                        val recognizedCharsCount =
                            recognizedString.groupingBy { it }.eachCount()
                        val filteredString = recognizedString.filter { char ->
                            char in '1'..'9' && (recognizedCharsCount[char] ?: 0) >= centeredContentTriples.size
                        }
                        val mostUsedFrequency = filteredString.findMostUsedFreq(
                            frequencyMinimalLength = centeredContentTriples.size,
                            requiredRepetitionCount = centeredContentTriples.size
                        ) ?: return@printedTextRecognition

                        if (mostUsedFrequency.length != centeredContentTriples.size) return@printedTextRecognition

                        return@async List(mostUsedFrequency.length) { index ->
                            SudokuTileValueInputModel(
                                value = mostUsedFrequency[index].digitToInt(10),
                                SudokuPosition(
                                    centeredContentTriples[index].second + 1,
                                    centeredContentTriples[index].third + 1
                                ),
                                tileType = SudokuInputTileType.FixedValue
                            )
                        }
                    }

                    return@async centeredContentTriples.mapNotNull { (centeredContent, x, y) ->
                        centeredContent
                            .addWhiteToBeSquare()
                            .padWithWhite(1.4f)
                            .let { modifiedCenteredContent ->
                                handwrittenTextRecognizer.recognize(
                                    modifiedCenteredContent,
                                    model = handwrittenModel,
                                    requiredProbability = 0.9f,
                                ).getOrNull()?.takeIf { it > 0 }
                            }?.let {
                                SudokuTileValueInputModel(
                                    value = it,
                                    SudokuPosition(x + 1, y + 1),
                                    tileType = SudokuInputTileType.SolvedValue
                                )
                            }
                    }
                }
            }
            .awaitAll()
            .flatten()
    }

    private suspend fun recognizeHandwrittenFirst(
        capturedPicture: Bitmap,
        handwrittenModel: HandwrittenRecognizerModel,
    ): List<SudokuTileValueInputModel> = withContext(dispatchers.default) {
        capturedPicture
            .cropToSquare()
            .cropEdges(0.05f)
            .toBlackAndWhite()
            .splitIntoImagesAndDo(xCount = 9, yCount = 9) { splitImage, x, y ->
                async {
                    val centeredContent = splitImage.getCenterContentThat {
                        it.isGrayScale(.1f) && it.isDarkerThan(71)
                    }

                    if (centeredContent == null) {
                        return@async null
                    }

                    centeredContent
                        .addWhiteToBeSquare()
                        .padWithWhite(1.4f)
                        .let { modifiedCenteredContent ->
                            handwrittenTextRecognizer.recognize(
                                modifiedCenteredContent,
                                model = handwrittenModel,
                                requiredProbability = 0.9f,
                            ).getOrNull()?.takeIf { it > 0 }
                        }
                        ?.also {
                            return@async SudokuTileValueInputModel(
                                value = it,
                                SudokuPosition(x + 1, y + 1),
                                tileType = SudokuInputTileType.SolvedValue
                            )
                        }



                    printedTextRecognizer.recognizeMostProbableDigit(
                        centeredContent
                            .toBlackAndWhite()
                            .tileHorizontallyWith("ABA", 2)
                            .padWithWhite(2.5f)
                    )
                        ?.takeIf { it > 0 }
                        ?.also {
                            return@async SudokuTileValueInputModel(
                                value = it,
                                SudokuPosition(x + 1, y + 1),
                                tileType = SudokuInputTileType.FixedValue
                            )
                        }

                    return@async null
                }
            }
            .awaitAll()
            .filterNotNull()
    }

    fun String.findMostUsedFreq(
        frequencyMinimalLength: Int,
        requiredRepetitionCount: Int
    ): String? {
        if (this.length < frequencyMinimalLength) return null

        return this
            .windowed(size = frequencyMinimalLength, step = 1)
            .groupingBy { it }
            .eachCount()
            .filter { it.value >= requiredRepetitionCount }
            .maxByOrNull { it.value }
            ?.key
    }
}
