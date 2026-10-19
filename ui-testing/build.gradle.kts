plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

// Kompilering, SDK-nivåer, testflaggor och publicering står i rotens build.gradle.kts.
// Kontraktstesterna för ramarna: apparna lägger till artefakten som testImplementation och kör
// kontrakten mot sina egna skärmar (Robolectric). Modulens egna tester kör dem mot bibliotekets ramar.
android {
    namespace = "se.partee71.ui.testing"
}

dependencies {
    api(project(":ui-components"))
    api(libs.androidx.compose.ui.test.junit4)
    implementation(libs.androidx.activity.compose)
    implementation(libs.junit)

    testImplementation(libs.robolectric)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
