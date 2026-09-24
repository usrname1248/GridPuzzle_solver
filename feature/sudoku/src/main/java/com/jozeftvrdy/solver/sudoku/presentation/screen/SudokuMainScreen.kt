package com.jozeftvrdy.solver.sudoku.presentation.screen

import androidx.compose.runtime.Composable
import com.jozeftvrdy.solver.sudoku.presentation.theme.SudokuTheme

@Composable
fun SudokuMainScreen(
    onBackClick: () -> Unit
) {
    SudokuTheme {
        SudokuScreen(onBackClick)
    }
}