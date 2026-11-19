plugins {
    id("gridpuzzle.android.library")
}

android {
    namespace = "dev.fezojko.solver.core.textrecognition"
}

dependencies {
    implementation(project(":core:datastore"))

    implementation(libs.google.mlkit.text.recognition)
    implementation(libs.litert)
    implementation(libs.tensorflow)

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)


    implementation(libs.koin.android)

    // Test
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.test.junit)
}