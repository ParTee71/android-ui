#!/bin/bash
set -euo pipefail

# Körs bara i fjärrsessioner (Claude på webben/telefonen) – lokala Android Studio-miljöer
# lämnas orörda.
if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

# Repots rot utifrån skriptets plats – fungerar även när sessionens projektrot är en
# föräldrakatalog med flera repon.
cd "$(dirname "$0")/../.."

# Maven Central via Googles spegel (429 från repo.maven.apache.org i sessionen).
mkdir -p "$HOME/.gradle/init.d"
cp .claude/hooks/maven-central-mirror.init.gradle.kts "$HOME/.gradle/init.d/"
echo "Gradle: Maven Central via Googles spegel (~/.gradle/init.d/maven-central-mirror.init.gradle.kts)"
