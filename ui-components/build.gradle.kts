plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.roborazzi)
}

// Kompilering, SDK-nivåer, testflaggor och publicering står i rotens build.gradle.kts.
android {
    namespace = "se.partee71.ui.components"
}

roborazzi {
    outputDir.set(file("src/test/screenshots"))
}

dependencies {
    api(project(":ui-theme"))

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(libs.konsist)
}

// UiConsistencyTest läser källorna i båda modulerna, regelfilerna och README i roten – en ändring där kör om testet.
tasks.withType<Test>().configureEach {
    inputs.dir(rootProject.file("ui-theme/src/main")).withPathSensitivity(PathSensitivity.RELATIVE).withPropertyName("themeSources")
    inputs.dir(file("src/main")).withPathSensitivity(PathSensitivity.RELATIVE).withPropertyName("componentSources")
    inputs.files(rootProject.files("ui-forbidden.txt", "ui-allowlist.txt", "README.md"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("regel4Files")
}
