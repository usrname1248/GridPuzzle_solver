package com.jozeftvrdy.solver.sudoku.model

import android.os.Parcelable
import androidx.annotation.IntRange
import kotlinx.parcelize.Parcelize

@Parcelize
data class SudokuPosition(
    @field:IntRange(from = 1)
    val x: Int,
    @field:IntRange(from = 1)
    val y: Int,
): Parcelable {
    init {
        require(x >= 1) { "x must be at least 1, but was $x" }
        require(y >= 1) { "y must be at least 1, but was $y" }
    }
}