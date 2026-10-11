// :content — puzzle assets, the tangram-puzzle/1 parser, validity check and ordering
// (IPuzzleLibrary implementation; architecture.md §3, ADR-004: parsed with kotlinx.serialization).
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.tangramplaytime.content"
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
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}
