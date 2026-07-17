package com.jozeftvrdy.solver.sudoku.model

sealed class SudokuSolvedTurnReason() {
    data class OnlyFreeSpaceInArea(
        val area: SudokuAreaDomainModel
    ): SudokuSolvedTurnReason()

    data class OnlyOptionInArea(
        val area: SudokuAreaDomainModel,
        val positionReasons: Map<SudokuPosition, SudokuSolvedTileType>
    ): SudokuSolvedTurnReason()

    data class OnlyValueOptionForThisTile(
        val otherValuesPositions: List<SudokuPosition>
    ): SudokuSolvedTurnReason()
}