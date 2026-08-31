package com.jozeftvrdy.solver.sudoku.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

@Composable
fun SudokuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val sudokuColors = if (darkTheme) DarkSudokuColors else LightSudokuColors

    CompositionLocalProvider(
        LocalSudokuColors provides sudokuColors
    ) {
        MaterialTheme(
            typography = SudokuTypography,
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
