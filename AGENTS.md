# APK-DoramaTV — Reglas de Ingeniería Agéntica y Arquitectura

Esta es una aplicación nativa de **Android en Kotlin multi-módulo**, orientada tanto a **Android TV** como a **dispositivos móviles** utilizando **Jetpack Compose**.

---

## 1. Estructura del Proyecto y Módulos

El código nativo reside dentro del directorio `android-app/`. Nunca ejecutes comandos de Gradle en la raíz del repositorio; hazlo siempre dentro de `android-app/`.

* `:core` — Lógica de negocio compartida, modelos de datos `@Immutable`, clientes HTTP, parsers, extractores y repositorios comunes.
* `:app` — Interfaz y navegación para **Android TV** (soporte estricto de D-Pad / control remoto, Compose for TV / Leanback, foco visual claro a 3 metros).
* `:app-mobile` — Interfaz táctil para **móviles y tablets** (Jetpack Compose Mobile, orientación adaptable, gestos y BottomSheets).
* `tools/extractors-lab/` — Laboratorio aislado de scripts experimentales en JavaScript y volcados HTML (ignorado en búsquedas de código Kotlin).
* `apks/` — Directorio de salida centralizado para binarios versionados listos para producción o prueba.

---

## 2. Pila Tecnológica

* **Lenguaje:** Kotlin 2.2.10
* **UI:** Jetpack Compose (Compose Compiler 2.2.10, Compose BOM 2025.02.00)
* **Build System:** Android Gradle Plugin 9.5.1 + Gradle Kotlin DSL (`*.gradle.kts`)
* **SDK:** Android SDK moderno con soporte de foojay toolchains (Java 17)

---

## 3. Las Tres Reglas de Oro Arquitectónicas (Anti-God Files)

1. **Límite estricto de archivos monolíticos:**  
   Prohibido agregar nueva lógica de negocio dentro de `PlayerScreen.kt` (TV/Mobile), `DetailScreen.kt` o `MovieRepository.kt`. Toda nueva funcionalidad debe extraerse a componentes visuales modulares (<20 KB) o UseCases en `:core`.
2. **Validación granular por módulo:**
   - Si el cambio es en extractores, red o datos: `.\gradlew.bat :core:testDebugUnitTest`
   - Si el cambio es en Android TV: `.\gradlew.bat :app:testDebugUnitTest`
   - Si el cambio es en Mobile: `.\gradlew.bat :app-mobile:compileDebugKotlin`
3. **Paridad TV / Mobile:**  
   Cualquier cambio en contratos, modelos o repositorios de `:core` debe compilar ambos targets antes de entregar:
   ```powershell
   .\gradlew.bat :app:compileDebugKotlin :app-mobile:compileDebugKotlin
   ```

---

## 4. Bucle Obligatorio de Autocorrección (*Self-Healing Build Loop*)

> [!IMPORTANT]
> **Prohibido pedir al usuario que actúe como QA manual de compilación o sintaxis.**
> Como agente, tienes acceso a la terminal y debes validar tus propios cambios antes de dar una tarea por finalizada.

### Reglas del Ciclo de Vida:
1. **Verificar antes de entregar:** Ejecuta siempre los comandos de compilación o test unitario pertinentes tras modificar archivos en `android-app/`.
2. **Auto-remediación:** Si la salida devuelve `BUILD FAILED` o un error de compilación:
   - Lee el log de error o la traza completa.
   - Corrige el código en un bucle autónomo.
   - Vuelve a ejecutar la validación. Solo presenta el resultado al usuario cuando la salida sea `BUILD SUCCESSFUL`.
3. **Diagnóstico de Runtime con ADB:**  
   Si una aplicación falla en tiempo de ejecución en un emulador o dispositivo conectado, inspecciona el error con:
   ```powershell
   adb logcat -d *:E
   ```

---

## 5. Flujo de Trabajo Basado en Especificaciones (Spec-Driven)

1. Para tareas de arquitectura o refactorizaciones complejas, consulta o actualiza siempre `docs/PLAN.md`.
2. Mantén la separación estricta de responsabilidades:
   - La lógica de red, parsing y datos pertenece a `:core`.
   - La UI de televisión nunca debe mezclar componentes táctiles diseñados para móvil.
   - La navegación de Android TV debe garantizar que cada elemento interactivo tenga un estado visual enfocado (`isFocused`).

---

## 6. Protocolo de Orquestación Multiagente

El sistema de desarrollo opera con una arquitectura multiagente coordinada por un **Orchestrator** central y cuatro **Roles Especialistas**:

