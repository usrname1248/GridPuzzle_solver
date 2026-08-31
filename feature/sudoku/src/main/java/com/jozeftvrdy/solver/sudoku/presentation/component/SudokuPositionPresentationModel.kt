package com.jozeftvrdy.solver.sudoku.presentation.component

import androidx.compose.ui.unit.Dp
import com.jozeftvrdy.solver.sudoku.model.SudokuInputTileType

private const val normalLineWidthConstant = 0.02f
const val boldLineWidthConstant = 0.1f
internal fun Dp.getNormalLineWidth() = this.times(normalLineWidthConstant)
internal fun Dp.getBoldLineWidth() = this.times(boldLineWidthConstant)

internal data class SudokuPositionValuePresentationModel(
    val value: Int,
    val inputTileType: SudokuInputTileType,
    val isErrorValue: Boolean = false,
)

internal enum class BorderSide {
    Top,
    Bottom,
    Left,
    Right,
}