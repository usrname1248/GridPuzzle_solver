package dev.fezojko.solver.core.imageprocessing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import androidx.annotation.FloatRange
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import androidx.core.graphics.set
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

object ImageProcessor {

    fun Bitmap.cropToSquare() : Bitmap = when {
        this.width == this.height -> this
        this.width > this.height -> {
            val newWidth = this.height
            val xStart = (this.width - newWidth) / 2
            Bitmap.createBitmap(this, xStart, 0, newWidth, this.height)
        }
        else -> {
            val newHeight = this.width
            val yStart = (this.height - newHeight) /2
            Bitmap.createBitmap(this, 0, yStart, this.width, newHeight)
        }
    }

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
    ): List<R> {
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

                    add (
                        callback(
                            splitBitmap,
                            xIndex,
                            yIndex,
                        )
                    )
                }
            }
        }
    }

    fun Bitmap.cropEdges(percentage: Float): Bitmap {
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

    fun Bitmap.addWhiteToBeSquare(): Bitmap {
        val newSize = max(this.width, this.height)

        val output = createBitmap(newSize, newSize)

        val canvas = Canvas(output)
        canvas.drawColor(Color.WHITE)

        // Center the original digit in the new larger frame
        val left = (newSize - this.width) / 2f
        val top = (newSize - this.height) / 2f
        canvas.drawBitmap(this, left, top, null)

        return output
    }

    fun Bitmap.padWithWhite(multiplier: Float): Bitmap = this.padWithWhite(multiplier, multiplier)
    fun Bitmap.padWithWhite(widthMultiplier: Float = 1f, heightMultiplier: Float = 1f): Bitmap {
        require(
            widthMultiplier >= 1f
        )
        require(
            heightMultiplier >= 1f
        )
        // multiplier = 2f -> new image is 2x the size of the original
        val newWidth = (this.width * widthMultiplier).roundToInt().coerceAtLeast(1)
        val newHeight = (this.height * heightMultiplier).roundToInt().coerceAtLeast(1)

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

    fun Bitmap.tileHorizontallyWith(text: String, repeatCount: Int = 1): Bitmap {
        val textBitmaps = text.map { drawTextToBitmap(it.toString(), height.times(3).div(4), height) }
        return buildList {
            repeat(repeatCount) {
                add(this@tileHorizontallyWith)
            }
            addAll(textBitmaps)
            repeat(repeatCount) {
                add(this@tileHorizontallyWith)
            }
        }.joinHorizontally()
    }

    fun List<Bitmap>.unifyHeight(): List<Bitmap> {
        val maxHeight = this.maxOf { it.height }

        return this.map { bitmap ->
            if (bitmap.height == maxHeight) {
                bitmap
            } else {
                bitmap.padWithWhite(heightMultiplier = maxHeight.div(bitmap.height.toFloat()))
            }
        }
    }

    fun List<Bitmap>.joinHorizontally(): Bitmap {
        require(isNotEmpty()) { "Bitmap list cannot be empty" }
        require(this.all { (it.height - this.first().height) == 0 }) { "Bitmaps cannot have different height" }
        val height = first().height
        val totalWidth = sumOf { it.width }

        val output = createBitmap(totalWidth, height)
        val canvas = Canvas(output)

        var currentX = 0f
        for (bitmap in this) {
            canvas.drawBitmap(bitmap, currentX, 0f, null)
            currentX += bitmap.width
        }

        return output
    }


    fun drawTextToBitmap(text: String, width: Int, height: Int): Bitmap {
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)

        // Background color
        canvas.drawColor(Color.WHITE)

        val paint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        var textSize = height.toFloat()
        paint.textSize = textSize

        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)

        if (bounds.height() > 0) {
            textSize *= (height.toFloat() / bounds.height()) * 0.9f
            paint.textSize = textSize
            paint.getTextBounds(text, 0, text.length, bounds)
        }

        val x = (width / 2f)
        val y = (height / 2f) - bounds.exactCenterY()

        canvas.drawText(text, x, y, paint)
        return bitmap
    }

    fun Int.isDarkerThan(
        @androidx.annotation.IntRange(from = 0, to = 255)
        threshold: Int
    ): Boolean {
        val red = (this shr 16) and 0xff
        val green = (this shr 8) and 0xff
        val blue = this and 0xff
        val gray = (red * 0.299 + green * 0.587 + blue * 0.114).toInt()
        return gray < threshold
    }

    fun Int.isGrayScale(
        @FloatRange(
            from = 0.0,
            to = 1.0
        )
        tolerance: Float = 0.05f
    ) : Boolean {

        val red = (this shr 16) and 0xff
        val green = (this shr 8) and 0xff
        val blue = this and 0xff

        val rgDiff = abs(red - green).toFloat() / 255f
        val rbDiff = abs(red - blue).toFloat() / 255f
        val gbDiff = abs(green - blue).toFloat() / 255f

        return rgDiff <= tolerance && rbDiff <= tolerance && gbDiff <= tolerance
    }
}