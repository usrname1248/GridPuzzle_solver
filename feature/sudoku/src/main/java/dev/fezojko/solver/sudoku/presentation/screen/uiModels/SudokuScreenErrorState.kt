package dev.fezojko.solver.sudoku.presentation.screen.uiModels

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import dev.fezojko.solver.sudoku.model.FinalSudokuResult
import dev.fezojko.solver.sudoku.model.SudokuPosition

@Stable
@Immutable
sealed class SudokuScreenErrorState {
    data object NoError: SudokuScreenErrorState()
    data class DuplicateInput(
        val position1: SudokuPosition,
        val position2: SudokuPosition,
    ): SudokuScreenErrorState() {
        constructor(duplicateError: FinalSudokuResult.Failure.InputWithDuplicate): this(
            position1 = duplicateError.value1,
            position2 = duplicateError.value2
        )

        fun contains(position: SudokuPosition): Boolean = this.position1 == position || this.position2 == position
    }
}