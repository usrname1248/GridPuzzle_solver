plugins {
    id("gridpuzzle.android.library")
}

android {
    namespace = "dev.fezojko.solver.core.imageprocessing"

    androidResources {
        noCompress.add("tflite")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)


    implementation(libs.koin.android)

    // Test
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.test.junit)
}
