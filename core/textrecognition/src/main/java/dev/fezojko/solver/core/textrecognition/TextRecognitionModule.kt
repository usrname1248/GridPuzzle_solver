package dev.fezojko.solver.core.textrecognition

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val textRecognitionModule = module {
    factory {
        HandwrittenNumbersRecognizer(
            androidContext()
        )
    }

    factory {
        PrintedTextRecognizer()
    }
}