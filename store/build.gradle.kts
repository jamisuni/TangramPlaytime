// :store — everything that survives a restart (IProgressStore implementation; architecture.md §3, G-13).
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.tangramplaytime.store"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":contracts"))
    implementation(libs.kotlinx.coroutines.core)
    // ADR-003: the progress file is written and read with kotlinx.serialization (architecture.md §5, no D5 record).
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}
