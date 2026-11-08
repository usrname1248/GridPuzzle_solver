package com.jozeftvrdy.solver.sudoku.presentation.screen.uiModels

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jozeftvrdy.solver.sudoku.R

sealed class SudokuDialogState {
    sealed class ErrorState: SudokuDialogState() {

        data object DuplicateInputValue: ErrorState() {
            @Composable
            override fun getDialogText(): String =
                stringResource(R.string.sudoku_dialog_text_input_with_errors)
        }

        data object GuessedValue: ErrorState() {
            @Composable
            override fun getDialogText(): String =
                stringResource(R.string.sudoku_dialog_text_guessed_value)
        }

        data object NoSolution: ErrorState() {
            @Composable
            override fun getDialogText(): String =
                stringResource(R.string.sudoku_dialog_text_no_solution)
        }

        @Composable
        override fun getDialogTitle(): String =
            stringResource(R.string.sudoku_error_dialog_title)

    }

    sealed class WarningState: SudokuDialogState() {
        data object SudokuImportedNeedUserCheck: WarningState() {
            @Composable
            override fun getDialogText(): String =
                stringResource(R.string.sudoku_dialog_text_check_import)
        }

        @Composable
        override fun getDialogTitle(): String =
            stringResource(R.string.sudoku_warning_dialog_title)
    }

    @Composable
    abstract fun getDialogTitle(): String

    @Composable
    abstract fun getDialogText(): String

    data object Hidden: SudokuDialogState() {
        @Composable
        override fun getDialogTitle(): String = ""

        @Composable
        override fun getDialogText(): String = ""
    }
}