package com.jozeftvrdy.solver.sudoku.model

data class PartiallySolvedSudokuResult(
    val value: Int,
    val position: SudokuPosition,
    val reason: SudokuSolvedTileReason,
): SudokuResult()