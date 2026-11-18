# GEMINI Context & Rules for APK-DoramaTV

- Native Android (Kotlin 2.2.10, Jetpack Compose, AGP 9.5.1, Gradle Kotlin DSL).
- Multi-Module in `android-app/`: `:core` (shared logic & data), `:app` (Android TV D-Pad), `:app-mobile` (Mobile touch).
- Anti-God Files: Prohibido agregar nueva lógica a `PlayerScreen.kt`, `DetailScreen.kt` o `MovieRepository.kt`. Toda funcionalidad debe residir en componentes modulares (<20 KB) o UseCases en `:core`.
- Multi-Agent Orchestration Protocol:
  - Antigravity acts as Lead Orchestrator delegando a especialistas: `extractor-specialist`, `tv-ui-specialist`, `mobile-ui-specialist`, `qa-sentinel`.
  - Handoff contract: Ningún agente de UI implementa features sin modelos `@Immutable` y tests verdes en `:core`.
- Granular Module Verification:
  - Core / Extractors: `.\gradlew.bat :core:testDebugUnitTest`
  - TV: `.\gradlew.bat :app:testDebugUnitTest`
  - Mobile: `.\gradlew.bat :app-mobile:compileDebugKotlin`
  - Parity check: `.\gradlew.bat :app:compileDebugKotlin :app-mobile:compileDebugKotlin`
- Autonomous Verification & Self-Healing: Always execute and verify the relevant Gradle command in `android-app/` before asking the user for review. Fix build issues autonomously until `BUILD SUCCESSFUL`.
- See `AGENTS.md` and `docs/PLAN.md` for full playbooks and architecture guidelines.
