// :store — everything that survives a restart (IProgressStore implementation; architecture.md §3, G-13).
plugins {
    alias(libs.plugins.android.library)
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
    testImplementation(libs.junit)
}
