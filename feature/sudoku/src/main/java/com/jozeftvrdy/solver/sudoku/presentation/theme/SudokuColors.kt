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
    val tileErrorBackground: Color,
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
    tileErrorBackground = Color(0xFFFC6969),
    areaErrorBackground = Color(0xFFFFEBEE),
    markedAsAreaTileBackground = Color(0xFFFFF9C4),
    markedAsReasonTileBackground = Color(0xFFC5EEC8),
    markedAsAddedTileBackground = Color(0xFF94CE96),
)

val DarkSudokuColors = SudokuColors(
    screenBackground = Color(0xFF121212),
    sudokuBorder = Color(0xFFE0E0E0),
    tileBackground = Color(0xFF1E1E1E),
    specialTileBackground = Color(0xFF0D47A1).copy(alpha = 0.2f),
    textValueColor = Color(0xFFE0E0E0),
    tileErrorBackground = Color(0xFFB01919),
    areaErrorBackground = Color(0xFF330E0E),
    markedAsReasonTileBackground = Color(0xFF304127),
    markedAsAreaTileBackground = Color(0xFF383623),
    markedAsAddedTileBackground = Color(0xFF2E7D32),
)
