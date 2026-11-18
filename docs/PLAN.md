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

### Fase 3: Desduplicación de Recursos y Fuentes (Siguiente Paso)
- [ ] Eliminar fuentes duplicadas entre `:app` y `:app-mobile` (`res/font/`).

---

### Fase 4: Modularización de ViewModels y PlayerScreen
- [ ] Extraer lógica común del reproductor a `PlayerViewModel` en `:core`.
- [ ] Fragmentar `PlayerScreen.kt` en sub-componentes.
- [ ] Crear suite de pruebas unitarias en `:app-mobile/src/test/`.
