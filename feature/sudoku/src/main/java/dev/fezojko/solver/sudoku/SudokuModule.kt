package dev.fezojko.solver.sudoku

import dev.fezojko.solver.core.imageprocessing.imageProcessingModule
import dev.fezojko.solver.core.textrecognition.textRecognitionModule
import dev.fezojko.solver.sudoku.data.SudokuRepository
import dev.fezojko.solver.sudoku.data.SudokuRepositoryImpl
import dev.fezojko.solver.sudoku.domain.ProcessSudokuImageUseCase
import dev.fezojko.solver.sudoku.presentation.screen.SudokuViewModel
import dev.fezojko.solver.sudoku.presentation.screen.uiModels.SudokuScreenState
import dev.fezojko.solver.sudoku.util.DefaultDispatcherProvider
import dev.fezojko.solver.sudoku.util.DispatcherProvider
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