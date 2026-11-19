package dev.fezojko.solver.gridpuzzle.di.module

import dev.fezojko.solver.core.datastore.dataStoreModule
import dev.fezojko.solver.gridpuzzle.settings.SettingsViewModel
import dev.fezojko.solver.sudoku.sudokuModule
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    includes(sudokuModule)
    includes(dataStoreModule)

    viewModel {
        SettingsViewModel(
            sudokuPreferencesRepository = get()
        )
    }
}