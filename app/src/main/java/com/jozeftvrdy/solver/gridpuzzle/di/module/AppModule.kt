package com.jozeftvrdy.solver.gridpuzzle.di.module

import com.jozeftvrdy.solver.core.datastore.dataStoreModule
import com.jozeftvrdy.solver.gridpuzzle.settings.SettingsViewModel
import com.jozeftvrdy.solver.sudoku.sudokuModule
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