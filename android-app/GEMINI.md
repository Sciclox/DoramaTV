# Android App Engineering Rules

- Working directory for builds: `android-app/`
- Build check: `.\gradlew.bat compileDebugKotlin`
- Test check: `.\gradlew.bat testDebugUnitTest`
- Architecture:
  - `:core`: Common data, models, repositories, network parsers.
  - `:app`: Android TV UI (D-Pad navigation focus, 10-foot experience).
  - `:app-mobile`: Mobile UI (touch, Jetpack Compose Mobile).
- Never delegate compilation or syntax bug-fixing to the user. Always run Gradle autonomously and self-correct.