```
                  ┌─────────────────────────────────────────┐
                  │          ORCHESTRATOR (Líder)           │
                  │   Planifica, asigna y valida paridad    │
                  └──────┬────────────┬─────────────┬───────┘
                         │            │             │
        ┌────────────────┴┐   ┌───────┴────────┐   ┌┴────────────────┐
        │  Extractor-     │   │     TV-UI-     │   │   Mobile-UI-    │
        │  Specialist     │   │   Specialist   │   │   Specialist    │
        │  (:core)        │   │   (:app)       │   │   (:app-mobile) │
        └────────┬────────┘   └───────┬────────┘   └────────┬────────┘
                 │                    │                     │
                 └────────────────────┼─────────────────────┘
                                      │
                         ┌────────────┴─────────────┐
                         │       QA-SENTINEL        │
                         │ Self-Healing, Tests, APKs│
                         └──────────────────────────┘
```

### 6.1 Matriz de Roles y Responsabilidades

| Rol | Alcance / Directorios | Responsabilidades Principales | Criterio de Éxito |
| :--- | :--- | :--- | :--- |
| **`Orchestrator`** | Todo el repositorio | Desglosa requerimientos, diseña contratos de datos, delega subtareas, sincroniza git y actualiza `docs/PLAN.md`. | Paridad global y entrega sin intervención manual del usuario. |
| **`extractor-specialist`** | `core/.../extractor/`, `tools/extractors-lab/` | Implementación de scrapers, desofuscación JavaScript (P.A.C.K.E.R.), regex de video y extractores bajo la interfaz `VideoExtractor`. | `.\gradlew.bat :core:testDebugUnitTest` en verde con tests unitarios offline. |
| **`tv-ui-specialist`** | `app/src/main/` | UI para Android TV, D-Pad focus chains (`FocusRequester`, `focusProperties`, `onKeyEvent`), componentes 10-foot y Compose for TV. | Foco D-Pad bidireccional fluido sin trampas de foco y 60 FPS estables. |
| **`mobile-ui-specialist`**| `app-mobile/src/main/` | UI táctil para smartphones y tablets, gestos swipe/tap, BottomSheets, soporte vertical/horizontal y adaptabilidad. | `.\gradlew.bat :app-mobile:compileDebugKotlin` en verde y UX táctil fluida. |
| **`qa-sentinel`** | `android-app/`, `scripts/`, `apks/` | Ejecución de suites unitarias cruzadas, resolución autónoma de errores de build, diagnóstico ADB `logcat` y generación de APKs con `build_apks.bat`. | Todas las suites `BUILD SUCCESSFUL` y binarios listos en `apks/`. |

---

### 6.2 Protocolo de Traspaso (Handoff Contracts)

Para evitar desbordamiento de contexto y fallas de compilación entre agentes:

1. **Contrato Core ➔ UI:**
   - Ningún especialista de UI (`tv-ui` o `mobile-ui`) debe implementar interfaces para una nueva fuente de datos hasta que el especialista de `:core` haya definido modelos `@Immutable` y sus UseCases tengan tests unitarios en verde.
2. **Contrato UI ➔ QA:**
   - Ningún archivo visual debe superar las 400 líneas o 25 KB. Si crece, el especialista de UI debe dividirlo en sub-componentes modulares dentro de una subcarpeta `components/<feature>/` antes de solicitar la validación final.
3. **Contrato QA ➔ Release:**
   - La tarea solo se marca como finalizada cuando `.\gradlew.bat :core:testDebugUnitTest :app:testDebugUnitTest :app-mobile:testDebugUnitTest` resulte en `BUILD SUCCESSFUL` y los APKs se encuentren exportados en `/apks/`.

---

### 6.3 Playbooks de Ejecución Estándar

#### Playbook 1: Añadir o Reparar un Servidor de Video (Stream Extractor)
1. **Fase de Análisis:** `extractor-specialist` prueba la extracción en `tools/extractors-lab/` (o con fixtures HTML locales).
2. **Implementación:** Crea `NuevoVideoExtractor.kt` implementando la interfaz `VideoExtractor` en `:core`.
3. **Registro:** Añade el extractor con su prioridad en `ExtractorRegistry.kt`.
4. **Prueba Unitaria:** Escribe `NuevoExtractorTest.kt` validando que resuelve streams sin conexión activa a la red.
5. **Verificación:** Ejecuta `.\gradlew.bat :core:testDebugUnitTest`.

#### Playbook 2: Nueva Pantalla o Funcionalidad End-to-End
1. **Dominio:** Definir UseCase y modelos `@Immutable` en `:core`. Validar con test unitario en JVM.
2. **Android TV:** `tv-ui-specialist` diseña la pantalla en `:app`, organizando la UI en componentes en `app/ui/components/<feature>/`.
3. **Mobile:** `mobile-ui-specialist` diseña la versión táctil en `:app-mobile`, reutilizando los mismos UseCases de `:core`.
4. **Validación:** `qa-sentinel` compila ambos targets (`.\gradlew.bat :app:compileDebugKotlin :app-mobile:compileDebugKotlin`) y exporta los APKs con `build_apks.bat`.
