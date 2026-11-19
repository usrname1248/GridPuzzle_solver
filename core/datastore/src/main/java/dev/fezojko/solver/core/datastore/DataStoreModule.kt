package dev.fezojko.solver.core.datastore

import dev.fezojko.solver.core.datastore.impl.SudokuPreferencesRepositoryImpl
import dev.fezojko.solver.core.datastore.`interface`.SudokuPreferencesRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataStoreModule = module {
    single<SudokuPreferencesRepository> {
        SudokuPreferencesRepositoryImpl(androidContext())
    }
}
