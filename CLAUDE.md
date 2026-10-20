# android-ui – projektets grundregler

> Gemensamt Compose-bibliotek (Material 3 Expressive) för ReseApoteket och Dagboken:
> `:ui-theme` (tema-API), `:ui-common` (list-, detalj- och formulärtillstånd, fel-API),
> `:ui-components` (komponenterna och ramarna) och `:ui-testing` (ramarnas kontraktstester för
> apparnas skärmar). Publiceras till GitHub Packages.
> Vad som finns och hur en app använder det: [README.md](README.md).

Pluginen **android@claude-android** gäller här som i apparna (android-dev, compose-expert,
accessibility-compose, android-gradle-logic, implement-issue, refine-issue, release).

**Svarslängd:** håll chattsvar minimala. Kod, commits och PR-beskrivningar skrivs normalt.

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
