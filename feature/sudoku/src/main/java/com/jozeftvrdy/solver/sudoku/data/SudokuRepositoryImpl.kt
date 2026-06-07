package com.jozeftvrdy.solver.sudoku.data

import com.jozeftvrdy.solver.sudoku.model.FinalSudokuResult
import com.jozeftvrdy.solver.sudoku.model.PartiallySolvedSudokuResult
import com.jozeftvrdy.solver.sudoku.model.SudokuFieldInputModel
import com.jozeftvrdy.solver.sudoku.model.SudokuPosition
import com.jozeftvrdy.solver.sudoku.model.SudokuResult
import com.jozeftvrdy.solver.sudoku.model.SudokuSolveType
import com.jozeftvrdy.solver.sudoku.model.SudokuSolvedTileReason
import com.jozeftvrdy.solver.sudoku.model.SudokuTileValueDataModel
import com.jozeftvrdy.solver.sudoku.model.SudokuTileValueFullSolvedModel
import com.jozeftvrdy.solver.sudoku.model.SudokuTileValueInputModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

internal class SudokuTileValueHolder(
    val position: SudokuPosition,
    var tileValue: SudokuTileValueDataModel,
) {
    fun toFullSolvedModel(): SudokuTileValueFullSolvedModel = when (val localTileValue = tileValue) {
            is SudokuTileValueDataModel.FixedTileValue -> SudokuTileValueFullSolvedModel.FixedTileValue(
                value = localTileValue.value,
                position = position
            )
            is SudokuTileValueDataModel.FlexibleTileValue.SolvedTileValue -> SudokuTileValueFullSolvedModel.SolvedTileValue(
                value = localTileValue.value,
                position = position,
            )
            is SudokuTileValueDataModel.FlexibleTileValue.UnsolvedTileValue -> throw IllegalStateException("Cannot map to FullSolvedModel because tile at position $position is unsolved.")
        }
}

internal data class ItemSolution(
    val value: Int,
    val position: SudokuPosition,
    val solveType: SudokuSolveType
)

class SudokuRepositoryImpl: SudokuRepository {
    override suspend fun solve(values: List<SudokuTileValueInputModel>, fieldParams: SudokuFieldInputModel): Flow<SudokuResult> = flow {
        val sudokuField = SudokuField(
            inputModel = fieldParams,
            values = values,
        )

        sudokuField.findFirstInvalidEntry()?.let {
            emit( FinalSudokuResult.Failure.InputWithDuplicate(
                it.first.position,
                it.second.position
            ))
            return@flow
        }

        solveInternal(
            sudokuField = sudokuField,
            onPartialResultFound = {
                emit(it)
            }
        )?.let {
            emit(it)
            return@flow
        }

        emit(FinalSudokuResult.Success(
            successValues = sudokuField.toFullSolvedModel()
        ))
    }

    private suspend fun solveInternal(
        sudokuField: SudokuField,
        onPartialResultFound: suspend (PartiallySolvedSudokuResult) -> Unit,
    ): FinalSudokuResult.Failure? {
        while (true) {
            if (sudokuField.isSolved()) {
                //nothing to solve return no problem
                return null
            }

            val founds: MutableMap<SudokuPosition, ItemSolution> = findSingleOptionInAllAreas(sudokuField)

            if (founds.isEmpty()) {
                return FinalSudokuResult.Failure.NoSolutionFound
            }

            founds.values.forEach { found ->
                onSolutionForTileFound(found, sudokuField, onPartialResultFound)
            }
        }
    }

    private suspend fun onSolutionForTileFound(
        found: ItemSolution,
        sudokuField: SudokuField,
        onPartialResultFound: suspend (PartiallySolvedSudokuResult) -> Unit
    ) {
        val causes = when (found.solveType) {
            SudokuSolveType.TheOnlyOptionInColumn -> sudokuField.columnAt(
                position = found.position
            ).findReasonPositionForValue(found.value)

            SudokuSolveType.TheOnlyOptionInRow -> sudokuField.rowAt(
                position = found.position
            ).findReasonPositionForValue(found.value)

            SudokuSolveType.TheOnlyOptionInSquare -> sudokuField.squareAt(
                position = found.position
            ).findReasonPositionForValue(found.value)

            SudokuSolveType.TheOnlyOptionInPlace -> {
                (sudokuField[found.position].tileValue as? SudokuTileValueDataModel.FlexibleTileValue.UnsolvedTileValue)
                    ?.getAllReasons()
                    ?: throw IllegalStateException("getAllReasons can be used only at unsolved tile")
            }
        }.distinct()

        onPartialResultFound(
            PartiallySolvedSudokuResult(
                value = found.value,
                position = found.position,
                reason = SudokuSolvedTileReason(
                    solveType = found.solveType,
                    causes = causes
                )
            )
        )

        sudokuField.setNumberToPosition(
            value = found.value,
            position = found.position,
            isFixed = false,
        )
    }

    private fun findSingleOptionInAllAreas(sudokuField: SudokuField): MutableMap<SudokuPosition, ItemSolution> {
        val founds: MutableMap<SudokuPosition, ItemSolution> = mutableMapOf()

        // First check squares
        sudokuField.squares.forEach { square ->
            square.findAllItemsWithSinglePossibility()
                .forEach { itemSolution ->
                    founds.putIfAbsent(itemSolution.position, itemSolution)
                }
        }

        // Second check rows
        sudokuField.rows
            .forEach { row ->
                row
                    .findAllItemsWithSinglePossibility()
                    .forEach { itemSolution ->
                        // we only add resolves, that are not there, cause those, which are already there have priority
                        founds.putIfAbsent(itemSolution.position, itemSolution)
                    }
            }

        // Third check columns
        sudokuField.columns
            .forEach { column ->
                column
                    .findAllItemsWithSinglePossibility()
                    .forEach { itemSolution ->
                        // we only add resolves, that are not there, cause those, which are already there have priority
                        founds.putIfAbsent(itemSolution.position, itemSolution)
                    }
            }

        // At last check, if tile has single possibility for its value, cause others has been ruled out
        sudokuField.findAllItemsWithSingleAreaPossibility()
            .forEach { itemSolution ->
                // we only add resolves, that are not there, cause those, which are already there have priority
                founds.putIfAbsent(itemSolution.position, itemSolution)
            }
        return founds
    }
}