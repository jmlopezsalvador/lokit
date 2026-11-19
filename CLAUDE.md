# lokit — app de magia Android + iOS

App móvil para **Android y iOS** con un único código en Kotlin Multiplatform + Compose Multiplatform.
El usuario habla español: responde y comenta el código en español.

## Carpetas con reglas propias
- `HANDOFF.md`: estado, pendientes y problemas abiertos; al retomar, leerlo primero.
- `docs/`: `ARQUITECTURA.md` (fuente de verdad de versiones y reglas) y `SPEC.md` (especificación funcional).
- `data/`: material de referencia que aporta el usuario (se puede leer); `data/icons/` es el origen del icono (ya aplicado a `app/src/main/res/mipmap-*` y a `iosApp/.../AppIcon-1024.png`); `data/android_build/` y `data/ios_build/` no se versionan.
- `ignore/`: **ignorar por completo** (ver reglas).

## Reglas para Claude
- **Ignorar por completo la carpeta `ignore/`**: no leerla, no listarla, no buscar dentro, no usarla como referencia. Excluirla
  también de búsquedas (Glob/Grep) y del control de versiones. Es el cajón del usuario para material suelto.
- La base técnica es **`docs/ARQUITECTURA.md`** (versiones, estructura, Gradle, CI de iOS, reglas de código). Si algo choca con él, consultar al usuario.
- **Antes de lanzar el workflow de iOS en GitHub Actions, preguntar siempre al usuario** (cuesta cuota de macOS; se acumulan mejoras).
  Por defecto se lanza **sin tests** (el usuario prueba en su iPhone); solo con tests si él lo pide.
- No inventar funcionalidad: la fuente de verdad es `docs/SPEC.md` y lo que pida el usuario. Ante la duda, preguntar.
- **No hacer commit ni push sin que el usuario lo pida.** No commitear si la compilación falla.
- Datos solo en local (SharedPreferences / UserDefaults y ficheros internos). Sin red ni cuentas salvo que el SPEC lo diga.
- Mantener este `CLAUDE.md` al día: al cambiar de forma relevante la app, el flujo o las decisiones, actualizarlo en la misma tarea.

## Qué hace la app
Efecto de magia: la app imita el móvil del mago (escritorio, bloqueo, teclado de PIN) con **capturas** y **cajas invisibles** configurables.
El PIN es de longitud variable: se valida con la tecla OK (mínimo configurable, por defecto 4; máximo, por defecto 12). El primer PIN que teclea el espectador se guarda; los `attempts-1` primeros fallan y el último desbloquea (por defecto 2); una caja secreta del escritorio revela el PIN guardado (solo mientras se pulsa).
Alternativa al PIN: **patrón** (`AppSettings.unlockMode`; lógica en `Pattern.kt`, dibujo en `PatternDraw.kt`). Tiene su propia pantalla (`ScreenId.PATTERN`, con captura y fondo limpio propios): el espectador traza sobre una caja que tapa la rejilla de la captura y la app dibuja la suya con tamaño absoluto (la caja solo la centra), y el peek enseña el patrón pequeño con números y flechas. Se guarda como `n:p1,p2,…` en `EffectState.first`. Al abrir la app sale un modal para elegir PIN o patrón (`askAtStart`); cambiar de tipo reinicia el efecto. Vibración con `rememberVibrator()` (`expect/actual`): doble en botones ocultos, simple en fallo.
Al abrir: guía rápida (`Guide.kt`, ilustraciones con Canvas) si `guideAtStart`, luego el modal de tipo de desbloqueo. Ajustes → «Instrucciones» (`Instructions.kt`) describe todos los ajustes: **al añadir o cambiar un ajuste, actualizar ese texto y, si procede, la guía**.
Detalle funcional (R1–R17) en `docs/SPEC.md`. Paquete `com.lokit`. Estado y pendientes en `HANDOFF.md`.

## Arquitectura
Todo el código vive en `shared/src/commonMain/kotlin/com/lokit/ui/` (`Model.kt` y `SettingsStore.kt` con la lógica pura; `App.kt`, `Masks.kt`, `KeyFeedback.kt` y `SettingsScreen.kt` con la UI). Lo del sistema va con `expect/actual` en `Platform.kt` + `androidMain`/`iosMain`.

