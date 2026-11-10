# android-ui – projektets grundregler

> Gemensamt Compose-bibliotek (Material 3 Expressive) för ReseApoteket och Dagboken:
> `:ui-theme` (tema-API), `:ui-common` (list-, detalj- och formulärtillstånd, fel-API),
> `:ui-components` (komponenterna och ramarna) och `:ui-testing` (ramarnas kontraktstester för
> apparnas skärmar). Publiceras till GitHub Packages.
> Vad som finns och hur en app använder det: [README.md](README.md). Repodetaljerna som pluginens
> skills och agenter slår upp (basgren `main`, kontroller, publicering): skill `android-projekt`.

Pluginen **android@claude-android** gäller här som i apparna (android-dev, compose-expert,
accessibility-compose, android-gradle-logic, implement-issue, refine-issue, release).

**Svarslängd:** håll chattsvar minimala. Kod, commits och PR-beskrivningar skrivs normalt.

**Ärliga svar:** säg emot när Mats har fel, och säg varför. Inget beröm för frågan, inga
artighetsfraser, inga ursäkter, ingen utfyllnad. Säg vad som är osäkert eller inte kontrollerat.
Ett nej mjukas inte upp.

**Plan före kod bara för större ändringar:** nytt beteende, flera lager, datamodell eller ett
vägval → plan i ett issue och Mats ok ("kör" eller `plan-godkänd`). Små fixar och dokument går
direkt till PR med raden `Liten ändring: <varför>`, och tråden mergar när CI är grön. Osäker →
större. Skill `android:arbetsregler` är enda källan.

## Reglerna

1. **Samma element ser likadant ut överallt, och varje sak finns på ett ställe.** En komponent
   här ersätter apparnas egna; ingen app behåller en kopia. Utseendet kommer bara från temat
   (`AppTheme.extendedColors`, `AppTheme.shapes`, `AppTheme.typography`, `Spacing`) – aldrig
   hårdkodade färger, former eller typsnitt i `ui-components`. Kontrolleras av `UiConsistencyTest`
   (mönstren i `ui-forbidden.txt`, undantag bara i `ui-allowlist.txt` med motivering och
   användarens ok i PR:en) och `cpdCheck`.
2. **Varje skillnad mellan apparna är en parameter vars standardvärde bevarar dagens utseende.**
   Ingen app får ändra utseende av en flytt. Klarar en parameter inte skillnaden: fråga, gissa inte.
   Appens färger, typsnitt och sådant bara en app har stannar i appen.
3. **Tester och skärmdumpar per komponent.** Robolectric-beteendetest och Roborazzi-skärmdump
   (ljust + mörkt, testtemat med ReseApotekets palett) för varje publik komponent; komponenten
   står också i README:s komponentlista. Flaky tester är buggar – gör dem deterministiska.
4. **CI-budget:** en PR kostar högst ~8 Actions-minuter (`ci.yml`: ett jobb). Inga nya dyra steg
   utan beslut; `spotlessApply` körs lokalt före push, inte i CI.
5. **En PR i taget**, mot `main`. Nästa påbörjas först när den förra är mergad.
6. **Versionen höjs i varje PR som ändrar biblioteket** (`version` i `gradle.properties`, raden i
   README:s versionshistorik): major = brytande API, minor = ny komponent/parameter, patch = fix.
   Publicering: `publish.yml` på `main`, manuellt (skapar taggen `v<version>`) eller via pushad tagg.
7. **Versionerna följer apparna:** Compose BOM, Material 3, Kotlin och AGP i
   `gradle/libs.versions.toml` ska vara desamma som i apparna.

## Modellval

Opus 5.5 (`claude-opus-5-5`) är standard för sessionen och varje agent. Fable 5.2
(`claude-fable-5-2`) är ok när det behövs – svår arkitektur eller design, knepiga integrationer,
långa agentuppgifter, buggar Opus inte klarat – men försiktigt för kostnadens skull: medium eller
high effort, aldrig max eller fast mode. Säg varför och vilken effort innan du byter till Fable 5.2;
planera med Fable, genomför med Opus när det går. Fable 5.1 används inte längre. Tabell, priser och
kostnadsspärrar: skill `android:ai-model-choice` (enda källan).

## Bygg & test

```bash
export LC_ALL=C.UTF-8                # annars kraschar kotlinc på svenska testnamn
./gradlew testDebugUnitTest verifyRoborazziDebug cpdCheck
./gradlew recordRoborazziDebug       # bara vid avsiktlig visuell ändring – checka in bilderna
./gradlew spotlessApply              # före push
./gradlew publishToMavenLocal        # prova i en app
```

Android SDK i sessionen: `local.properties` med `sdk.dir=/opt/android-sdk` (gitignorerad). JDK 21.
Maven Central nås i molnsessioner via Googles spegel: `.claude/hooks/session-start.sh` lägger
`maven-central-mirror.init.gradle.kts` i `~/.gradle/init.d/`, så att Gradle slipper 429. CI påverkas inte.
