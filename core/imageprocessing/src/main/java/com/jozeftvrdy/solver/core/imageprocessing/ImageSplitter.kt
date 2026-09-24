package com.jozeftvrdy.solver.core.imageprocessing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import androidx.core.graphics.set

object ImageProcessor {
    data class SplitResult<R>(
        val xIndex: Int,
        val yIndex: Int,
        val result: R,
    )

    fun Bitmap.cropFromCenter(
        newWidth: Int,
        newHeight: Int,
    ): Bitmap {
        val xStart = (this.width - newWidth) /2
        val yStart = (this.height - newHeight) /2
        return Bitmap.createBitmap(this, xStart, yStart, newWidth, newHeight)
    }

    fun <R> Bitmap.splitIntoImagesAndDo(
        xCount: Int,
        yCount: Int,
        skipThisPart: (xIndex: Int, yIndex: Int) -> Boolean = { _,_ -> false },
        callback: (splitBitmap: Bitmap, xIndex: Int, yIndex: Int) -> R
    ): List<SplitResult<R>> {
        val singleImageWidth = this.width / xCount
        val singleImageHeight = this.height / yCount

        return buildList {
            for (yIndex in 0 until yCount) {
                for (xIndex in 0 until xCount) {
                    if (skipThisPart(xIndex, yIndex)) {
                        continue
                    }

                    val xStart = xIndex * singleImageWidth
                    val yStart = yIndex * singleImageHeight

                    // Slice an individual cell out of the master square grid
                    val splitBitmap = Bitmap.createBitmap(this@splitIntoImagesAndDo, xStart, yStart, singleImageWidth, singleImageHeight)

                    callback(
                        splitBitmap,
                        xIndex,
                        yIndex,
                    ).also { result ->
                        add(
                            SplitResult(
                                xIndex = xIndex,
                                yIndex = yIndex,
                                result = result,
                            )
                        )
                    }
                }
            }
        }
    }

    fun Bitmap.cropEdges(percentage: Float): Bitmap {
        require(
            percentage in 0f..0.6f
        )

        val insetX = (this.width * percentage).toInt()
        val insetY = (this.height * percentage).toInt()
        val newWidth = this.width - (insetX * 2)
        val newHeight = this.height - (insetY * 2)

        return Bitmap.createBitmap(this, insetX, insetY, newWidth, newHeight)
    }

    fun Bitmap.toBlackAndWhite(threshold: Int = 100): Bitmap {
        val result = createBitmap(this.width, this.height)
        for (x in 0 until this.width) {
            for (y in 0 until this.height) {
                val pixel = this[x, y]
                val red = (pixel shr 16) and 0xff
                val green = (pixel shr 8) and 0xff
                val blue = pixel and 0xff
                val gray = (red * 0.299 + green * 0.587 + blue * 0.114).toInt()

                val newColor = if (gray < threshold) Color.BLACK else Color.WHITE
                result[x, y] = newColor
            }
        }
        return result
    }

    fun Bitmap.padWithWhite(multiplier: Float): Bitmap {
        require(
            multiplier >= 1f
        )
        // multiplier = 2f -> new image is 2x the size of the original
        val newWidth = (this.width * multiplier).toInt().coerceAtLeast(1)
        val newHeight = (this.height * multiplier).toInt().coerceAtLeast(1)

        val output = createBitmap(newWidth, newHeight)

        val canvas = Canvas(output)
        canvas.drawColor(Color.WHITE)

        // Center the original digit in the new larger frame
        val left = (newWidth - this.width) / 2f
        val top = (newHeight - this.height) / 2f
        canvas.drawBitmap(this, left, top, null)

        return output
    }

    fun Bitmap.tileHorizontally(count: Int): Bitmap {
        val output = createBitmap(this.width * count, this.height)
        val canvas = Canvas(output)

        for (i in 0 until count) {
            canvas.drawBitmap(this, (i * this.width).toFloat(), 0f, null)
        }

        return output
    }

}