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

---

### Fase 6: Reorganización Limpia, Pipeline Automatizado y Modularización de DetailScreen (Completada)
- [x] **Higiene Total de Workspace:**
  - Se movieron 32 capturas de pantalla sueltas a `screenshots/`.
  - Se movieron los APKs y se creó la carpeta dedicada `apks/`.
  - Se movieron los volcados y scrapers a `tools/extractors-lab/`.
  - La raíz del proyecto quedó impecable con solo 5 archivos esenciales y 5 carpetas organizadas.
- [x] **Pipeline de Compilación y Exportación Automatizada:**
  - Script [`scripts/build_apks.ps1`](file:///c:/Users/Lenovo/OneDrive/Documentos/Proyectos/APK-DoramaTV/scripts/build_apks.ps1) y lanzador [`build_apks.bat`](file:///c:/Users/Lenovo/OneDrive/Documentos/Proyectos/APK-DoramaTV/build_apks.bat).
  - Compilación paralela y exportación automática a `apks/` con nombrado versionado.
- [x] **CI/CD con GitHub Actions:**
  - Creado [`.github/workflows/android.yml`](file:///c:/Users/Lenovo/OneDrive/Documentos/Proyectos/APK-DoramaTV/.github/workflows/android.yml) para ejecutar tests y ensamblaje automáticamente en cada push a `main`.
- [x] **Desacoplamiento de `MovieRepository.kt`:**
  - Extraído [`JsUnpacker.kt`](file:///c:/Users/Lenovo/OneDrive/Documentos/Proyectos/APK-DoramaTV/android-app/core/src/main/java/com/tvpro/core/data/extractor/JsUnpacker.kt) (algoritmos de desofuscación P.A.C.K.E.R., ROT13, Base64).
  - Extraído [`MovieDetailCacheManager.kt`](file:///c:/Users/Lenovo/OneDrive/Documentos/Proyectos/APK-DoramaTV/android-app/core/src/main/java/com/tvpro/core/data/repository/MovieDetailCacheManager.kt) (gestión de cachés en memoria LRU).
- [x] **Fragmentación de TV `DetailScreen.kt` (Reducción de 1,901 líneas a ~450 líneas):**
  - [`DetailTopBar.kt`](file:///c:/Users/Lenovo/OneDrive/Documentos/Proyectos/APK-DoramaTV/android-app/app/src/main/java/com/tvpro/app/ui/components/detail/DetailTopBar.kt) (Barra superior de navegación y sincronización).
  - [`DetailHeroContent.kt`](file:///c:/Users/Lenovo/OneDrive/Documentos/Proyectos/APK-DoramaTV/android-app/app/src/main/java/com/tvpro/app/ui/components/detail/DetailHeroContent.kt) (Metadatos, insignias, sinopsis y barra de progreso).
  - [`DetailActionButtons.kt`](file:///c:/Users/Lenovo/OneDrive/Documentos/Proyectos/APK-DoramaTV/android-app/app/src/main/java/com/tvpro/app/ui/components/detail/DetailActionButtons.kt) (Botones de acción, foco D-Pad y estados de disponibilidad).
  - [`DetailTabsHeader.kt`](file:///c:/Users/Lenovo/OneDrive/Documentos/Proyectos/APK-DoramaTV/android-app/app/src/main/java/com/tvpro/app/ui/components/detail/DetailTabsHeader.kt) (Pestañas de navegación inferior e indicadores).
  - [`DetailEpisodesRail.kt`](file:///c:/Users/Lenovo/OneDrive/Documentos/Proyectos/APK-DoramaTV/android-app/app/src/main/java/com/tvpro/app/ui/components/detail/DetailEpisodesRail.kt) (Selector de temporadas y carrusel de episodios con restauración de foco).
  - [`DetailSimilarRail.kt`](file:///c:/Users/Lenovo/OneDrive/Documentos/Proyectos/APK-DoramaTV/android-app/app/src/main/java/com/tvpro/app/ui/components/detail/DetailSimilarRail.kt) (Carrusel de películas similares).
  - [`DetailExtrasContent.kt`](file:///c:/Users/Lenovo/OneDrive/Documentos/Proyectos/APK-DoramaTV/android-app/app/src/main/java/com/tvpro/app/ui/components/detail/DetailExtrasContent.kt) (Galería de escenas y ficha técnica).
  - [`DetailLightboxOverlay.kt`](file:///c:/Users/Lenovo/OneDrive/Documentos/Proyectos/APK-DoramaTV/android-app/app/src/main/java/com/tvpro/app/ui/components/detail/DetailLightboxOverlay.kt) (Modal a pantalla completa de vista previa).
  - [`DetailFormatters.kt`](file:///c:/Users/Lenovo/OneDrive/Documentos/Proyectos/APK-DoramaTV/android-app/app/src/main/java/com/tvpro/app/ui/components/detail/DetailFormatters.kt) (Formateadores y enums).
- [x] **Estabilidad de Compose y 60 FPS en Android TV:**
  - Se agregó `compileOnly("androidx.compose.runtime:runtime:1.7.6")` a `:core`.
  - Se anotaron todos los modelos de dominio (`MovieItem`, `EpisodeItem`, `StreamItem`, `WatchHistoryItem`, etc.) con `@Immutable` para habilitar el smart skipping del compilador de Compose.
- [x] **Verificación Total de Suites:**
  - `.\gradlew.bat :core:testDebugUnitTest :app:testDebugUnitTest :app-mobile:testDebugUnitTest` -> `BUILD SUCCESSFUL`.

---

### Fase 7: Extractores Nativos en `:core` y Modularización de TV PlayerScreen (Completada)
- [x] **Extractores Nativos en `:core`:**
  - [`StreamtapeVideoExtractor.kt`](file:///c:/Users/Lenovo/OneDrive/Documentos/Proyectos/APK-DoramaTV/android-app/core/src/main/java/com/tvpro/core/data/extractor/StreamtapeVideoExtractor.kt) (Prioridad 5, MP4 directo con token).
  - [`FilemoonVideoExtractor.kt`](file:///c:/Users/Lenovo/OneDrive/Documentos/Proyectos/APK-DoramaTV/android-app/core/src/main/java/com/tvpro/core/data/extractor/FilemoonVideoExtractor.kt) (Prioridad 3, HLS m3u8 con desofuscación P.A.C.K.E.R.).
  - [`StreamwishVideoExtractor.kt`](file:///c:/Users/Lenovo/OneDrive/Documentos/Proyectos/APK-DoramaTV/android-app/core/src/main/java/com/tvpro/core/data/extractor/StreamwishVideoExtractor.kt) (Prioridad 4, HLS m3u8 con desofuscación P.A.C.K.E.R.).
  - Integración en [`ExtractorRegistry.kt`](file:///c:/Users/Lenovo/OneDrive/Documentos/Proyectos/APK-DoramaTV/android-app/core/src/main/java/com/tvpro/core/data/extractor/ExtractorRegistry.kt) con escala de prioridades calibrada (1 a 7).
  - Suite de 30 tests unitarios en [`ExtractorRegistryTest.kt`](file:///c:/Users/Lenovo/OneDrive/Documentos/Proyectos/APK-DoramaTV/android-app/core/src/test/java/com/tvpro/core/data/extractor/ExtractorRegistryTest.kt) (`:core:testDebugUnitTest` -> `BUILD SUCCESSFUL`).
- [x] **Modularización de TV `PlayerScreen.kt`:**
  - [`TvPlayerAudioSubtitleModal.kt`](file:///c:/Users/Lenovo/OneDrive/Documentos/Proyectos/APK-DoramaTV/android-app/app/src/main/java/com/tvpro/app/ui/components/player/TvPlayerAudioSubtitleModal.kt) (Modal cinema de pistas de audio y subtítulos).
  - [`TvPlayerEpisodesModal.kt`](file:///c:/Users/Lenovo/OneDrive/Documentos/Proyectos/APK-DoramaTV/android-app/app/src/main/java/com/tvpro/app/ui/components/player/TvPlayerEpisodesModal.kt) (Modal cinema de capítulos y escenas con navegación D-Pad).
  - Reducción de más de 1,720 líneas de `PlayerScreen.kt`.
- [x] **Validación de Paridad y Binarios:**
  - `.\gradlew.bat :core:testDebugUnitTest :app:testDebugUnitTest :app-mobile:compileDebugKotlin` -> `BUILD SUCCESSFUL`.
  - Binarios exportados en `apks/` (`DoramaTV-AndroidTV-v1.0.0.apk` y `DoramaTV-Mobile-v1.0.0.apk`).
