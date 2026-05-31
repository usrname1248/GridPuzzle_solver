package com.jozeftvrdy.solver.sudoku.model

sealed class SudokuTileValueFullSolvedModel {
    abstract val position: SudokuPosition
    abstract val value: Int

    data class FixedTileValue(
        override val value: Int,
        override val position: SudokuPosition,
    ) : SudokuTileValueFullSolvedModel()

    data class SolvedTileValue(
        override val value: Int,
        override val position: SudokuPosition,
    ) : SudokuTileValueFullSolvedModel()
}