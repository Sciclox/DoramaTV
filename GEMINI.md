# GEMINI Context & Rules for APK-DoramaTV

- Native Android (Kotlin 2.2.10, Jetpack Compose, AGP 9.5.1).
- Main modules in `android-app/`: `:core`, `:app` (Android TV), `:app-mobile` (Mobile).
- Anti-God Files: Prohibido agregar nueva lógica a `PlayerScreen.kt` o `MovieRepository.kt`. Extraer en UseCases o componentes modulares.
- Granular Module Verification:
  - Core / Extractors: `.\gradlew.bat :core:testDebugUnitTest`
  - TV: `.\gradlew.bat :app:testDebugUnitTest`
  - Mobile: `.\gradlew.bat :app-mobile:compileDebugKotlin`
  - Core changes (Parity check): `.\gradlew.bat :app:compileDebugKotlin :app-mobile:compileDebugKotlin`
- Autonomous Verification Rule: Always execute and verify the relevant Gradle command in `android-app/` before asking the user for review.
- Self-Healing: Inspect stack traces upon build failure, fix code automatically, and repeat until `BUILD SUCCESSFUL`.
- See `AGENTS.md` and `docs/PLAN.md` for detailed guidelines.
