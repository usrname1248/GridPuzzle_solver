package com.jozeftvrdy.solver.sudoku.presentation.component

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import com.jozeftvrdy.solver.sudoku.model.SudokuInputTileType

private const val normalLineWidthConstant = 0.02f
const val boldLineWidthConstant = 0.1f
internal fun Dp.getNormalLineWidth() = this.times(normalLineWidthConstant).coerceAtLeast(1.dp)
internal fun Dp.getBoldLineWidth() = this.times(boldLineWidthConstant).coerceAtLeast(2.dp)

data class SudokuPositionValuePresentationModel(
    val value: Int,
    val inputTileType: SudokuInputTileType,
)

enum class BorderSide {
    Top,
    Bottom,
    Left,
    Right,
}