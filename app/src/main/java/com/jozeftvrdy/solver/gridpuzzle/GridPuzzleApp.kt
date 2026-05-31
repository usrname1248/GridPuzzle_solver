package com.jozeftvrdy.solver.gridpuzzle

import android.app.Application
import com.jozeftvrdy.solver.gridpuzzle.di.module.appModule
import com.jozeftvrdy.solver.sudoku.sudokuModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class GridPuzzleApp : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            // Log Koin events (Level.INFO by default)
            androidLogger()
            // Reference Android context
            androidContext(this@GridPuzzleApp)
            // Load modules
            modules(appModule, sudokuModule)
        }
    }
}