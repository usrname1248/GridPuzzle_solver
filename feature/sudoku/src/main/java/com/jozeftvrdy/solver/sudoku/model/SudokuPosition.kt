package com.jozeftvrdy.solver.sudoku.model

import androidx.annotation.IntRange

data class SudokuPosition(
    @field:IntRange(from = 1)
    val x: Int,
    @field:IntRange(from = 1)
    val y: Int,
) {
    init {
        require(x >= 1) { "x must be at least 1, but was $x" }
        require(y >= 1) { "y must be at least 1, but was $y" }
    }
}