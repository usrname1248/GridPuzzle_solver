package com.jozeftvrdy.solver.sudoku.model

sealed class FinalSudokuResult : SudokuResult() {
    data class Success(
        val successValues: List<SudokuTileValueFullSolvedModel>
    ): FinalSudokuResult()

    sealed class Failure: FinalSudokuResult() {
        data class InputWithDuplicate(
            val value1: SudokuPosition,
            val value2: SudokuPosition,
        ): Failure()
        data object NoSolutionFound: Failure()

    }
}