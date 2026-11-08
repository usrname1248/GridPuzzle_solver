plugins {
    id("gridpuzzle.android.library")
    id("gridpuzzle.compose")
}

android {
    namespace = "com.jozeftvrdy.solver.core.ui"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.test.junit)
}
