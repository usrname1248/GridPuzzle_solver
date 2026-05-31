package com.jozeftvrdy.solver.sudoku.data

import com.jozeftvrdy.solver.sudoku.model.SudokuPosition
import com.jozeftvrdy.solver.sudoku.model.SudokuSolveType
import com.jozeftvrdy.solver.sudoku.model.SudokuTileValueDataModel
import com.jozeftvrdy.solver.sudoku.model.SudokuTileValueFullSolvedModel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

internal class SudokuField {
    val allValues: List<SudokuTileValueHolder> = List(size = 9*9) { index ->
        SudokuTileValueHolder(
            position = SudokuPosition(
                x = index % 9 + 1,
                y = index / 9 + 1
            ),
            tileValue = SudokuTileValueDataModel.FlexibleTileValue.UnsolvedTileValue()
        )
    }

    val rows: List<SudokuRow> = allValues.chunked(9).map {
        SudokuRow(it)
    }

    fun rowAt(position: SudokuPosition) : SudokuRow {
        return rows[position.y - 1]
    }

    val columns: List<SudokuColumn> = (0 until 9).map { colIndex ->
        rows.map { row -> row[colIndex] }
    }.map {
        SudokuColumn(it)
    }

    fun columnAt(position: SudokuPosition) : SudokuColumn {
        return columns[position.x - 1]
    }

    val squares: List<SudokuSquare> = allValues
        .chunked(9)
        .chunked(3)
        .flatMap { threeRows ->
            (0 until 3).map { colGroup ->
                threeRows.flatMap { row ->
                    row.subList(colGroup * 3, (colGroup + 1) * 3)
                }
            }
        }
        .map {
            SudokuSquare(it)
        }

    fun squareAt(position: SudokuPosition) : SudokuSquare {
        return squares[(position.y - 1) / 3 * 3 + (position.x - 1) / 3]
    }

    operator fun get(position: SudokuPosition): SudokuTileValueHolder {
        return rows[position.y - 1][position.x -1]
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

        // we want to remove possibilities in squares first, cause its way more common reason as in row / column so it has priority
        squareAt(position).removePossibilitiesIfMissing(value, position, force = true)
        // otherwise it cant be also in same row and column, the only value sharing this column and row is this tile, which are we modifying right now, so order does not matter
        rowAt(position).removePossibilitiesIfMissing(value, position)
        columnAt(position).removePossibilitiesIfMissing(value, position)
    }

    suspend fun findFirstInvalidEntry(): Pair<SudokuTileValueHolder, SudokuTileValueHolder>? = coroutineScope {
        val allAreas = squares + columns + rows
        val size = allAreas.size
        val channel = Channel<Pair<SudokuTileValueHolder, SudokuTileValueHolder>?>(capacity = size)

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
}