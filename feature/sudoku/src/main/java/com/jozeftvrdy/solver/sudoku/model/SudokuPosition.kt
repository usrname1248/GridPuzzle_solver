package com.jozeftvrdy.solver.sudoku.model

import androidx.annotation.IntRange

data class SudokuPosition(
    @field:IntRange(from = 1, to = 9)
    val x: Int,
    @field:IntRange(from = 1, to = 9)
    val y: Int,
) {
    init {
        require(x in 1..9) { "x must be between 1 and 9, but was $x" }
        require(y in 1..9) { "y must be between 1 and 9, but was $y" }
    }
}