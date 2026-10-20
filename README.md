# android-ui

Gemensamt Compose-bibliotek för ParTee71:s Android-appar (ReseApoteket och Dagboken, senare
eventuellt Fonder): ett tema-API och de delade komponenterna, så att samma element ser ut och
beter sig likadant i alla appar och bara finns på ett ställe. Varje app behåller sina egna
färger, former och typsnitt och skickar in dem i temat.

**Version:** 0.5.0 · Material 3 `1.5.0-beta01` (Expressive) · Compose BOM `2026.09.00` ·
Kotlin `2.4.20` · AGP `9.4.1` · compileSdk 37 · minSdk 30.

## Moduler

| Artefakt | Paket | Innehåll |
|---|---|---|
| `se.partee71.ui:ui-theme` | `se.partee71.ui.theme` | `AppTheme`, `Spacing`, `AppShapes`, `AppTypography`, `ExtendedColors`, `Tone`, `ToneColors` |
| `se.partee71.ui:ui-common` | `se.partee71.ui.common` | ramarnas tillstånd utan UI: `UiError`/`ErrorMapper`, `ListUiState`, `DetailUiState`, `EditorState`, `EditorLoader`, `shownAfter` |
| `se.partee71.ui:ui-components` | `se.partee71.ui.components` | komponenterna och ramarna nedan (drar in `ui-theme` och `ui-common`) |
| `se.partee71.ui:ui-testing` | `se.partee71.ui.testing` | ramarnas kontraktstester, för appens enhetstester (`testImplementation`) |

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

och i versionskatalogen/byggfilen `implementation("se.partee71.ui:ui-components:0.5.0")` och
`testImplementation("se.partee71.ui:ui-testing:0.5.0")`.

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

## ui-common

Ren tillståndslogik för listor, detaljer och formulär – ingen Firestore, Hilt eller appkod.
Testas med JUnit och Turbine.

- **Fel:** `UiError(cause, message, retryable)` är det enda fel-API:t i ramarna. Appen skriver en
  `ErrorMapper` en gång (`map(Throwable)` från `DataError`, `notFound()`). `UiError` jämförs på
  identitet: samma fel två gånger i rad visas två gånger.
- **Listor:** `ListUiState` (Loading, Empty, Content, Error), `asListUiState(errors[, retry])`,
  `ListLoader`, `ListEvent.Retry`; detaljer: `DetailUiState`, `asDetailUiState`, `DetailLoader`.
- **Formulär:** `EditorState` (validering, ändrat-läge, `saveUnchanged`, läsfel, spara/kör) och
  `EditorLoader` (läsning, "Försök igen", `project`, `prepare`, `afterLoad`).
- **`Flow<Boolean>.shownAfter(delay)`** – visar en indikator först efter en fördröjning.

## Ramarna

Varje listskärm, redigeringsskärm och detaljskärm i apparna byggs på en av tre ramar, så att laddning,
tomt läge, fel, "Försök igen", "Släng ändringar?", sparfel och arkivering beter sig likadant överallt.

```kotlin
EntityListScreen(title, state: ListUiState<T>, empty: EmptyContent, add: AddAction?, key, …,
    onRetry, group: ((T) -> ListGroup)?, archive: ListArchive?, subtitle, onBack, actions, header,
    subgroup, filter, topBarSize = TopBarSize.Large, footer, row)
EntityEditScreen(title, state: EditorUiState<*>, events: EditorEvents, onSave, onClose, …,
    saveLabel, onArchive, onRestore, delete: DeleteAction?, onRetry, onSaved = onClose, formError,
    horizontalMargin = Spacing.xl, content)
EntityDetailScreen(state: DetailUiState<T>, header: ((T) -> DetailHeader)?, onBack: (() -> Unit)?, …,
    title, onEdit, menu, onRetry, error: UiError?, onErrorShown, subtitle, actions, snackbar,
    sectionGap = Spacing.m, leading, content)
```

