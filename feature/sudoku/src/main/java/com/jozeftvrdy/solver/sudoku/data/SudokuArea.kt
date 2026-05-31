package com.jozeftvrdy.solver.sudoku.data

import androidx.annotation.VisibleForTesting
import com.jozeftvrdy.solver.sudoku.model.SudokuPosition
import com.jozeftvrdy.solver.sudoku.model.SudokuSolveType
import com.jozeftvrdy.solver.sudoku.model.SudokuTileValueDataModel

internal abstract class SudokuArea {

    @VisibleForTesting
    abstract val items: List<SudokuTileValueHolder>
    protected abstract val solveType: SudokuSolveType
    operator fun get(index: Int): SudokuTileValueHolder = items[index]

    fun findDuplicates(): Pair<SudokuTileValueHolder, SudokuTileValueHolder>? {
        items.groupBy {
            it.tileValue.valueOrNull()
        }.forEach {
            if (it.key != null && it.value.size > 1) {
                return it.value[0] to it.value[1]
            }
        }

        return null
    }

    fun removePossibilitiesIfMissing(value: Int, becauseOfPosition: SudokuPosition, force: Boolean = false) {
        items.forEach {
            when (val tileValue = it.tileValue) {
                is SudokuTileValueDataModel.FixedTileValue,
                is SudokuTileValueDataModel.FlexibleTileValue.SolvedTileValue -> {}
                is SudokuTileValueDataModel.FlexibleTileValue.UnsolvedTileValue -> {
                    tileValue.removePossibilityIfMissing(value, becauseOfPosition, force)
                }
            }
        }
    }

    fun findAllItemsWithSinglePossibility(): List<ItemSolution> {
        return items.flatMap { tile ->
            val possibleValues = when (val localTileValue = tile.tileValue) {
                is SudokuTileValueDataModel.FlexibleTileValue.UnsolvedTileValue -> localTileValue.possibleValues
                else -> emptySet() // Fixed or Solved tiles don't go into these groups
            }
            // Create a pair for every possible value this tile could take
            possibleValues.map { value -> value to tile }
        }
            .groupBy({ it.first }, { it.second })
            .filter { it.value.size == 1 }
            .mapValues {
                it.value.first()
            }
            .map { (value, item) ->
                ItemSolution(
                    value,
                    item.position,
                    this.solveType
                )
            }
    }

    fun findReasonPositionForValue(value: Int): List<SudokuPosition> {
        return items.mapNotNull { item ->
            when (val localTileValue = item.tileValue) {
                is SudokuTileValueDataModel.FixedTileValue -> null
                is SudokuTileValueDataModel.FlexibleTileValue.SolvedTileValue -> null
                is SudokuTileValueDataModel.FlexibleTileValue.UnsolvedTileValue -> localTileValue.findReasonPositionForValue(value)
            }
        }
    }
}

internal class SudokuRow(
    override val items: List<SudokuTileValueHolder>
): SudokuArea() {
    override val solveType: SudokuSolveType = SudokuSolveType.TheOnlyOptionInRow
}

internal class SudokuColumn(
    override val items: List<SudokuTileValueHolder>
): SudokuArea() {
    override val solveType: SudokuSolveType = SudokuSolveType.TheOnlyOptionInColumn
}

internal class SudokuSquare(
    override val items: List<SudokuTileValueHolder>
): SudokuArea() {
    override val solveType: SudokuSolveType = SudokuSolveType.TheOnlyOptionInSquare
}