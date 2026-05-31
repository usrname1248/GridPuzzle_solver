package com.jozeftvrdy.solver.sudoku

import com.jozeftvrdy.solver.sudoku.data.SudokuRepository
import com.jozeftvrdy.solver.sudoku.data.SudokuRepositoryImpl
import com.jozeftvrdy.solver.sudoku.presentation.SudokuViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val sudokuModule = module {
    factory<SudokuRepository> {
        SudokuRepositoryImpl()
    }

    viewModel {
        SudokuViewModel()
    }
}