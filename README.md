# DoramaTV 📺📱

Aplicación nativa para **Android TV** y **dispositivos móviles** desarrollada en **Kotlin** y **Jetpack Compose**. Diseñada para navegar, descubrir y reproducir doramas y series asiáticas con una experiencia optimizada para control remoto (10-foot UI) y pantallas táctiles.

---

## 🏗️ Arquitectura Multi-Módulo

El proyecto reside en `android-app/` y está dividido en tres módulos:

* **`:core`** — Lógica compartida, modelos de datos, clientes HTTP (OkHttp), parsers GraphQL y extractores de servidores de streaming (Voe, Doodstream, Vidhide, OkRu).
* **`:app`** — Interfaz para **Android TV** desarrollada con Jetpack Compose for TV y Leanback, con soporte nativo de D-Pad (control remoto) y estados de foco visual.
* **`:app-mobile`** — Interfaz táctil moderna y adaptable para **teléfonos y tablets** con Jetpack Compose Mobile.

---

## 🛠️ Pila Tecnológica

* **Lenguaje:** Kotlin 2.2.10
* **UI:** Jetpack Compose (Compose Compiler 2.2.10)
* **Reproductor:** Media3 / ExoPlayer (HLS, MP4, Dash)
* **Networking:** OkHttp 4.x + GraphQL client
* **Build System:** Android Gradle Plugin 9.5.1 + Gradle Kotlin DSL

---

## 🚀 Compilación y Validación

Ejecutar siempre desde el directorio `android-app/`:

```powershell
# 1. Comprobación rápida de compilación:
.\gradlew.bat compileDebugKotlin

# 2. Ejecución de suite de tests unitarios:
.\gradlew.bat testDebugUnitTest

# 3. Compilación de APKs debug (TV y Mobile):
.\gradlew.bat assembleDebug
```

---

## 📄 Documentación y Especificaciones

* [`AGENTS.md`](AGENTS.md) — Reglas maestras de ingeniería y bucles de autocorrección (*Self-Healing*).
* [`docs/PLAN.md`](docs/PLAN.md) — Especificación técnica activa y contratos de desarrollo (*Spec-Driven*).
