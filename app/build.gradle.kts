// :app — the application shell (architecture.md §3): wiring, resources, Clock implementation, manifest.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "app.tangramplaytime.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.tangramplaytime"
        minSdk = 31
        targetSdk = 37
        // The first upload (tasks.md § WO-009 S-58; decisions.md 2026-10-22 "The first upload's identity: …"):
        // version 1, "1.0.0"; every later upload raises versionCode.
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    // S-58: every language's resources stay in the app bundle's base APK, so an install from Play holds the same
    // resources as the tested APK (REQ-047 statement; decisions.md 2026-10-18, "The device's first locale" (S-36, Q7)).
    // No signing config (hard-stop 1): the AAB stays unsigned; the owner signs it with his upload key.
    bundle {
        language {
            enableSplit = false
        }
    }
}

// S-56: the store artifact (the AAB, Play's upload) waits on the release bar, `:content:checkReleasePuzzles`. The gate
// runs before the bundle is packaged, so a refused build writes no AAB. `assembleRelease`, `packageRelease` and every
// debug or test task never wait on it: V-01 and the release-APK kits read the release APK at every step.
//
// Code review S1 (reviews/WO-009-code-review.md; decisions.md 2026-10-22 "S1, S2, S3 fixed now …"): before the gate,
// the store build deletes the AAB an earlier build left in build/outputs/bundle/release/ — it may hold a puzzle the
// owner has since unmarked (REQ-039 rule 2) — so after a refusal there is no AAB to upload; a passing build writes it
// again. The gate runs after this delete (its `mustRunAfter`, content/build.gradle.kts).
val deleteReleaseBundle = tasks.register<Delete>("deleteReleaseBundle") {
    description = "Deletes the release AAB of an earlier build before the store build's gate runs (code review S1)."
    delete(layout.buildDirectory.dir("outputs/bundle/release"))
}

tasks.named { it == "bundleRelease" || it == "packageReleaseBundle" }.configureEach {
    dependsOn(deleteReleaseBundle, ":content:checkReleasePuzzles")
}

dependencies {
    implementation(project(":contracts"))
    implementation(project(":kernel"))
    implementation(project(":content"))
    implementation(project(":store"))
    implementation(project(":play"))

    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)

    testImplementation(libs.junit)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
