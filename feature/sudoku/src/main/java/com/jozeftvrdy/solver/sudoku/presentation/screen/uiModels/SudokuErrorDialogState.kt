package com.jozeftvrdy.solver.sudoku.presentation.screen.uiModels

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jozeftvrdy.solver.sudoku.R

sealed class SudokuErrorDialogState {
    sealed class Visible: SudokuErrorDialogState() {

        data object DuplicateInputValue: Visible() {
            @Composable
            override fun getDialogText(): String =
                stringResource(R.string.sudoku_dialog_text_input_with_errors)
        }

        data object GuessedValue: Visible() {
            @Composable
            override fun getDialogText(): String =
                stringResource(R.string.sudoku_dialog_text_guessed_value)
        }

        data object NoSolution: Visible() {
            @Composable
            override fun getDialogText(): String =
                stringResource(R.string.sudoku_dialog_text_no_solution)
        }


        @Composable
        fun getDialogTitle(): String =
            stringResource(R.string.sudoku_dialog_title)

        @Composable
        abstract fun getDialogText(): String
    }

    data object Hidden: SudokuErrorDialogState()
}