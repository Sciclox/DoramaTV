# Android App Engineering Rules

- Working directory for builds: `android-app/`
- Anti-God Files:
  - Do NOT bloat `PlayerScreen.kt` (currently 436 KB TV / 149 KB Mobile) or `MovieRepository.kt` (91.9 KB).
  - Extract all new features into UseCases or separate composables.
- Granular Verification:
  - Data / Extractors: `.\gradlew.bat :core:testDebugUnitTest`
  - TV UI: `.\gradlew.bat :app:testDebugUnitTest`
  - Mobile UI: `.\gradlew.bat :app-mobile:compileDebugKotlin`
  - Parity: `.\gradlew.bat :app:compileDebugKotlin :app-mobile:compileDebugKotlin`
- Self-Healing: Never delegate compilation or syntax bug-fixing to the user. Always run Gradle autonomously and self-correct.
