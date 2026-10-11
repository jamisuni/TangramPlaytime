// Root build: plugin versions come from gradle/libs.versions.toml; every module applies its own.
// The Kotlin JVM plugin is declared here so its version (2.4.20) is the one AGP's built-in
// Kotlin support uses in the Android modules, and the Compose compiler plugin matches it.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
