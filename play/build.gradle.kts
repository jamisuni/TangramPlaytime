// :play — the Play screen in Compose (architecture.md §3); the DEV aid lives in src/debug only (G-08, O-08).
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "app.tangramplaytime.play"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
        // The device kit (play/src/androidTest) runs under the AndroidX JUnit runner (tasks.md § WO-003 TASK-012).
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":contracts"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    testImplementation(libs.junit)
    // The real lock engine (WO-001) and the bundled puzzle library (WO-002) for the JVM kit only; main
    // code depends on :contracts alone and gets both injected by :app (G-12; tasks.md § WO-003 TASK-010).
    testImplementation(project(":kernel"))
    testImplementation(project(":content"))
    // The Compose device kit (play/src/androidTest): the same engine and library, device-test only
    // (tasks.md § WO-003 TASK-012). All from the catalog (architecture.md §5); no Material (D2/D5).
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.ext.junit)
    // Pins Espresso to the catalog's 3.7.0 (as :app does): the 3.5.x that compose ui-test pulls in reflects
    // `InputManager.getInstance()`, which no longer exists on the API 37 dev image.
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(project(":kernel"))
    androidTestImplementation(project(":content"))
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
