package com.jozeftvrdy.solver.sudoku.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


enum class SudokuInputTileType {
    FixedValue,
    SolvedValue,
}

@Parcelize
data class SudokuTileValueInputModel(
    val value: Int,
    val position: SudokuPosition,
    val tileType: SudokuInputTileType,
): Parcelable