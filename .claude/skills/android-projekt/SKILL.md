---
name: android-projekt
description: android-ui:s repodetaljer för pluginens skills och agenter (android:implement-issue, android:refine-issue, android:release, agenten android:granskare) – repo, basgren, vad som körs före push, granskning, versionshöjning och publicering till GitHub Packages. Ladda tillsammans med en android:-skill när arbetet gäller biblioteket. Trigger-ord: android-ui, komponent, bibliotek, publicera, GitHub Packages, version, release, PR, granskning, issue.
---

# android-ui – repodetaljer

Reglerna står i [CLAUDE.md](../../../CLAUDE.md). Här står bara det pluginens skills slår upp per repo.

## Arbetsflöde (android:implement-issue)

- Repo `partee71/android-ui`, basgren **`main`** (inte `master`).
- Före push: `export LC_ALL=C.UTF-8` och `./gradlew testDebugUnitTest verifyRoborazziDebug cpdCheck`,
  plus `./gradlew spotlessApply`. CI (`ci.yml`) kör samma sak i ett jobb.
- Ny komponent eller parameter: Robolectric-test, Roborazzi-skärmdump (ljust + mörkt) och raden i
  README:s komponentlista. Mockup först om komponenten syns i apparna (`android:mockup`).

## Granskning (agenten android:granskare)

Granska mot CLAUDE.md:s sju regler i stället för apparnas fem: inga hårdkodade färger, former eller
typsnitt; varje skillnad mellan apparna är en parameter vars standard bevarar dagens utseende; test
och skärmdump per publik komponent; versionen höjd; versionerna i `gradle/libs.versions.toml` lika
med apparnas. Ingen KRAVLISTA, ingen datasäkerhetsregel (biblioteket lagrar inget).

## Issues (android:refine-issue)

Issuet säger vilka appar som påverkas och om de behöver höja sitt beroende efteråt (ett issue per
app). **Agent · modell · effort** som i `android:ai-model-choice`.

## Release (android:release)

- Versionen står i `gradle.properties` (`version=`) och raden i README:s **Versionshistorik**; höjs i
  varje PR som ändrar biblioteket (major = brytande API, minor = ny komponent/parameter, patch = fix).
- Publicering: `publish.yml` på `main`, via `workflow_dispatch` (skapar taggen `v<version>`) eller en
  pushad tagg. Resultatet är paketen i GitHub Packages och taggen; ingen GitHub Release skapas.
  Länka taggen (`https://github.com/partee71/android-ui/tree/v<version>`) och körningen.
- Ingen APK och ingen signering.
