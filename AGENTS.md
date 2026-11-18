# APK-DoramaTV — Reglas de Ingeniería Agéntica y Arquitectura

Esta es una aplicación nativa de **Android en Kotlin multi-módulo**, orientada tanto a **Android TV** como a **dispositivos móviles** utilizando **Jetpack Compose**.

---

## 1. Estructura del Proyecto y Módulos

El código nativo reside dentro del directorio `android-app/`. Nunca ejecutes comandos de Gradle en la raíz del repositorio; hazlo siempre dentro de `android-app/`.

* `:core` — Lógica de negocio compartida, modelos de datos, clientes HTTP, parsers y repositorios comunes.
* `:app` — Interfaz y navegación para **Android TV** (soporte estricto de D-Pad / control remoto, Compose for TV / Leanback, foco visual claro).
* `:app-mobile` — Interfaz táctil para **móviles y tablets** (Jetpack Compose Mobile, orientación adaptable).

---

## 2. Pila Tecnológica

* **Lenguaje:** Kotlin 2.2.10
* **UI:** Jetpack Compose (Compose Compiler 2.2.10)
* **Build System:** Android Gradle Plugin 9.5.1 + Gradle Kotlin DSL (`*.gradle.kts`)
* **SDK:** Android SDK moderno con soporte de foojay toolchains

---

## 3. Bucle Obligatorio de Autocorrección (*Self-Healing Build Loop*)

> [!IMPORTANT]
> **Prohibido pedir al usuario que actúe como QA manual de compilación o sintaxis.**
> Como agente, tienes acceso a la terminal y debes validar tus propios cambios antes de dar una tarea por finalizada.

### Comandos de Validación (ejecutados en `android-app/`):

```powershell
# 1. Comprobación rápida de tipos y compilación Kotlin:
.\gradlew.bat compileDebugKotlin

# 2. Ejecución de suite de tests unitarios:
.\gradlew.bat testDebugUnitTest

# 3. Compilación de APK debug (si se requiere validar ensamble completo):
.\gradlew.bat assembleDebug
```

### Reglas del Ciclo de Vida:
1. **Verificar antes de entregar:** Ejecuta siempre `.\gradlew.bat compileDebugKotlin` o `.\gradlew.bat testDebugUnitTest` tras modificar archivos en `android-app/`.
2. **Auto-remediación:** Si la salida devuelve `BUILD FAILED` o un error de compilación:
   - Lee el log de error o la traza completa.
   - Corrige el código en un bucle autónomo.
   - Vuelve a ejecutar la validación. Solo presenta el resultado al usuario cuando la salida sea `BUILD SUCCESSFUL`.
3. **Diagnóstico de Runtime con ADB:**  
   Si una aplicación falla en tiempo de ejecución en un emulador o dispositivo conectado, inspecciona el error con:
   ```powershell
   adb logcat -d *:E
   ```
   No solicites que el usuario copie y pegue el logcat si puedes leerlo directamente por ADB.

---

## 4. Flujo de Trabajo Basado en Especificaciones (Spec-Driven)

1. Para tareas de arquitectura o refactorizaciones complejas, consulta o crea primero `docs/PLAN.md`.
2. Mantén la separación estricta de responsabilidades:
   - La lógica de red, parsing y datos pertenece a `:core`.
   - La UI de televisión nunca debe mezclar componentes táctiles diseñados para móvil.
   - La navegación de Android TV debe garantizar que cada elemento interactivo tenga un estado visual enfocado (`isFocused`).
