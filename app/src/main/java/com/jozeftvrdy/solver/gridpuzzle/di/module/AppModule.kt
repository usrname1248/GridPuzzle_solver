package com.jozeftvrdy.solver.gridpuzzle.di.module

import com.jozeftvrdy.solver.sudoku.sudokuModule
import org.koin.dsl.module

val appModule = module {
    includes(sudokuModule)
}