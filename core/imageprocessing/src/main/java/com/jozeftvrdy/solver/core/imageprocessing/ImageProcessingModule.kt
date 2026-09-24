package com.jozeftvrdy.solver.core.imageprocessing

import org.koin.dsl.module

val imageProcessingModule = module {
    factory {
        TextRecognizerProcessor()
    }
}