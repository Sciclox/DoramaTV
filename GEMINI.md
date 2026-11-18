# GEMINI Context & Rules for APK-DoramaTV

- Native Android (Kotlin 2.2.10, Jetpack Compose, AGP 9.5.1).
- Main modules in `android-app/`: `:core`, `:app` (Android TV), `:app-mobile` (Mobile).
- Autonomous Verification Rule: Always execute and verify `.\gradlew.bat compileDebugKotlin` or `.\gradlew.bat testDebugUnitTest` inside `android-app/` before asking the user for review.
- Self-Healing: Inspect stack traces upon build failure, fix code automatically, and repeat until `BUILD SUCCESSFUL`.
- See `AGENTS.md` and `docs/PLAN.md` for detailed guidelines.
