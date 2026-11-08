package com.jozeftvrdy.solver.core.datastore

import com.jozeftvrdy.solver.core.datastore.impl.SudokuPreferencesRepositoryImpl
import com.jozeftvrdy.solver.core.datastore.`interface`.SudokuPreferencesRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataStoreModule = module {
    single<SudokuPreferencesRepository> {
        SudokuPreferencesRepositoryImpl(androidContext())
    }
}
