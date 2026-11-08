package com.jozeftvrdy.solver.sudoku.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import com.jozeftvrdy.solver.core.ui.theme.GridPuzzleSolverTheme

@Composable
fun SudokuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val sudokuColors = if (darkTheme) DarkSudokuColors else LightSudokuColors

    CompositionLocalProvider(
        LocalSudokuColors provides sudokuColors
    ) {
        GridPuzzleSolverTheme(
            darkTheme = darkTheme,
            content = content
        )
    }
}

object SudokuTheme {
    val colors: SudokuColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSudokuColors.current
}
