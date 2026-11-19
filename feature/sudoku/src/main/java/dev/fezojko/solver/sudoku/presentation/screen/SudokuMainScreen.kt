package dev.fezojko.solver.sudoku.presentation.screen

import androidx.compose.runtime.Composable
import dev.fezojko.solver.sudoku.presentation.theme.SudokuTheme

@Composable
fun SudokuMainScreen(
    onBackClick: () -> Unit
) {
    SudokuTheme {
            // For test purposes
//        SudokuPerformanceScreen()
        SudokuScreen(onBackClick)
    }
}