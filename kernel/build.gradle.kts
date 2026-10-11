// :kernel — the TYPE-001/003/004/005/006/007 rules and the ILockEngine implementation (architecture.md §3).
// Pure Kotlin/JVM: no Android plugin, no android.* import (guardrail G-12).
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":contracts"))
    testImplementation(libs.junit)
}
