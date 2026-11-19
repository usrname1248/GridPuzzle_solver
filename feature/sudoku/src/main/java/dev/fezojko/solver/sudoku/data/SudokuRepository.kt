package dev.fezojko.solver.sudoku.data

import dev.fezojko.solver.sudoku.model.SudokuFieldInputModel
import dev.fezojko.solver.sudoku.model.SudokuResult
import dev.fezojko.solver.sudoku.model.SudokuTileValueInputModel
import kotlinx.coroutines.flow.Flow

interface SudokuRepository {
    suspend fun solve(values: List<SudokuTileValueInputModel>, fieldParams: SudokuFieldInputModel): Flow<SudokuResult>
}