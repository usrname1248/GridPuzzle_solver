package com.jozeftvrdy.solver.sudoku.model

sealed class SudokuSolvedTurnReason() {
    data class OnlyFreeSpaceInArea(
        val area: SudokuAreaDomainModel
    ): SudokuSolvedTurnReason()

    data class OnlyOptionInArea(
        val area: SudokuAreaDomainModel,
        val positionReasons: Map<SudokuPosition, SudokuSolvedTileType>
    ): SudokuSolvedTurnReason() {
        val externalReasons: List<SudokuPosition> = buildList {
            positionReasons.forEach { (_, type) ->
                when(type) {
                    SudokuSolvedTileType.SolvedTile,
                    SudokuSolvedTileType.FilledTile -> {}
                    is SudokuSolvedTileType.RuledOutTile -> {
                        add(type.becauseOfTile)
                    }
                }
            }
        }
    }

    data class OnlyValueOptionForThisTile(
        val otherValuesPositions: List<SudokuPosition>
    ): SudokuSolvedTurnReason()

    data object GuessedValue: SudokuSolvedTurnReason()
}