package com.jozeftvrdy.solver.sudoku.data

import com.jozeftvrdy.solver.sudoku.model.SudokuFieldInputModel
import com.jozeftvrdy.solver.sudoku.model.SudokuInputTileType
import com.jozeftvrdy.solver.sudoku.model.SudokuPosition
import com.jozeftvrdy.solver.sudoku.model.SudokuSolveType
import com.jozeftvrdy.solver.sudoku.model.SudokuTileValueDataModel
import com.jozeftvrdy.solver.sudoku.model.SudokuTileValueFullSolvedModel
import com.jozeftvrdy.solver.sudoku.model.SudokuTileValueInputModel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

internal class SudokuField(
    val sudokuFieldWidth: Int,
    val sudokuFieldHeight: Int,
    val areaWidth: Int,
    val areaHeight: Int,
    values: List<SudokuTileValueInputModel>
) {
    constructor(
        inputModel: SudokuFieldInputModel,
        values: List<SudokuTileValueInputModel>
    ): this(
        sudokuFieldWidth  = inputModel.sudokuFieldWidth,
        sudokuFieldHeight = inputModel.sudokuFieldHeight,
        areaWidth         = inputModel.areaWidth,
        areaHeight        = inputModel.areaHeight,
        values            = values,
    )

    val allValues: List<SudokuTileValueHolder> = List(size = sudokuFieldHeight * sudokuFieldWidth) { index ->
        SudokuTileValueHolder(
            position = SudokuPosition(
                x = index % sudokuFieldWidth + 1,
                y = index / sudokuFieldWidth + 1
            ),
            tileValue = SudokuTileValueDataModel.FlexibleTileValue.UnsolvedTileValue()
        )
    }

    val rows: List<SudokuRow> = allValues.chunked(sudokuFieldWidth).map {
        SudokuRow(it)
    }

    fun rowAt(position: SudokuPosition) : SudokuRow {
        return rows[position.y - 1]
    }

    val columns: List<SudokuColumn> = (0 until sudokuFieldWidth).map { colIndex ->
        SudokuColumn(
            rows.map { row -> row[colIndex] }
        )
    }

    fun columnAt(position: SudokuPosition) : SudokuColumn {
        return columns[position.x - 1]
    }

    val squares: List<SudokuSquare> = run {
        val horizontalSquareCount = sudokuFieldWidth / areaWidth
        val verticalSquareCount = sudokuFieldHeight / areaHeight

        List(horizontalSquareCount * verticalSquareCount) { index ->
            val squareX = index % horizontalSquareCount
            val squareY = index / horizontalSquareCount
            val startX = squareX * areaWidth
            val startY = squareY * areaHeight

            val tiles = mutableListOf<SudokuTileValueHolder>()
            for (yOffset in 0 until areaHeight) {
                val row = rows[startY + yOffset]
                for (xOffset in 0 until areaWidth) {
                    tiles.add(row[startX + xOffset])
                }
            }
            SudokuSquare(tiles)
        }
    }


    fun squareAt(position: SudokuPosition) : SudokuSquare {
        val horizontalSquareCount = sudokuFieldWidth / areaWidth
        val squareX = (position.x - 1) / areaWidth
        val squareY = (position.y - 1) / areaHeight
        return squares[squareY * horizontalSquareCount + squareX]
    }

    operator fun get(position: SudokuPosition): SudokuTileValueHolder {
        return rows[position.y - 1][position.x -1]
    }

    init {
        values.forEach { inputModel ->
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