| Skillnad mellan apparna | Parameter | ReseApoteket | Dagboken |
|---|---|---|---|
| Lägg till-knapp | `add: AddAction(label, onClick, menu)`; `null` = ingen knapp, varken i listan eller i tomt läge | `AddAction(…)` (förr `onAdd` + `EmptyContent.actionLabel` + `addMenu`) | `AddAction` eller `null` |
| Kolumner | `ListGroup.columns` är ett tak; så många kolumner om minst 260 dp som får plats används | som förut | nytt: en kolumn på telefon |
| Filter, sidfot, postkort, räknare | `filter`, `footer`, `ListGroup.cards`, `ListGroup.showCount`, `topBarSize` | – | som förut |
| Arkiveringsfel | `ListArchive.error: UiError` | som förut | förr `failure: Failure` |
| Formulärets sidomarginal | `horizontalMargin` | `Spacing.xl` (standard) | `Spacing.l` |
| Formulärfel överst | `formError` | – | som förut |
| Tangentbordet | `imePadding` före `verticalScroll` (fältet hamnar ovanför tangentbordet) | nytt | som förut |
| Detaljens flikläge | `onBack = null`: stor topprad som fälls ihop, `title`, `subtitle`, `actions`, `snackbar`, plats för verktygsraden | – | som förut |
| Detaljens avstånd mellan sektioner | `sectionGap` | `Spacing.m` (standard) | `Spacing.l` |
| Hopfällbar grupp | rubriken minst 48 dp, läses som knapp ("Fäll ut"/"Fäll ihop"), chevronen fjädrar | nytt | som förut |

- **Arkivering:** `ArchiveActions(scope, errors, setArchived, undoFormat, errorMessage)` är logiken i
  listans ViewModel (svep, Ångra, "Visa arkiverade", `offerUndo` för annat än arkivering);
  `visible(source) { it.archived }` filtrerar och `collectAsListArchive()` ger listans `ListArchive`.
  Raderna sveps med `SwipeToHide`; `UndoSnackbar` visas av listramen.
- **Lägg till-knappens tagg:** `ENTITY_LIST_ADD_TAG`, som kontraktet använder för att pröva att knappen saknas.

## ui-testing – kontraktstesterna

En skärm som använder en ram kör ramens kontrakt i sitt Robolectric-test och testar sedan bara det
som är unikt för skärmen. Kontrakten prövar ramens beteende genom appens skärm, med appens tema och fel:

| Kontrakt | Prövar |
|---|---|
| `runListScreenContract(environment, item, itemText, emptyTitle, addLabel: String?, screen)` | laddar; tomt läge med lägg till-knapp (eller ingen knapp med `addLabel = null`); fel med felets text och "Försök igen"; innehåll med lägg till ovanför listan |
| `runEditScreenContract(environment, editor, makeInvalid?, makeValid, invalidMessage?, saveLabel, screen)` | "Spara" aktiv först när giltigt och ändrat; fältfel efter ändring; "Släng ändringar?" vid bakåtpilen och systemets bakåt; sparfel som snackbar (kvitterat); lyckad sparning stänger (kvitterat); bakåt utan ändringar stänger direkt |
| `runDetailScreenContract(environment, value, valueText, editable = true, tab = false, screen)` | laddar utan Redigera; läsfel med "Försök igen"; något som inte finns utan "Försök igen"; innehåll med Redigera och tillbakapil (ingen pil i flikläge) |

Appen skriver sin miljö en gång i testkoden:

```kotlin
val contracts = ContractEnvironment(AppErrors, DataError.Offline) { ReseApoteketTheme(content = it) }

@Test
fun `listan uppfyller listkontraktet`() = rule.runListScreenContract(contracts, item, "Solkräm", "Inga familjeartiklar än", "Lägg till") { state, onAdd, onRetry ->
    FamilyItemsContent(state, onAdd = onAdd, onRetry = onRetry)
}
```

Modulens egna tester kör kontrakten mot bibliotekets ramar – och prövar att de fäller en skärm som
inte använder ramen rätt.

## Komponenter