## Decisiones estables
- **Capturas a pantalla completa estiradas** (`FillBounds`); todas las cajas van normalizadas 0..1, así valen para cualquier resolución.
- **Ajustes**: `AppSettings.overrides` solo guarda lo que el usuario cambia (null = caja desactivada); lo demás sale del `default` de `Specs`. Cada valor tiene su clave en `SettingsStore`; la marca `"default"` = sin override.
- **Teclado**: una caja por tecla (rejilla 3×4 por defecto, editable). Los botones ocultos (Ajustes arriba-derecha, reset arriba-izquierda) son pulsación larga con duración configurable; «Bloquear» del escritorio también reinicia.
- **Máscaras de hora/fecha**: libres (se añaden/eliminan en el editor, `AppSettings.masks` por pantalla, persistidas como `masks_<PANTALLA>` = `id#zona#estilo;…`). El **tamaño del texto es absoluto** (fracción de la altura de pantalla), no depende de la caja; la caja solo alinea. Fondo: transparente o **imagen limpia** (`ScreenId.cleanFile`: captura sin hora/fecha/iconos que el usuario carga por pantalla); se recorta el mismo trozo normalizado. Se probó y descartó rellenar automáticamente (clonar/degradado): no encajaba.
- **Modo atenuado** (bloqueo): la captura y los fondos de máscara se desenfocan juntos (`Modifier.blur`, solo Android 12+ e iOS) y el texto real queda nítido encima.
- **Datos solo locales**; formatos de almacenamiento antiguos se siguen leyendo (`MaskStyle.decode` acepta 6, 7 y 9 campos).

## Convenciones
- Comentarios y textos en español. Cambios de ajustes: añadir campo en `AppSettings` + carga/guardado en `SettingsStore` + test en `SettingsStoreTest`.
- Lógica pura y testeable en `Model.kt`/`SettingsStore.kt`; la UI se verifica en el emulador con capturas.
- Al cambiar de forma relevante la app, actualizar `docs/SPEC.md`, este fichero y `HANDOFF.md`.

## Reglas de código para que compile en iOS
- En `commonMain`: nada de `java.*` ni `android.*`. Usar `kotlinx-datetime` (0.6.1), `kotlin.math.*` y, si hace falta aritmética
  grande, `bignum` (`com.ionspin.kotlin:bignum:0.3.10`). Sin `String.format`, `Locale`, `SimpleDateFormat`, `Thread`, `synchronized`.
- Sin recursos de Android (`R.string`) en común; textos en el código.
- Iconos dibujados con `Canvas`/`Path` de Compose (no `material-icons-extended`).
- Lógica de dominio pura y sin UI, con tests en `commonTest` (y reloj inyectable si depende del tiempo).
- Lo del sistema (sensores, vibración, galería, portapapeles…): `expect` en `commonMain/.../ui/Platform.kt` + `actual` en `androidMain`/`iosMain`.
- `Platform.ios.kt` lleva `@file:OptIn(ExperimentalForeignApi::class)` (cinterop de Kotlin/Native).
- El fallo de iOS solo se ve en Actions (~20 min): revisar con cuidado antes de lanzarlo.
- Con scripts: en esta máquina los heredocs de Bash con contenido complejo fallan; escribe los `.py` con la herramienta Write y ejecútalos.

## Compilar y probar (Windows)
- Versiones de Kotlin/Compose/AGP/Gradle y SDK: ver `docs/ARQUITECTURA.md` (JDK 17).
- `JAVA_HOME` apunta al JDK 17: `C:\Program Files\Microsoft\jdk-17.0.21.21-hotspot`.
- SDK de Android en `C:\Users\jmlop\AppData\Local\Android\Sdk`: crear `local.properties` con `sdk.dir=...` (ignorado por git).
- `./gradlew.bat :shared:testDebugUnitTest assembleDebug` → APK en `app/build/outputs/apk/debug/app-debug.apk`.
  Copiar el APK a `data/android_build/` si se quiere conservar.
- Emulador AVD `calcu` (Android 15), reutilizable. Poner la zona horaria `Europe/Madrid` (arranca en GMT). Gboard puede salir en negro (es del emulador).
- `gh` en Windows: `C:\Program Files\GitHub CLI\gh.exe` (hace falta `gh auth login` una vez).
- En Windows los destinos de iOS se ignoran (`kotlin.native.ignoreDisabledTargets=true`); Android compila y testea igual.

## Flujo de trabajo
1. Desarrollar y probar en Android/JVM en local (emulador).
2. Commit y push cuando el usuario lo pida.
3. **Preguntar antes de lanzar Actions** y acumular mejoras: `gh workflow run ios.yml --ref main` (opción `-f tests=true` solo si se pide).
4. Descargar el artefacto a `data/ios_build/`: `gh run download <id> -n <Nombre>-ios-sin-firmar -D data/ios_build`.
5. Instalar el `.ipa` con Sideloadly/AltStore (Apple ID gratis; caduca a los 7 días; activar Modo desarrollador en el iPhone).

## Lecciones de iOS ya aprendidas
- `gradlew` debe estar en git con permiso de ejecución (`git update-index --chmod=+x gradlew`).
- En iOS, `UIDevice.proximityState` se lee por sondeo; apagar y reactivar la monitorización deja el estado pegado. Mantenerla activa.
- Barra de estado e indicador de inicio se ocultan desde SwiftUI (`ContentView.swift`); el bloqueo automático con `idleTimerDisabled`.

## Instrucciones para el usuario (si la app las necesita)
Si la app lleva una página de «Instrucciones» de cara al usuario final, mantenerla al día en la misma tarea en que se cambie la función.
