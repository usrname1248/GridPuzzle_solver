package com.jozeftvrdy.solver.sudoku.model

sealed class SudokuSolvedTileType {
    data object FilledTile: SudokuSolvedTileType()
    data class RuledOutTile(
        val becauseOfTile: SudokuPosition
    ): SudokuSolvedTileType()
    data object SolvedTile: SudokuSolvedTileType()
}