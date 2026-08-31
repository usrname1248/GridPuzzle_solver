package com.jozeftvrdy.solver.sudoku.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class SudokuColors(
    val screenBackground: Color,
    val sudokuBorder: Color,
    val tileBackground: Color,
    val specialTileBackground: Color,
    val textValueColor: Color,
    val textValueErrorColor: Color,
    val areaErrorBackground: Color,
    val markedAsReasonTileBackground: Color,
    val markedAsAreaTileBackground: Color,
    val markedAsAddedTileBackground: Color,
)

val LocalSudokuColors = staticCompositionLocalOf<SudokuColors> {
    error("No SudokuColors provided")
}

val LightSudokuColors = SudokuColors(
    screenBackground = Color(0xFFF5F5F5),
    sudokuBorder = Color(0xFF212121),
    tileBackground = Color.White,
    specialTileBackground = Color(0xFFE3F2FD),
    textValueColor = Color(0xFF212121),
    textValueErrorColor = Color(0xFFB71C1C),
    areaErrorBackground = Color(0xFFFFEBEE),
    markedAsReasonTileBackground = Color(0xFFFFF9C4),
    markedAsAreaTileBackground = Color(0xFFE8F5E9),
    markedAsAddedTileBackground = Color(0xFFA5D6A7),
)

val DarkSudokuColors = SudokuColors(
    screenBackground = Color(0xFF121212),
    sudokuBorder = Color(0xFFE0E0E0),
    tileBackground = Color(0xFF1E1E1E),
    specialTileBackground = Color(0xFF0D47A1).copy(alpha = 0.2f),
    textValueColor = Color(0xFFE0E0E0),
    textValueErrorColor = Color(0xFFEF9A9A),
    areaErrorBackground = Color(0xFF330E0E),
    markedAsReasonTileBackground = Color(0xFF333016),
    markedAsAreaTileBackground = Color(0xFF1B5E20),
    markedAsAddedTileBackground = Color(0xFF2E7D32),
)
