plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

// Kompilering, SDK-nivåer, testflaggor och publicering står i rotens build.gradle.kts.
// Ren logik utan utseende: listtillstånd, formulärlogik och felen ramarna visar. Komposablerna ligger i ui-components.
android {
    namespace = "se.partee71.ui.common"
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    // @Immutable/@Stable på tillstånden, så att appens skärmar kan hoppa över oförändrade.
    api(libs.androidx.compose.runtime)
    api(libs.androidx.annotation)
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.collections.immutable)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
