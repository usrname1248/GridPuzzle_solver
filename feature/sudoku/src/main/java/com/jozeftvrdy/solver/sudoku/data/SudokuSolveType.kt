package com.jozeftvrdy.solver.sudoku.data

internal sealed class SudokuSolveType {
    data class TheOnlyUnsolvedTileInArea(
        val area: SudokuArea
    ): SudokuSolveType()

    data class TheOnlyOptionInArea(
        val area: SudokuArea
    ): SudokuSolveType()

    data object TheOnlyOptionInPlace: SudokuSolveType()

}