| Komponent | Vad | Skillnad mot apparnas tidigare version |
|---|---|---|
| `AddSplitButton` | "Lägg till ▾" – huvudåtgärd och pil med fler val | Menyn är en lucka: `menu = items.asSplitMenu()` (ReseApoteket) i stället för Dagbokens `menuItems` |
| `AppBottomSheet` | Ark med rubrik; innehållet rullar alltid. `error` (`UiError`), `dirty` ("Släng ändringar?" vid bakåt, svep och tryck utanför), `canDismiss`, `hide`, `footer` (knappraden står fast i botten) | Dagbokens parametrar; ReseApotekets rullning gäller också med `footer` |
| `AppButton` | Den enda knappen: Primary, Secondary, Text, Destructive; `loading`, `compact` | – |
| `AppCard` | Kortet: `tone`, `contentColor` (standard `null`: ärver omgivande `LocalContentColor`; Dagboken `onSurface`) | ReseApotekets `ApotekCard` heter nu `AppCard` |
| `AppDivider` | Avdelare mellan rader i ett kort | – |
| `AppFilterChip` | Filterchip; `icon`, `onLongClick`, `value` (värde efter etiketten), `emphasized` (det aktiva bland flera valda), `pending` (`ChipPending`: väntande läge, tonat med streckad kant, ikon och tilläggstext) | `chipColors()` är publik för egna chips |
| `AppFloatingToolbar` | Toppnivånavigeringen: flytande flikar; `action` (`ToolbarAction`, huvudknapp i `secondary` till höger – Dagboken). `LocalBottomClearance`, `FloatingToolbarClearance`, `NoBottomClearance` | – |
| `AppIconButton` | Ikonknapp, minst 48 dp, alltid `contentDescription` | – |
| `AppLoading` | Expressive laddningsindikator mitt på ytan | – |
| `AppMenu` | Menyn bakom ⋮ (`AppMenuItem`: `icon`, `destructive`, `section` (rubrik), `checked` (kryssrad som håller menyn öppen), `supporting`/`supportingTone` (undertext), `muted` (dämpad etikett)) | `section`, `checked` från Dagboken; `asSplitMenu()` från ReseApoteket |
| `AppMenuPopup` | Själva menyn, för egna knappar som öppnar en meny | Publik (Dagboken använder den direkt) |
| `AppSegmentedChoice` | Ett val av få alternativ som segmentknappar | – |
| `AppSnackbarHost` | Den enda platsen där meddelanden visas | – |
| `AppTextField` | Det enda textfältet; `maxLength` (`AppTextFieldDefaults.SHORT_MAX_LENGTH` 200 / `LONG_MAX_LENGTH` 2000), `maxLines`, `suffix`, `valueIsPlaceholder` | Dagboken anger `maxLength = 5000` för långa fält |
| `AppTopBar` | Skärmrubriken, stor eller liten | Synkindikatorn är appens: den sätts som `LocalTopBarStatus` (eller parametern `status`) |
| `CardSegment` | Ett segment i ett sammanfogat kort (`top`, `bottom`), `contentColor` | – |
| `CheckRow` | Kryssrad med morfande kryss; `onClick` (krysset blir egen knapp), `accent`, `note`, `menu` (långtryck + ⋮, går före bubblan), `below`, `enabled` | Dagbokens `note`, `menu`, `below`, `enabled` |
| `ChipRow` | En rad chips som bryts eller rullar | – |
| `ChoiceChips` | Ett val av flera som chips; `onClear` (Dagboken). Överlagring för flerval (`selected: Set<T>`, `onToggle`) | Flervalet är nytt (Dagboken #321) |
| `ChoiceGrid` | Val i ett rutnät med egen cell; `selectionRing` (Dagboken: ring i primärfärg) | – |
| `ColorSwatchPicker` | Färgval; `marker` krävs: `SwatchMarker.RingAndCheck(checkColor)` (ReseApoteket) eller `GridRing` (Dagboken) | `marker` saknar standard – bockens färg är appens |
| `Confetti` | Kort konfettiregn som belöning | `colors` (standard `AppTheme.extendedColors.accents`) |
| `ConfirmDialog` | Bekräftelse; `destructive`, `onDecline`, `dismissLabel` (`null` = bara en knapp), `content` (fält under texten) | Dagbokens `dismissLabel: String?` och `content` |
| `DateField` | Datumfält (kotlinx `LocalDate`, "lör 19 dec 2026"); `emptyLabel`, `onClear` (✕ "Till …"), `context` för TalkBack | Dagbokens parametrar |
| `DeleteConfirmDialog` | Permanent radering av en `DeleteAction` (titel, text, "Radera" i felfärg); titeln t.ex. `R.string.delete_title_format` ("Radera %s?") | Var appens egen (`DeleteAction` från `EntityEditScreen`) |
| `DiscardChangesDialog` | "Släng ändringar?" – "Släng" / "Fortsätt redigera" | Var appens egen |
| `EmojiPicker` | Välj en emoji ur `emojis` med `ChoiceGrid`s ring | Listan krävs – apparnas listor (`PERSON_EMOJIS`, `DEFAULT_EMOJIS`) stannar i appen; ReseApotekets egen markering ersätts av ringen |
| `EmptyState` | Tomt läge med ikon, rubrik, text och knapp; `compact` (Dagboken) | – |
| `EntityDetailScreen` | Detaljramen: huvud, sektioner, Redigera och meny; underskärm med `title`, flikläge med `onBack = null` | Union av apparnas; se Ramarna |
| `EntityEditScreen` | Redigeringsramen med `EditorState`: Spara, "Släng ändringar?", sparfel, arkivera/återställ/radera, `formError`, tangentbordet | Union av apparnas; se Ramarna |
| `EntityListScreen` | Listramen: laddar, tomt, fel, grupper i kort, kolumner efter bredd, arkivering, `filter`, `footer` | Union av apparnas; se Ramarna |
| `ErrorSnackbar` | Visar ett `UiError` en gång; `template` (t.ex. "Synken nekades: %1$s"), `duration` | Tar `UiError` i stället för appens `DataError` |
| `ExampleChips` | Exempel att trycka in i ett fält | – |
| `FieldError` | Felraden under ett fält | – |
| `GroupLabel` | Etikett över en grupp | – |
| `InfoPill` | Pill med ton; `trailingIcon`, `role`, `onClickLabel`, `colors` | – |
| `ItemRow` | Den enda listraden: `leading`, `trailing`, `tinted`, `done`, `accent`, `navigates`, `inactive`, `below`, `titleHighlight`, `icon`, `belowAlignsWithTitle`. Kapad titel: långtryck visar hela titeln och undertexten i en bubbla | Dagbokens `icon`, `belowAlignsWithTitle`; bubblan nu även i Dagboken. Avståndet titel–undertext: `LocalItemRowTextGap` (standard 2 dp; Dagboken `Spacing.xs`) |
| `ItemRowText` | Listradens titel och undertext, för appens egna kort | Dagbokens `RowText` |
| `LabeledGroup` | Etikett, hjälptext och innehåll; `helperAbove` (Dagboken) | – |
| `LeadingSlot` | 48 dp-ruta för det ledande elementet, så att titlarna linjerar | – |
| `LoadErrorState` | Läsfel med "Försök igen" (bara om `retryable`) | Tar `UiError` |
| `MessageSnackbar` | Visar en text en gång i `AppSnackbarHost` | – |
| `MorphingCheck` | Krysset som morfar från ring till kaka med bock (bara visning) | – |
| `NoteIndicator` | Anteckningsikon som visar anteckningen i en dialog | – |
| `PausableRow` | Rad som kan pausas: typ som pill, nedtonad "Pausat"; `pillTone` krävs | Tonen är appens (ReseApoteket `Tone.Blue`, Dagboken `Tone.Neutral`) |
| `PickerField` | Fält som öppnar en väljare; `emptyLabel`, `contentLabel` | – |
| `QuantityStepper` | − värde +; `step`, `valueText`, `buttonSize` (`QuantityStepperDefaults.ButtonSize` 44 dp, `TouchTargetButtonSize` 48 dp) | – |
| `SectionHeader` | Sektionsrubrik med antal; `countTone` | – |
| `SuggestionField` | Textfält med förslag (`Suggestion`) i ett kort under, antalet läses upp | – |
| `SwipeToHide` | Arkivera/dölj med svep åt vänster, och samma åtgärd för TalkBack | – |
| `SwitchControl` | Själva växeln, för appens kort med reglage | – |
| `SwitchRow` | Inställningsrad med växel; `onClick` (växeln blir egen kontroll), `inactive` (tonar ned texten, inte växeln) | Dagbokens `inactive` |
| `TimeField` | Tidsfält (kotlinx `LocalTime`) som öppnar `TimePickerPopup` | – |
| `TimePickerPopup` | Tidsväljaren som dialog | – |
| `UndoSnackbar` | "Arkiverad – Ångra" för `UndoRequest`; `format`, `onRequestDismissed` | – |

Strängarna (svenska) och ikonerna ligger i `ui-components/src/main/res`. En app som har en
resurs med samma namn (t.ex. `ic_add` eller `more_options`) ersätter bibliotekets – så behåller
varje app sin ikonstil utan parameter.

## Bygga och testa

```bash
./gradlew testDebugUnitTest          # Robolectric-beteende, UiConsistencyTest, temat, kontrakten i ui-testing
./gradlew verifyRoborazziDebug       # skärmdumpar mot ui-components/src/test/screenshots
./gradlew recordRoborazziDebug       # spela in referenserna – bara vid avsiktlig visuell ändring
./gradlew cpdCheck                   # copy-paste-detektor (PMD CPD, 80 tokens)
./gradlew spotlessApply              # kodstil (ktlint + compose-rules), före push – inte i CI
./gradlew publishToMavenLocal        # prova en app mot en lokal version
```

Robolectric (SDK 36) kräver JDK 21; koden byggs för Java 17. Skärmdumparna tas med ett testtema
med ReseApotekets palett (`ui-components/src/test/.../testing/TestTheme.kt`).

## Publicering

Versionen står i `gradle.properties` (`version=`). `.github/workflows/publish.yml` publicerar alla fyra
artefakterna till GitHub Packages, antingen när en tagg `v<version>` pushas eller manuellt
(Actions → Publicera → Run workflow på `main`), som då skapar taggen själv. Molnsessioner kan inte
pusha taggar och använder därför den manuella vägen.

## Versionshistorik

| Version | Ändring |
|---|---|
| 0.5.0 | `AppFilterChip` får `value`, `emphasized` och `pending` (`ChipPending`), `AppMenuItem` får `supporting`, `supportingTone` och `muted` – för Dagbokens symptomstyrka och vid behov-markeringar. Standardvärdena bevarar utseendet |
| 0.4.0 | Ramarna `EntityListScreen`, `EntityEditScreen` och `EntityDetailScreen` med båda apparnas parametrar, `ArchiveActions`/`ListArchive` för arkivering, och modulen `ui-testing` med kontraktstesterna för apparnas skärmar. Kolumner efter bredd och tangentbordshanteringen gäller båda apparna |
| 0.3.0 | 19 komponenter till, som glidit isär mellan apparna, i en version med båda apparnas parametrar: listrader (`ItemRow` med bubbla vid kapad titel, `CheckRow`, `PausableRow`, `SwitchRow`), `SuggestionField`, `AppBottomSheet` med "Släng ändringar?" och fast knapprad, `AppMenu` med avdelningar och kryssrader, `AppFloatingToolbar` med huvudknapp, `ConfirmDialog`, `DeleteConfirmDialog`, `DiscardChangesDialog`, `DateField` med rensa-knapp och `EmojiPicker` |
| 0.2.0 | Modulen `ui-common` (fel-API, list-, detalj- och formulärtillstånd) och 22 komponenter till: fält, val, kort, pills, snackbars, tomt läge och läsfel; flerval i `ChoiceChips` |
| 0.1.0 | Första versionen: tema-API:t och elva komponenter ur ReseApoteket och Dagboken |
