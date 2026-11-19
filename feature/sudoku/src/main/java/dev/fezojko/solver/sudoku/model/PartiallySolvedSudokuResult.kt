package dev.fezojko.solver.sudoku.model

data class PartiallySolvedSudokuResult(
    val value: Int,
    val position: SudokuPosition,
    val reason: SudokuSolvedTurnReason,
): SudokuResult()