package dev.fezojko.solver.core.imageprocessing

import android.graphics.Bitmap
import androidx.annotation.FloatRange
import androidx.core.graphics.get
import kotlin.math.min
import kotlin.math.roundToInt

object ImageContentExtractor {
    internal enum class Side {
        Top,
        Right,
        Bottom,
        Left;
    }

    internal data class SideCalculationResult(
        val distanceFound: Int?
    )

    internal class SideValues(
        private val maxDistance: Int,
        private val maxEmptyDistance: Int = 6,
        private val bitmap: Bitmap,
        private val startingPoint: Int,
        private val side: Side,
    ) {
        var distance: Int = 1
            private set

        var lastFoundDistance = 0
            private set
        var shouldBeSkipped = false
            private set

        fun getPixelIndex(distance: Int = this.distance): Int {
            val operation: Int.(Int) -> Int = when (side) {
                Side.Left,
                Side.Top -> Int::minus
                Side.Right,
                Side.Bottom -> Int::plus
            }

            return startingPoint.operation(distance)
        }

        fun calculate(
            touchingSides: Pair<SideValues, SideValues>,
            condition: (Int) -> Boolean
        ): SideCalculationResult {
            if (this.shouldBeSkipped) return SideCalculationResult(null)

            val diagonalDirectionPixelRange = touchingSides.let {
                val pixelIndex1 = it.first.getPixelIndex()
                val pixelIndex2 = it.second.getPixelIndex()
                if (pixelIndex2 > pixelIndex1) IntRange(pixelIndex1, pixelIndex2) else IntRange(pixelIndex1, pixelIndex2)
            }
            val distanceRange = IntRange(lastFoundDistance + 1, distance)
            // we go reversed, so furthest distance are checked first, lastFoundDistance+1 last
            val foundDistance = distanceRange.reversed().firstOrNull { distanceToCheck ->
                val pixelIndex = getPixelIndex(distanceToCheck)
                diagonalDirectionPixelRange.any { otherDirectionPixelIndex ->
                    condition(
                        when (side) {
                            Side.Top,
                            Side.Bottom -> bitmap[otherDirectionPixelIndex, pixelIndex]
                            Side.Right,
                            Side.Left -> bitmap[pixelIndex, otherDirectionPixelIndex]

                        }
                    )
                }
            }


            return SideCalculationResult(
                foundDistance
            )
        }

        fun evaluate(distanceFound: SideCalculationResult): Boolean {
            if (shouldBeSkipped) return false

            distanceFound.distanceFound?.let {
                lastFoundDistance = it
            }

            val maxDistancePass = (distance - lastFoundDistance < maxEmptyDistance)
            val canIncreaseDistance = distance + 1 <= maxDistance
            var result = false

            if (maxDistancePass && canIncreaseDistance) {
                distance++
                result = true
            } else if (!canIncreaseDistance) {
                shouldBeSkipped = true
            }

            return result
        }
    }

    fun Bitmap.getCenterContentThat(
        @FloatRange(from = 0.0, to = 1.0)
        additionalContentRatio: Float = 0.15f,
        requirement: (Int) -> Boolean,
    ): Bitmap? {
        val cacheMap = mutableMapOf<Int, Boolean>()
        return getCenterContentThatInternal(
            additionalContentRatio = additionalContentRatio
        ) {
            cacheMap.getOrPut(
                it
            ) {
                requirement(it)
            }
        }
    }

    private fun Bitmap.getCenterContentThatInternal(
        @FloatRange(from = 0.0, to = 1.0)
        additionalContentRatio: Float = 0.15f,
        requirement: (Int) -> Boolean,
    ): Bitmap? {
        val (startX, startY) = findPixelNearestCenterThat(requirement)?:return null

        val topSideValues = SideValues(
            maxDistance = startY,
            bitmap = this,
            startingPoint = startY,
            side = Side.Top
        )

        val bottomSideValues = SideValues(
            maxDistance = height - startY - 1,
            bitmap = this,
            startingPoint = startY,
            side = Side.Bottom
        )

        val leftSideValues = SideValues(
            maxDistance = startX,
            bitmap = this,
            startingPoint = startX,
            side = Side.Left
        )

        val rightSideValues = SideValues(
            maxDistance = width - startX - 1,
            bitmap = this,
            startingPoint = startX,
            side = Side.Right
        )

        var anyChange : Boolean
        do {
            // topLine
            val topFound = topSideValues.calculate(
                leftSideValues to rightSideValues,
                requirement
            )

            // bottomLine
            val bottomFound = bottomSideValues.calculate(
                leftSideValues to rightSideValues,
                requirement
            )

            // leftLane
            val leftFound = leftSideValues.calculate(
                topSideValues to bottomSideValues,
                requirement
            )

            // rightLane
            val rightFound = rightSideValues.calculate(
                topSideValues to bottomSideValues,
                requirement
            )

            anyChange = topSideValues.evaluate(topFound) or
                    bottomSideValues.evaluate(bottomFound) or
                    leftSideValues.evaluate(leftFound) or
                    rightSideValues.evaluate(rightFound)
        } while (anyChange)

        val emptySpacePx = min(
            topSideValues.lastFoundDistance + bottomSideValues.lastFoundDistance,
            leftSideValues.lastFoundDistance + rightSideValues.lastFoundDistance
        ).times(additionalContentRatio).roundToInt().coerceAtLeast(4)

        val bitmapStartX = (startX - leftSideValues.lastFoundDistance - emptySpacePx).coerceAtLeast(0)
        val bitmapStartY = (startY - topSideValues.lastFoundDistance - emptySpacePx).coerceAtLeast(0)
        val bitmapWidth = (startX + rightSideValues.lastFoundDistance + emptySpacePx).coerceAtMost(width) - bitmapStartX
        val bitmapHeight = (startY + bottomSideValues.lastFoundDistance + emptySpacePx).coerceAtMost(height) - bitmapStartY
        if (bitmapHeight < 10 || bitmapWidth < 10) return null
        return Bitmap.createBitmap(
            this,
            bitmapStartX,
            bitmapStartY,
            bitmapWidth,
            bitmapHeight
        )
    }

    private fun Bitmap.findPixelNearestCenterThat(
        requirement: (pixel: Int) -> Boolean
    ) : Pair<Int, Int>? {
        val midX = width.div(2)
        val midY = height.div(2)
        var dx = 0
        var dy = 0
        val maxDx = width.div(7)
        val maxDy = height.div(7)
        do {
            // topLine + bottomLine
            if (dx < maxDx) {
                IntRange(
                    start = midX - dx,
                    endInclusive = midX + dx
                ).forEach { x ->
                    // topLine
                    val topPixel = this[x, midY - dy]
                    if (requirement(topPixel)) return x to midY - dy

                    // bottomLine if different
                    if (dy > 0) {
                        val bottomPixel = this[x, midY + dy]
                        if (requirement(bottomPixel)) return x to midY + dy
                    }
                }
            }

            // left + right line
            if (dy in 2..<maxDy) {
                IntRange(
                    start = midY - dy + 1,
                    endInclusive = midY + dy - 1,
                ).forEach { y ->
                    val leftPixel = this[midX - dx, y]
                    if (requirement(leftPixel)) return midX - dx to y

                    // bottomLine if different
                    if (dx > 0) {
                        val bottomPixel = this[midX + dx, y]
                        if (requirement(bottomPixel)) return midX + dx to y
                    }
                }
            }

            dx++
            dy++
        } while (dy<maxDy || dx < maxDx)

        return null
    }
}