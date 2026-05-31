package com.jozeftvrdy.solver.sudoku.model


enum class SudokuInputTileType {
    FixedValue,
    SolvedValue,
}

data class SudokuTileValueInputModel(
    val value: Int,
    val position: SudokuPosition,
    val tileType: SudokuInputTileType,
)