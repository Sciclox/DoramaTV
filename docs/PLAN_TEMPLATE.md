# Plantilla de Especificación Técnica (PLAN / SPEC)

> Esta plantilla sirve como contrato técnico entre la fase de arquitectura (diseñada con **4 Argon: Código y análisis** en la web de Gemini o con razonamiento profundo) y la fase de ejecución autónoma en Antigravity.

## 1. Resumen y Objetivo
* **Feature / Requerimiento:** [Descripción breve del objetivo de negocio o técnico]
* **Módulo(s) Afectado(s):** [`:core`, `:app` (TV), `:app-mobile`]
* **Arquitecto:** Gemini Web (4 Argon)

---

## 2. Contratos de Datos y Arquitectura
* **Modelos / DTOs:** [Nuevos campos, serialización, tipos inmutables]
* **Casos de Uso / Repositorios:** [Firmas de métodos, tipos de retorno `Flow<T>`, manejo de errores]
* **Estados de UI (Compose State):** [Data classes de estado: `UiState.Loading`, `Success`, `Error`]

---

## 3. Plan Paso a Paso de Implementación
* [ ] **Paso 1:** [Modificar / Crear archivos en `:core`]
* [ ] **Paso 2:** [Escribir pruebas unitarias en `src/test/`]
* [ ] **Paso 3:** [Implementar pantallas / componentes en Compose]
* [ ] **Paso 4:** [Integrar navegación y estados de foco (TV)]

---

## 4. Criterios de Aceptación y Validación Automática
* **Comando de compilación:** `cd android-app; .\gradlew.bat compileDebugKotlin`
* **Comando de pruebas:** `cd android-app; .\gradlew.bat testDebugUnitTest`
* **Condición de éxito:** Salida con código 0 y `BUILD SUCCESSFUL`.
