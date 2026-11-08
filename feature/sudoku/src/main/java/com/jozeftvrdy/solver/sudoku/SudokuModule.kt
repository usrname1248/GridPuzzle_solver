package com.jozeftvrdy.solver.sudoku

import com.jozeftvrdy.solver.core.imageprocessing.imageProcessingModule
import com.jozeftvrdy.solver.core.textrecognition.textRecognitionModule
import com.jozeftvrdy.solver.sudoku.data.SudokuRepository
import com.jozeftvrdy.solver.sudoku.data.SudokuRepositoryImpl
import com.jozeftvrdy.solver.sudoku.domain.ProcessSudokuImageUseCase
import com.jozeftvrdy.solver.sudoku.presentation.screen.SudokuViewModel
import com.jozeftvrdy.solver.sudoku.presentation.screen.uiModels.SudokuScreenState
import com.jozeftvrdy.solver.sudoku.util.DefaultDispatcherProvider
import com.jozeftvrdy.solver.sudoku.util.DispatcherProvider
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val sudokuModule = module {
    includes(imageProcessingModule)
    includes(textRecognitionModule)

    single<DispatcherProvider> {
        DefaultDispatcherProvider()
    }

    factory {
        ProcessSudokuImageUseCase(
            dispatchers = get(),
            printedTextRecognizer = get(),
            handwrittenTextRecognizer = get(),
            preferences = get(),
        )
    }

    factory<SudokuRepository> {
        SudokuRepositoryImpl(
            dispatchers = get()
        )
    }

    viewModel { (initialState: SudokuScreenState) ->
        SudokuViewModel(
            savedStateHandle = get(),
            sudokuRepository = get(),
            initialState = initialState,
        )
    }
}