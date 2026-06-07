package com.jozeftvrdy.solver.sudoku.data

import com.jozeftvrdy.solver.sudoku.model.SudokuFieldInputModel
import com.jozeftvrdy.solver.sudoku.model.SudokuResult
import com.jozeftvrdy.solver.sudoku.model.SudokuTileValueInputModel
import kotlinx.coroutines.flow.Flow

interface SudokuRepository {
    suspend fun solve(values: List<SudokuTileValueInputModel>, fieldParams: SudokuFieldInputModel): Flow<SudokuResult>
}