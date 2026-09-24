package com.jozeftvrdy.solver.sudoku.data

import com.jozeftvrdy.solver.sudoku.model.FinalSudokuResult
import com.jozeftvrdy.solver.sudoku.model.PartiallySolvedSudokuResult
import com.jozeftvrdy.solver.sudoku.model.SudokuFieldInputModel
import com.jozeftvrdy.solver.sudoku.model.SudokuPosition
import com.jozeftvrdy.solver.sudoku.model.SudokuResult
import com.jozeftvrdy.solver.sudoku.model.SudokuSolvedTurnReason
import com.jozeftvrdy.solver.sudoku.model.SudokuTileValueDataModel
import com.jozeftvrdy.solver.sudoku.model.SudokuTileValueFullSolvedModel
import com.jozeftvrdy.solver.sudoku.model.SudokuTileValueInputModel
import com.jozeftvrdy.solver.sudoku.util.DefaultDispatcherProvider
import com.jozeftvrdy.solver.sudoku.util.DispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

internal class SudokuTile(
    val position: SudokuPosition,
    var tileValue: SudokuTileValueDataModel,
) {
    fun toFullSolvedModel(): SudokuTileValueFullSolvedModel =
        when (val localTileValue = tileValue) {
            is SudokuTileValueDataModel.FixedTileValue -> SudokuTileValueFullSolvedModel.FixedTileValue(
                value = localTileValue.value,
                position = position
            )

            is SudokuTileValueDataModel.FlexibleTileValue.SolvedTileValue -> SudokuTileValueFullSolvedModel.SolvedTileValue(
                value = localTileValue.value,
                position = position,
            )

            is SudokuTileValueDataModel.FlexibleTileValue.UnsolvedTileValue -> throw IllegalStateException(
                "Cannot map to FullSolvedModel because tile at position $position is unsolved."
            )
        }
}

internal data class ItemSolution(
    val value: Int,
    val position: SudokuPosition,
    val solveType: SudokuSolveType
)

