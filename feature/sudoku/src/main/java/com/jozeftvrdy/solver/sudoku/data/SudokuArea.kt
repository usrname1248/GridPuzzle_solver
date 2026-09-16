package com.jozeftvrdy.solver.sudoku.data

import androidx.annotation.VisibleForTesting
import com.jozeftvrdy.solver.sudoku.model.SudokuAreaDomainModel
import com.jozeftvrdy.solver.sudoku.model.SudokuAreaPriority
import com.jozeftvrdy.solver.sudoku.model.SudokuPosition
import com.jozeftvrdy.solver.sudoku.model.SudokuSolvedTileType
import com.jozeftvrdy.solver.sudoku.model.SudokuTileValueDataModel

internal data class SudokuArea(
    @get:VisibleForTesting
    val tiles: List<SudokuTile>,
    val priority: SudokuAreaPriority,
    val tag: Any? = null,
) {

    operator fun get(index: Int): SudokuTile = tiles[index]

    fun findDuplicates(): Pair<SudokuTile, SudokuTile>? {
        tiles.groupBy {
            it.tileValue.valueOrNull()
        }.forEach {
            if (it.key != null && it.value.size > 1) {
                return it.value[0] to it.value[1]
            }
        }

        return null
    }

    fun removePossibilitiesIfMissing(value: Int, becauseOfPosition: SudokuPosition, priority: SudokuAreaPriority) {
        tiles.forEach {
            when (val tileValue = it.tileValue) {
                is SudokuTileValueDataModel.FixedTileValue,
                is SudokuTileValueDataModel.FlexibleTileValue.SolvedTileValue -> {}
                is SudokuTileValueDataModel.FlexibleTileValue.UnsolvedTileValue -> {
                    tileValue.removePossibilityIfMissing(value, becauseOfPosition, priority)
                }
            }
        }
    }

    fun findSingleUnsolvedTileSolution(): ItemSolution? {
        var theOnlyUnsolvedTile: SudokuTile? = null
        tiles.forEach { tile ->
            if (tile.tileValue is SudokuTileValueDataModel.FlexibleTileValue.UnsolvedTileValue) {
                // we have already found one unsolved, so this have to be second
                if (theOnlyUnsolvedTile != null) {
                    return null
                }

                theOnlyUnsolvedTile = tile
            }
        }

        return theOnlyUnsolvedTile?.let { tile ->
            ItemSolution(
                value = (tile.tileValue as SudokuTileValueDataModel.FlexibleTileValue.UnsolvedTileValue).possibleValues.let {
                    if (it.isEmpty()) {
                        return null
                    } else {
                        it.first()
                    }
                },
                position = tile.position,
                solveType = SudokuSolveType.TheOnlyUnsolvedTileInArea(
                    area = this
                ),
            )
        }
    }

    fun findAllItemsWithSinglePossibility(): List<ItemSolution> {
        return tiles.flatMap { tile ->
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
                    SudokuSolveType.TheOnlyOptionInArea(this)
                )
            }
    }

    fun findReasonPositionForValue(found: ItemSolution): Map<SudokuPosition, SudokuSolvedTileType> {
        return buildMap {
            tiles.forEach { item ->
                if (item.position == found.position) {
                    return@forEach
                }

                when (val localTileValue = item.tileValue) {
                    is SudokuTileValueDataModel.FixedTileValue,
                    is SudokuTileValueDataModel.FlexibleTileValue.SolvedTileValue -> {
                        put(item.position, SudokuSolvedTileType.SolvedTile)
                    }
                    is SudokuTileValueDataModel.FlexibleTileValue.UnsolvedTileValue -> localTileValue.findReasonPositionForValue(found.value)?.let {
                        put(
                            it,
                            SudokuSolvedTileType.RuledOutTile(
                                becauseOfTile = it
                            )
                        )
                    }
                }
            }

            put(
                found.position,
                SudokuSolvedTileType.FilledTile
            )
        }
    }

    fun toDomainModel() = SudokuAreaDomainModel(
        positions = this.tiles.map { it.position },
        priority = this.priority,
        tag = this.tag
    )
}

internal fun SudokuAreaDomainModel.toLocalModel(
    getTileForPosition: (SudokuPosition) -> SudokuTile
) = SudokuArea(
    tiles = this.positions.map(getTileForPosition),
    priority = this.priority,
    tag = this.tag,
)