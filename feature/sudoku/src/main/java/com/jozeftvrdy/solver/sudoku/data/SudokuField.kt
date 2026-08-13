package com.jozeftvrdy.solver.sudoku.data

import com.jozeftvrdy.solver.sudoku.model.SudokuAreasInputModel
import com.jozeftvrdy.solver.sudoku.model.SudokuInputTileType
import com.jozeftvrdy.solver.sudoku.model.SudokuPosition
import com.jozeftvrdy.solver.sudoku.model.SudokuTileValueDataModel
import com.jozeftvrdy.solver.sudoku.model.SudokuTileValueFullSolvedModel
import com.jozeftvrdy.solver.sudoku.model.SudokuTileValueInputModel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

internal data class SudokuField(
    val valuesInputModel: List<SudokuTileValueInputModel>,
    val areasInputModel: SudokuAreasInputModel,
) {

    val valueByPositionMap: Map<SudokuPosition, SudokuTile> = areasInputModel.areas.flatMap {
        it.positions
    }.distinct().associateWith {
        SudokuTile(
            position = it,
            tileValue = SudokuTileValueDataModel.FlexibleTileValue.UnsolvedTileValue(
                areasInputModel.areas.first().positions.size
            )
        )
    }

    val allValues: Collection<SudokuTile> = valueByPositionMap.values.distinct()

    private fun MutableList<SudokuArea>.addByPriority(
        item: SudokuArea
    ) {
        if (this.isEmpty()) {
            this.add(item)
            return
        }

        val indexOfItemWithLowerPriority = this.indexOfFirst {
            it.priority < item.priority
        }

        if (indexOfItemWithLowerPriority < 0) {
            this.add(item)
        } else {
            this.add(indexOfItemWithLowerPriority, item)
        }
    }

    val allAreas: List<SudokuArea> = areasInputModel.areas.map {
        it.toLocalModel(valueByPositionMap::getValue)
    }

    val areasByPosition: Map<SudokuPosition, List<SudokuArea>> = allAreas.let { areas ->
        buildMap<SudokuPosition, MutableList<SudokuArea>> {
            areas.forEach { area ->
                area.tiles.forEach { tile ->
                    this[tile.position]?.addByPriority(area)?:run {
                        this[tile.position] = mutableListOf(
                            area
                        )
                    }
                }
            }
        }
    }


    operator fun get(position: SudokuPosition): SudokuTile {
        return valueByPositionMap[position]!!
    }

    init {
        valuesInputModel.forEach { inputModel ->
            setNumberToPosition(
                value = inputModel.value,
                position = inputModel.position,
                isFixed = when (inputModel.tileType) {
                    SudokuInputTileType.FixedValue -> true
                    SudokuInputTileType.SolvedValue -> false
                },
            )
        }
    }

    fun setNumberToPosition(
        value: Int,
        position: SudokuPosition,
        isFixed: Boolean = false,
    ) {
        val tileAtPosition = this[position]
        tileAtPosition.tileValue = if (isFixed) {
            SudokuTileValueDataModel.FixedTileValue(
                value = value
            )
        } else {
            SudokuTileValueDataModel.FlexibleTileValue.SolvedTileValue(
                value = value
            )
        }

        areasByPosition[position]?.forEach { area ->
            area.removePossibilitiesIfMissing(value, position, priority = area.priority)
        }
    }

    suspend fun findFirstInvalidEntry(): Pair<SudokuTile, SudokuTile>? = coroutineScope {
        val size = allAreas.size
        val channel = Channel<Pair<SudokuTile, SudokuTile>?>(capacity = size)

        // Start all searches in parallel
        allAreas.forEach {
            launch {
                val result = it.findDuplicates()
                channel.send(result)
            }
        }

        repeat(size) {
            val found = channel.receive()
            if (found != null) {
                // FIRST RESULT FOUND: Cancel all other pending scans immediately
                coroutineContext.cancelChildren()
                return@coroutineScope found
            }
        }
        null
    }

    fun toFullSolvedModel(): List<SudokuTileValueFullSolvedModel> = allValues.map { it.toFullSolvedModel() }

    fun isSolved(): Boolean = allValues.all {
        when (it.tileValue) {
            is SudokuTileValueDataModel.FixedTileValue,
            is SudokuTileValueDataModel.FlexibleTileValue.SolvedTileValue -> true
            is SudokuTileValueDataModel.FlexibleTileValue.UnsolvedTileValue -> false
        }
    }

   fun hasDeadEnd(): Boolean = allValues.any {
        it.tileValue is SudokuTileValueDataModel.FlexibleTileValue.UnsolvedTileValue &&
                (it.tileValue as SudokuTileValueDataModel.FlexibleTileValue.UnsolvedTileValue).possibleValues.isEmpty()
    } || allAreas.any { area ->
        area.tiles.mapNotNull {
            if (it.tileValue is SudokuTileValueDataModel.FlexibleTileValue.UnsolvedTileValue)
                it.tileValue as SudokuTileValueDataModel.FlexibleTileValue.UnsolvedTileValue
            else null
        }.let { unsolvedTiles ->
            // we must have same number of empty tiles and same number of possible items to put them to.
            // if we have more possibilities to lower count of empty tiles
            // or less possibilities to higher count of empty tiles,
            // Then this cannot be solved
            val sameSize = unsolvedTiles.size == unsolvedTiles.flatMap {
                it.possibleValues
            }.distinct().size

            // Pigeonhole Principle
            // if we have x numbers of tiles with same y possibilities,
            // we cannot have less those tiles than number of possibilities that must fit into them
            val hasSamePossibilitiesForFewTiles = unsolvedTiles.groupBy {
                it.possibleValues
            }.any { it.key.size < it.value.size }


            sameSize.not() || hasSamePossibilitiesForFewTiles
        }
   }

    fun findAllItemsWithSingleAreaPossibility(): List<ItemSolution> {
        return allValues.mapNotNull { tile ->
            when (val localTileValue = tile.tileValue) {
                is SudokuTileValueDataModel.FlexibleTileValue.UnsolvedTileValue -> {
                    val (value, item) = if (
                        localTileValue.possibleValues.size == 1
                    ) {
                        localTileValue.possibleValues.first() to tile
                    } else return@mapNotNull null

                    ItemSolution(
                        value,
                        item.position,
                        SudokuSolveType.TheOnlyOptionInPlace
                    )
                }

                else -> null
            }
        }
    }

    fun createNewFieldWithGuessedValue(
        guessedValue: Int,
        guessedLocation: SudokuPosition,
    ): SudokuField {
        val currentValues = allValues.mapNotNull {
            when (val localTileValue = it.tileValue) {
                is SudokuTileValueDataModel.FixedTileValue -> {
                    SudokuTileValueInputModel(
                        value = localTileValue.value,
                        position = it.position,
                        tileType = SudokuInputTileType.FixedValue
                    )
                }
                is SudokuTileValueDataModel.FlexibleTileValue.SolvedTileValue -> {
                    SudokuTileValueInputModel(
                        value = localTileValue.value,
                        position = it.position,
                        tileType = SudokuInputTileType.SolvedValue
                    )
                }
                is SudokuTileValueDataModel.FlexibleTileValue.UnsolvedTileValue -> null
            }
        }
        val nextValues = currentValues + SudokuTileValueInputModel(
            value = guessedValue,
            position = guessedLocation,
            tileType = SudokuInputTileType.SolvedValue
        )
        return SudokuField(
            valuesInputModel = nextValues,
            areasInputModel = this.areasInputModel
        )
    }
    fun getFirstUnsolvedValue(alreadyGuessedValues: List<Pair<SudokuPosition, Int>>): Pair<SudokuPosition, Int>? {
        this.allValues.forEach { value ->
            if (value.tileValue is SudokuTileValueDataModel.FlexibleTileValue.UnsolvedTileValue) {
                (value.tileValue as SudokuTileValueDataModel.FlexibleTileValue.UnsolvedTileValue).possibleValues.forEach { possibleValue ->
                    val pair = value.position to possibleValue
                    if (alreadyGuessedValues.contains(pair).not())
                        return pair
                }
            }
        }

        return null
    }
}