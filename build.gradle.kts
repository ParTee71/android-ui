import com.android.build.api.dsl.LibraryExtension
import java.time.Duration
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    base
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.spotless)
}

// Kodstil (ktlint + compose-rules) som i apparna – bara lokalt, inte i CI: ./gradlew spotlessApply före push.
val ktlintRules = mapOf(
    "ktlint_code_style" to "android_studio",
    "max_line_length" to "off",
    "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
    "compose_allowed_composition_locals" to "LocalExtendedColors,LocalAppShapes,LocalAppTypography,LocalTopBarStatus",
    // Samma undantag som i apparna: komponenternas API flyttas oförändrat (ingen app får ändras här).
    "ktlint_compose_parameter-naming" to "disabled",
    "ktlint_compose_lambda-param-event-trailing" to "disabled",
    "ktlint_compose_multiple-emitters-check" to "disabled",
    "ktlint_compose_modifier-naming" to "disabled"
)
spotless {
    kotlin {
        target("ui-*/src/**/*.kt")
        ktlint(libs.versions.ktlint.get())
            .editorConfigOverride(ktlintRules)
            .customRuleSets(listOf(libs.compose.rules.ktlint.get().toString()))
    }
    kotlinGradle {
        target("*.gradle.kts", "ui-*/*.gradle.kts")
        ktlint(libs.versions.ktlint.get()).editorConfigOverride(ktlintRules)
    }
}

// Gemensam byggkonfiguration för båda modulerna – enda stället. Modulfilerna har bara det som är unikt.
val javaVersion = JavaVersion.VERSION_17
val githubPackages = "https://maven.pkg.github.com/ParTee71/android-ui"

subprojects {
    group = rootProject.property("group").toString()
    version = rootProject.property("version").toString()

    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions.jvmTarget.set(JvmTarget.fromTarget(javaVersion.toString()))
    }

    plugins.withId("com.android.library") {
        extensions.configure<LibraryExtension> {
            compileSdk = 37
            defaultConfig { minSdk = 30 }
            compileOptions {
                sourceCompatibility = javaVersion
                targetCompatibility = javaVersion
            }
            buildFeatures { compose = true }
            testOptions {
                unitTests {
                    isIncludeAndroidResources = true
                    all {
                        it.systemProperty("robolectric.graphicsMode", "NATIVE")
                        // Robolectric 4.17 på SDK 36 skriver i FileDescriptor via jdk.internal.access.
                        it.jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
                    }
                }
            }
            publishing {
                singleVariant("release") { withSourcesJar() }
            }
        }

        // Publicering: GitHub Packages, se.partee71.ui:<modulnamn>:<version ur gradle.properties>.
        // Inloggningen kommer från miljön (CI: GITHUB_ACTOR/GITHUB_TOKEN, lokalt GPR_USER/GPR_TOKEN) – aldrig från repot.
        apply(plugin = "maven-publish")
        afterEvaluate {
            extensions.configure<PublishingExtension> {
                publications {
                    create<MavenPublication>("release") {
                        from(components["release"])
                        artifactId = project.name
                        pom {
                            name.set(project.name)
                            description.set("Delade Compose-komponenter för ParTee71:s Android-appar")
                            url.set("https://github.com/ParTee71/android-ui")
                        }
                    }
                }
                repositories {
                    maven {
                        name = "GitHubPackages"
                        url = uri(githubPackages)
                        credentials {
                            username = providers.environmentVariable("GITHUB_ACTOR").orElse(providers.environmentVariable("GPR_USER")).orNull
                            password = providers.environmentVariable("GITHUB_TOKEN").orElse(providers.environmentVariable("GPR_TOKEN")).orNull
                        }
                    }
                }
            }
        }
    }

    tasks.withType<Test>().configureEach {
        // Ett test som hänger får inte äta upp CI-budgeten: tasken avbryts efter 4 min.
        timeout.set(Duration.ofMinutes(4))
        testLogging {
            events("started", "skipped", "failed")
            exceptionFormat = TestExceptionFormat.FULL
        }
    }
}

// Copy-paste-detektor (regel 4): PMD CPD för Kotlin via PMD:s CLI, samma tröskel som apparna (80 tokens).
// Gradle-pluginen de.aaschmid.cpd fungerar inte med Gradle 9.
val cpdMinimumTokens = 80
val cpdClasspath = configurations.create("cpdClasspath")
dependencies {
    add(cpdClasspath.name, libs.pmd.cli)
    add(cpdClasspath.name, libs.pmd.kotlin)
}
val cpdSources = fileTree(rootDir) {
    include("ui-*/src/**/*.kt")
    exclude("**/build/**")
}

val cpdCheck = tasks.register<JavaExec>("cpdCheck") {
    group = "verification"
    description = "Hittar kopierad Kotlin-kod (PMD CPD, minst $cpdMinimumTokens tokens)."
    val fileList = layout.buildDirectory.file("cpd/files.txt")
    val report = layout.buildDirectory.file("reports/cpd/cpd.txt")
    val root = rootDir
    val tokens = cpdMinimumTokens
    val sources: FileCollection = cpdSources
    val result = executionResult
    workingDir = root
    classpath = cpdClasspath
    mainClass.set("net.sourceforge.pmd.cli.PmdCli")
    inputs.files(sources).withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.file(report)
    outputs.cacheIf { true }
    isIgnoreExitValue = true
    argumentProviders.add(
        CommandLineArgumentProvider {
            listOf(
                "cpd", "--language", "kotlin", "--minimum-tokens", "$tokens",
                "--file-list", fileList.get().asFile.path,
                "--format", "text", "--report-file", report.get().asFile.path
            )
        }
    )
    doFirst {
        fileList.get().asFile.apply {
            parentFile.mkdirs()
            writeText(sources.files.map { it.relativeTo(root).path }.sorted().joinToString("\n"))
        }
        report.get().asFile.parentFile.mkdirs()
    }
    doLast {
        // PMD: 0 = inga dubbletter, 4 = dubbletter hittade, annat = verktygsfel.
        val exit = result.get().exitValue
        if (exit != 0 && exit != 4) throw GradleException("cpdCheck: PMD avslutades med kod $exit (se loggen ovan).")
        if (exit == 4) {
            val text = report.get().asFile.takeIf { it.exists() }?.readText().orEmpty()
            throw GradleException(
                "cpdCheck: kopierad kod hittad (regel 4). Bryt ut till en delad byggsten i stället för att höja tröskeln.\n$text"
            )
        }
    }
}

tasks.named("check") { dependsOn(cpdCheck) }
