# Plan Activo de Desarrollo (APK-DoramaTV)

## Hoja de Ruta de Refactorización y Mejoras (Diseñada por 4 Argon)

### Fase 1: Higiene de Contexto y Reglas de Oro (Completada)
- [x] Mover los 49 scripts `.js` y fixtures `.json` de la raíz a `tools/extractors-lab/` para no saturar las búsquedas y contexto de Antigravity.
- [x] Configurar las Tres Reglas de Oro en `AGENTS.md` y `GEMINI.md`:
  - Límite estricto a God Files (`PlayerScreen.kt` y `MovieRepository.kt`).
  - Validación modular por comando específico (`:core:testDebugUnitTest`, `:app:testDebugUnitTest`, `:app-mobile:compileDebugKotlin`).
  - Paridad obligatoria TV/Mobile al modificar `:core`.

---

### Fase 2: Estandarización de VideoExtractor en `:core` (Completada)
- [x] Definir interfaz común `VideoExtractor`:
  - [`VideoExtractor.kt`](file:///c:/Users/Lenovo/OneDrive/Documentos/Proyectos/APK-DoramaTV/android-app/core/src/main/java/com/tvpro/core/data/extractor/VideoExtractor.kt)
  - [`ExtractedStream.kt`](file:///c:/Users/Lenovo/OneDrive/Documentos/Proyectos/APK-DoramaTV/android-app/core/src/main/java/com/tvpro/core/data/extractor/ExtractedStream.kt)
- [x] Implementar extractores desacoplados con prioridades:
  - [`VoeVideoExtractor.kt`](file:///c:/Users/Lenovo/OneDrive/Documentos/Proyectos/APK-DoramaTV/android-app/core/src/main/java/com/tvpro/core/data/extractor/VoeVideoExtractor.kt) (Prioridad 1)
  - [`OkRuVideoExtractor.kt`](file:///c:/Users/Lenovo/OneDrive/Documentos/Proyectos/APK-DoramaTV/android-app/core/src/main/java/com/tvpro/core/data/extractor/OkRuVideoExtractor.kt) (Prioridad 2)
  - [`DoodVideoExtractor.kt`](file:///c:/Users/Lenovo/OneDrive/Documentos/Proyectos/APK-DoramaTV/android-app/core/src/main/java/com/tvpro/core/data/extractor/DoodVideoExtractor.kt) (Prioridad 3)
  - [`VidhideVideoExtractor.kt`](file:///c:/Users/Lenovo/OneDrive/Documentos/Proyectos/APK-DoramaTV/android-app/core/src/main/java/com/tvpro/core/data/extractor/VidhideVideoExtractor.kt) (Prioridad 4)
- [x] Implementar [`ExtractorRegistry.kt`](file:///c:/Users/Lenovo/OneDrive/Documentos/Proyectos/APK-DoramaTV/android-app/core/src/main/java/com/tvpro/core/data/extractor/ExtractorRegistry.kt) con Fallback automático en cascada (`extractFirstAvailable`) y puente `toStreamItem()`.
- [x] Suite de pruebas unitarias implementada y validada en verde:
  - [`ExtractorRegistryTest.kt`](file:///c:/Users/Lenovo/OneDrive/Documentos/Proyectos/APK-DoramaTV/android-app/core/src/test/java/com/tvpro/core/data/extractor/ExtractorRegistryTest.kt) (`:core:testDebugUnitTest` -> `BUILD SUCCESSFUL`).
- [x] Validación de paridad TV / Mobile (`:app:compileDebugKotlin` y `:app-mobile:compileDebugKotlin` -> `BUILD SUCCESSFUL`).

---

### Fase 3: Desduplicación de Recursos y Fuentes (Completada)
- [x] Eliminar fuentes duplicadas entre `:app` y `:app-mobile` (`res/font/`), centralizándolas en `:core/src/main/res/font/` (ahorro de 3.2 MB).

---

### Fase 4: Modularización de ViewModels, Dominio y UI del Reproductor (Completada)
- [x] Configurar suite de pruebas unitarias en `:app-mobile/build.gradle.kts` e implementar [`MovieUiUtilsTest.kt`](file:///c:/Users/Lenovo/OneDrive/Documentos/Proyectos/APK-DoramaTV/android-app/app-mobile/src/test/java/com/tvpro/mobile/ui/MovieUiUtilsTest.kt) (`:app-mobile:testDebugUnitTest` -> `BUILD SUCCESSFUL`).
- [x] Extraer lógica de dominio del reproductor a `:core`:
  - [`PlayerTimeUtils.kt`](file:///c:/Users/Lenovo/OneDrive/Documentos/Proyectos/APK-DoramaTV/android-app/core/src/main/java/com/tvpro/core/domain/PlayerTimeUtils.kt) (Formateo unificado de tiempo).
  - [`StreamRanker.kt`](file:///c:/Users/Lenovo/OneDrive/Documentos/Proyectos/APK-DoramaTV/android-app/core/src/main/java/com/tvpro/core/domain/StreamRanker.kt) (Detección de enlaces caídos y ordenamiento inteligente por servidor).
  - [`EpisodeNavigator.kt`](file:///c:/Users/Lenovo/OneDrive/Documentos/Proyectos/APK-DoramaTV/android-app/core/src/main/java/com/tvpro/core/domain/EpisodeNavigator.kt) (Navegación entre temporadas y episodios).
- [x] Suite de pruebas de dominio validada en `:core`:
  - [`PlayerDomainTest.kt`](file:///c:/Users/Lenovo/OneDrive/Documentos/Proyectos/APK-DoramaTV/android-app/core/src/test/java/com/tvpro/core/domain/PlayerDomainTest.kt) (`:core:testDebugUnitTest` -> `BUILD SUCCESSFUL`).
- [x] Fragmentar componentes visuales del reproductor:
  - [`MobilePlayerGestures.kt`](file:///c:/Users/Lenovo/OneDrive/Documentos/Proyectos/APK-DoramaTV/android-app/app-mobile/src/main/java/com/tvpro/mobile/ui/components/player/MobilePlayerGestures.kt) (Extracción de `DoubleTapSeekOverlay`, `InteractiveVerticalBar`, `AudioWaveIndicator`).
  - [`PlayerTrackModels.kt`](file:///c:/Users/Lenovo/OneDrive/Documentos/Proyectos/APK-DoramaTV/android-app/app/src/main/java/com/tvpro/app/ui/components/player/PlayerTrackModels.kt) (Modelos de audio/subtítulos y normalizador de nombres de servidor para TV).
  - Unificación de formateadores de tiempo en `PlayerScreen.kt` (TV y Mobile) delegando a `PlayerTimeUtils`.
- [x] Validación completa de suites de pruebas:
  - `:app-mobile:testDebugUnitTest` -> `BUILD SUCCESSFUL`.
  - `:app:testDebugUnitTest` -> `BUILD SUCCESSFUL`.
- [x] Validación de paridad TV / Mobile (`:app:compileDebugKotlin` y `:app-mobile:compileDebugKotlin` -> `BUILD SUCCESSFUL`).

---

### Fase 5: Unificación de ViewModels y Casos de Uso en `:core` (Completada)
- [x] Crear caso de uso unificado [`GetHomeCatalogUseCase.kt`](file:///c:/Users/Lenovo/OneDrive/Documentos/Proyectos/APK-DoramaTV/android-app/core/src/main/java/com/tvpro/core/domain/GetHomeCatalogUseCase.kt) en `:core`:
  - Manejo concurrente de taxonomías (`emision`, `latino`, `sub`, `cdrama`, `jdrama`, `peliculas`, `doramasflix`, catálogo local).
  - Optimización de backdrops de alta resolución.
  - Generación de secciones (Top 10, Doramas en emisión, géneros temáticos) y fallback offline garantizado.
- [x] Pruebas unitarias para `GetHomeCatalogUseCase`:
  - [`GetHomeCatalogUseCaseTest.kt`](file:///c:/Users/Lenovo/OneDrive/Documentos/Proyectos/APK-DoramaTV/android-app/core/src/test/java/com/tvpro/core/domain/GetHomeCatalogUseCaseTest.kt) (`:core:testDebugUnitTest` -> `BUILD SUCCESSFUL`).
- [x] Refactorizar [`HomeViewModel.kt`](file:///c:/Users/Lenovo/OneDrive/Documentos/Proyectos/APK-DoramaTV/android-app/app/src/main/java/com/tvpro/app/ui/screens/HomeViewModel.kt) (TV) y [`MobileHomeViewModel.kt`](file:///c:/Users/Lenovo/OneDrive/Documentos/Proyectos/APK-DoramaTV/android-app/app-mobile/src/main/java/com/tvpro/mobile/ui/screens/MobileHomeViewModel.kt) (Mobile) para delegar en `GetHomeCatalogUseCase`:
  - Se eliminaron más de 200 líneas de lógica de red y async duplicada en ambas capas de presentación.
- [x] Validación total de suites de pruebas en simultáneo:
  - `.\gradlew.bat :core:testDebugUnitTest :app:testDebugUnitTest :app-mobile:testDebugUnitTest` -> `BUILD SUCCESSFUL`.
- [x] Validación de paridad TV / Mobile (`:app:compileDebugKotlin :app-mobile:compileDebugKotlin` -> `BUILD SUCCESSFUL`).
