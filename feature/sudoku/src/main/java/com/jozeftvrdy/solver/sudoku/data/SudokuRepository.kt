package com.jozeftvrdy.solver.sudoku.data

import com.jozeftvrdy.solver.sudoku.model.SudokuResult
import com.jozeftvrdy.solver.sudoku.model.SudokuTileValueInputModel
import kotlinx.coroutines.flow.Flow

interface SudokuRepository {
    suspend fun solve(input: List<SudokuTileValueInputModel>): Flow<SudokuResult>
}