class SudokuRepositoryImpl(
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : SudokuRepository {

    override suspend fun solve(
        values: List<SudokuTileValueInputModel>,
        fieldParams: SudokuFieldInputModel
    ): Flow<SudokuResult> = flow {
        val sudokuField = SudokuField(
            valuesInputModel = values,
            areasInputModel = fieldParams.areasInputModel
        )

        sudokuField.findFirstInvalidEntry()?.let {
            emit(
                FinalSudokuResult.Failure.InputWithDuplicate(
                    it.first.position,
                    it.second.position
                )
            )
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

        emit(
            FinalSudokuResult.Success(
                successValues = sudokuField.toFullSolvedModel()
            )
        )
    }.flowOn(dispatchers.default)

    private suspend fun solveInternal(
        sudokuField: SudokuField,
        onPartialResultFound: suspend (PartiallySolvedSudokuResult) -> Unit,
    ): FinalSudokuResult.Failure? {
        while (true) {
            if (sudokuField.isSolved()) {
                //nothing to solve return no problem
                return null
            }

            if (sudokuField.hasDeadEnd()) {
                return FinalSudokuResult.Failure.NoSolutionFound
            }

            val founds: MutableMap<SudokuPosition, ItemSolution> =
                findSingleOptionInAllAreas(sudokuField)

            if (founds.isEmpty()) {
                // make a guess
                return startGuessing(
                    sudokuField,
                    onPartialResultFound
                )
            } else {
                founds.values.forEach { found ->
                    onSolutionForTileFound(found, sudokuField, onPartialResultFound)
                }
            }
        }
    }

    private suspend fun startGuessing(
        sudokuField: SudokuField,
        onPartialResultFound: suspend (PartiallySolvedSudokuResult) -> Unit,
    ): FinalSudokuResult.Failure? {
        val alreadyGuessedValues = mutableListOf<Pair<SudokuPosition, Int>>()

        while (true) {
            val partialResults = mutableListOf<PartiallySolvedSudokuResult>()
            val newGuess = sudokuField.getFirstUnsolvedValue(alreadyGuessedValues)
                ?: return FinalSudokuResult.Failure.NoSolutionFound
            alreadyGuessedValues.add(newGuess)
            val finalResult = this.solveInternal(
                sudokuField = sudokuField.createNewFieldWithGuessedValue(
                    newGuess.second,
                    newGuess.first
                ),
                onPartialResultFound = {
                    partialResults.add(it)
                }
            )

            if (finalResult == null) {
                partialResults.add(0, PartiallySolvedSudokuResult(
                    value = newGuess.second,
                    position = newGuess.first,
                    reason = SudokuSolvedTurnReason.GuessedValue
                ))

                partialResults.forEach {
                    onPartialResultFound(it)
                    sudokuField.setNumberToPosition(
                        value = it.value,
                        position = it.position,
                        isFixed = false,
                    )
                }

                return null
            }
        }
    }

    private suspend fun onSolutionForTileFound(
        found: ItemSolution,
        sudokuField: SudokuField,
        onPartialResultFound: suspend (PartiallySolvedSudokuResult) -> Unit
    ) {
        val reason: SudokuSolvedTurnReason = when (found.solveType) {
            is SudokuSolveType.TheOnlyOptionInArea -> {
                SudokuSolvedTurnReason.OnlyOptionInArea(
                    area = found.solveType.area.toDomainModel(),
                    positionReasons = found.solveType.area.findReasonPositionForValue(found)
                )
            }

            is SudokuSolveType.TheOnlyOptionInPlace -> {
                SudokuSolvedTurnReason.OnlyValueOptionForThisTile(
                    otherValuesPositions = (sudokuField[found.position].tileValue as? SudokuTileValueDataModel.FlexibleTileValue.UnsolvedTileValue)
                        ?.getAllReasons()
                        ?: throw IllegalStateException("getAllReasons can be used only at unsolved tile")
                )
            }

            is SudokuSolveType.TheOnlyUnsolvedTileInArea -> {
                SudokuSolvedTurnReason.OnlyFreeSpaceInArea(
                    area = found.solveType.area.toDomainModel(),
                )
            }
        }

        onPartialResultFound(
            PartiallySolvedSudokuResult(
                value = found.value,
                position = found.position,
                reason = reason
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

        val allAreasSortedByPriority = sudokuField.allAreas.sortedByDescending { it.priority }

        allAreasSortedByPriority.forEach {
            it.findSingleUnsolvedTileSolution()?.also { itemSolution ->
                founds.putIfAbsent(itemSolution.position, itemSolution)
            }
        }

        allAreasSortedByPriority.forEach {
            it.findAllItemsWithSinglePossibility().forEach { itemSolution ->
                founds.putIfAbsent(itemSolution.position, itemSolution)
            }
        }

        // At last check, if tile has single possibility for its value, cause others has been ruled out
        sudokuField.findAllItemsWithSingleAreaPossibility()
            .forEach { itemSolution ->
                // we only add resolves, that are not there, cause those, which are already there have priority
                founds.putIfAbsent(itemSolution.position, itemSolution)
            }

        val foundsPositions = founds.map {
            it.value
        }.groupBy {
            it.value
        }.mapValues { mappedValue ->
            mappedValue.value.map {
                it.position
            }
        }.values

        foundsPositions.forEach { allPositionsForDigit ->
            val areasAlreadyClaimedByThisDigit = mutableSetOf<SudokuArea>()

            allPositionsForDigit.forEach { pos ->
                val areasForThisPos = sudokuField.areasByPosition[pos] ?: emptyList()

                // Check if any area of this tile was already claimed by another tile in this batch
                val conflict = areasForThisPos.any { it in areasAlreadyClaimedByThisDigit }

                if (conflict) {
                    founds.remove(pos)
                } else {
                    areasAlreadyClaimedByThisDigit.addAll(areasForThisPos)
                }
            }
        }

        return founds
    }
}