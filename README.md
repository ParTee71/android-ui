# android-ui

Gemensamt Compose-bibliotek för ParTee71:s Android-appar (ReseApoteket och Dagboken, senare
eventuellt Fonder): ett tema-API och de delade komponenterna, så att samma element ser ut och
beter sig likadant i alla appar och bara finns på ett ställe. Varje app behåller sina egna
färger, former och typsnitt och skickar in dem i temat.

**Version:** 0.1.0 · Material 3 `1.5.0-beta01` (Expressive) · Compose BOM `2026.09.00` ·
Kotlin `2.4.20` · AGP `9.4.1` · compileSdk 37 · minSdk 30.

## Moduler

| Artefakt | Paket | Innehåll |
|---|---|---|
| `se.partee71.ui:ui-theme` | `se.partee71.ui.theme` | `AppTheme`, `Spacing`, `AppShapes`, `AppTypography`, `ExtendedColors`, `Tone`, `ToneColors` |
| `se.partee71.ui:ui-components` | `se.partee71.ui.components` | komponenterna nedan (drar in `ui-theme`) |

## Lägga till i en app

Paketen ligger i GitHub Packages, som kräver inloggning även för läsning. I appens
`settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/ParTee71/android-ui")
            credentials {
                username = providers.environmentVariable("GITHUB_ACTOR").orElse(providers.environmentVariable("GPR_USER")).orNull
                password = providers.environmentVariable("GITHUB_TOKEN").orElse(providers.environmentVariable("GPR_TOKEN")).orNull
            }
            content { includeGroup("se.partee71.ui") }
        }
    }
}
```

och i versionskatalogen/byggfilen `implementation("se.partee71.ui:ui-components:0.1.0")`.

- **CI i appen:** `GITHUB_TOKEN` räcker när paketet gett apprepot läsåtkomst
  (paketets inställningar → *Manage Actions access*) och jobbet har `permissions: packages: read`.
- **Lokalt och i molnsessioner:** `GPR_USER` (GitHub-användaren) och `GPR_TOKEN` (en token med
  `read:packages`) i miljön. Token checkas aldrig in.

## Tema-API

```kotlin
@Composable
fun AppTheme(
    colorScheme: ColorScheme,
    extendedColors: ExtendedColors,
    shapes: AppShapes,
    typeScale: Typography,                                     // appens typskala med appens typsnitt
    typography: AppTypography = AppTypography.from(typeScale), // de namngivna stilarna
    motionScheme: MotionScheme = MotionScheme.expressive(),
    content: @Composable () -> Unit
)

AppTheme.extendedColors   // card, rowTint, toolbar, onToolbar, tones, accents
AppTheme.shapes           // card, cardSegment(top, bottom), row, pill, field, menu, sheet, smallTile, iconTile
AppTheme.typography       // screenTitle, bigNumber, headline, sectionTitle, quantity, itemTitle, itemSubtitle, body, pill, caption, button
AppTheme.tone(tone)       // ToneColors(container, content)
Spacing.xs … Spacing.xxl  // 4, 8, 12, 16, 24, 32 dp
```

- **Toner:** `Tone` är en nyckel. Biblioteket känner bara `Tone.Primary`; appen lägger till sina
  som tillägg, t.ex. `val Tone.Companion.Green get() = Tone("green")`, och ger dem färger i
  `ExtendedColors.tones`. En ton utan färger får primärtonen.
- **Former:** bara `cardRadius` måste anges (ReseApoteket 28 dp, Dagboken 22 dp); övriga former
  har apparnas gemensamma värden som standard.
- **Accentfärger:** `ExtendedColors.accents` är appens dekorativa färgserie (personfärger,
  valbara färger) – används av `Confetti`.
- Det som bara en app har (t.ex. Dagbokens energiskala, sömnstadier och känslotoner, `IconSize`)
  stannar i appen.

Appens tema blir en tunn omslutning:

```kotlin
@Composable
fun ReseApoteketTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) =
    AppTheme(
        colorScheme = if (darkTheme) AppColors.dark else AppColors.light,
        extendedColors = if (darkTheme) AppColors.darkExtended else AppColors.lightExtended,
        shapes = AppShapes(cardRadius = 28.dp),
        typeScale = AppTypeScale,
        content = content
    )
```

## Komponenter

| Komponent | Vad | Skillnad mot apparnas tidigare version |
|---|---|---|
| `AddSplitButton` | "Lägg till ▾" – huvudåtgärd och pil med fler val | Menyn är en lucka (`menu = { expanded, onDismiss -> AppMenuPopup(items, expanded, onDismiss) }`) i stället för `menuItems`, eftersom `AppMenu` ännu inte finns här |
| `AppButton` | Den enda knappen: Primary, Secondary, Text, Destructive; `loading`, `compact` | – |
| `AppDivider` | Avdelare mellan rader i ett kort | – |
| `AppIconButton` | Ikonknapp, minst 48 dp, alltid `contentDescription` | – |
| `AppLoading` | Expressive laddningsindikator mitt på ytan | – |
| `AppSegmentedChoice` | Ett val av få alternativ som segmentknappar | – |
| `AppSnackbarHost` | Den enda platsen där meddelanden visas | – |
| `AppTopBar` | Skärmrubriken, stor eller liten | Synkindikatorn är appens: den sätts som `LocalTopBarStatus` (eller parametern `status`) |
| `ChipRow` | En rad chips som bryts eller rullar | – |
| `Confetti` | Kort konfettiregn som belöning | `colors` (standard `AppTheme.extendedColors.accents`) |
| `SwipeToHide` | Arkivera/dölj med svep åt vänster, och samma åtgärd för TalkBack | – |

Strängarna (svenska) och ikonerna ligger i `ui-components/src/main/res`. En app som har en
resurs med samma namn (t.ex. `ic_add` eller `more_options`) ersätter bibliotekets – så behåller
varje app sin ikonstil utan parameter.

## Bygga och testa

```bash
./gradlew testDebugUnitTest          # Robolectric-beteende, UiConsistencyTest, temat
./gradlew verifyRoborazziDebug       # skärmdumpar mot ui-components/src/test/screenshots
./gradlew recordRoborazziDebug       # spela in referenserna – bara vid avsiktlig visuell ändring
./gradlew cpdCheck                   # copy-paste-detektor (PMD CPD, 80 tokens)
./gradlew spotlessApply              # kodstil (ktlint + compose-rules), före push – inte i CI
./gradlew publishToMavenLocal        # prova en app mot en lokal version
```

Robolectric (SDK 36) kräver JDK 21; koden byggs för Java 17. Skärmdumparna tas med ett testtema
med ReseApotekets palett (`ui-components/src/test/.../testing/TestTheme.kt`).

## Publicering

Versionen står i `gradle.properties` (`version=`). En tagg `v<version>` på `main` kör
`.github/workflows/publish.yml`, som publicerar båda artefakterna till GitHub Packages.

## Versionshistorik

| Version | Ändring |
|---|---|
| 0.1.0 | Första versionen: tema-API:t och elva komponenter ur ReseApoteket och Dagboken |
