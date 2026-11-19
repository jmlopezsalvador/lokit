# HANDOFF — lokit (2026-11-19)

## Estado actual
- App completa en Android y iOS: bloqueo → teclado PIN **o patrón** → escritorio, con cajas invisibles, botones ocultos por pulsación larga, PIN/patrón revelado solo mientras se pulsa, máscaras libres de hora/fecha, modo atenuado, puntos del PIN configurables, resalte de teclas, intentos configurables, tecla OK con mínimo/máximo de dígitos, vibraciones, modal de arranque, guía rápida e instrucciones, icono definitivo.
- Último commit anterior en `main`: `901783a`. El trabajo posterior (todo lo de arriba) se commitea el 2026-11-19 en `main` («Patrón, intentos, OK, vibraciones, guía rápida, instrucciones e icono»); el push está pendiente de que el usuario lo pida. La **red social (Instagram) va en una rama aparte**: `feature/redes-sociales`.
- iOS: el último run compilado con éxito (43797977083) es anterior a todo lo nuevo; hay que relanzar Actions (preguntar antes) y revisar sobre todo `rememberVibrator` en `Platform.ios.kt`.
- Android: tests (`:shared:testDebugUnitTest`) y `assembleDebug` pasan. APK en `data/android_build/lokit-debug.apk`.

## Lo que falta / pendiente
- **Instagram (rama `feature/redes-sociales`)**: publicar la historia con el número descubierto. Decisiones tomadas: solo por API de Meta, botón «Conectar Instagram» con login (OAuth) y servidor mínimo gratuito propio (canje del código, renovación de tokens, alojamiento temporal de la imagen JPEG), solo en modo PIN, disparador = caja secreta en el escritorio con pulsación larga. Fase 1: modo desarrollo con probadores (acceso estándar); fase 2: verificación de empresa + revisión de Meta para abrirlo a cualquiera. Empezar por Ajustes + imagen, luego servidor/login/publicación. Cambia el principio «sin red ni cuentas»: actualizar CLAUDE.md y SPEC al empezar. Facebook (solo Páginas) y WhatsApp (sin vía oculta) quedan fuera por ahora.
- **Al abrir la app** las capturas tardan unos segundos en cargarse y mientras tanto sale «Sin captura» (se vio en el emulador); mejorar la carga inicial.
- Falta cargar captura y fondo limpio en «Pantalla de patrón» (antes usaba las del teclado).
- **Probar en iPhone real** (nunca verificado ahí): selector de fotos para capturas y para «Fondo limpio», desenfoque del modo atenuado, pulsaciones largas de los botones ocultos, resalte de teclas, máscaras con la imagen limpia, formatos de hora.
- No verificado ni siquiera en el emulador: eliminar una máscara, controles de tipo de letra / separación / fecha larga, arrastre de las cajas de botones ocultos, panel de puntos y de PIN revelado, pulsación corta en las esquinas ocultas.
- El usuario debe crear las **imágenes de fondo limpias** (capturas sin hora, fecha ni iconos, mismo móvil y resolución) y cargarlas por pantalla en Ajustes → «Fondo limpio». Sin ellas, las máscaras con fondo no tapan nada.

## Funcionalidades nuevas (TODO, en orden)
1. [x] **Intentos del espectador** (Ajustes → «Intentos del espectador», 1–10, por defecto 2): el primer PIN se guarda; los `N-1` primeros fallan y el N.º desbloquea. Hecho; falta probar en emulador.
1b. [x] **PIN sin límite + tecla OK**: se elimina «Dígitos del PIN»; nueva caja de tecla `keyOk` (por defecto abajo a la izquierda, `KeyHit.Ok`), ajustes `minDigits` (4) y `maxDigits` (12) y puntos sin huecos (uno por dígito, centrados). Hecho; falta probar en emulador. Nota: el ajuste «grosor del borde» de los puntos ya no se usa al dibujar (antes era para los anillos vacíos). El código ya aplica el máximo; falta verlo en el emulador.
2. [x] **Desbloqueo por patrón (versión 1)**: Ajustes → «Tipo de desbloqueo» (PIN / Patrón). Rejilla 3–5, mínimo de puntos, caja «Rejilla del patrón» con máscara + dibujo propio (el espectador traza sobre ella) y caja «Patrón revelado» en el escritorio (números, líneas y flechas). Probado en el emulador (trazo, desbloqueo y peek). Falta: probar con una captura real de pantalla de patrón y su imagen de fondo limpia, y en iPhone; posibles mejoras (animación de fallo, trazo rojo, variantes del peek).
3. [x] **Vibraciones, modal de arranque y patrón independiente**: doble vibración al abrir Ajustes y en el reset oculto, simple en fallo; modal «¿Cómo se desbloquea?» al abrir (ajuste «Preguntar al abrir la app»); nueva pantalla `ScreenId.PATTERN` con captura y fondo limpio propios; rejilla del patrón con tamaño propio (punto, separación horizontal/vertical, grosor, colores de punto y línea por separado; la caja ya no la escala). Probado en el emulador: modal, pantalla de patrón y desbloqueo. **Sin verificar:** que se sienta la vibración (el emulador no la muestra) y todo el código de iOS (`UIImpactFeedbackGenerator` en `Platform.ios.kt`; revisar al lanzar Actions). Hay que cargar de nuevo captura y fondo limpio en «Pantalla de patrón» (antes usaban las del teclado).
4. [ ] (por definir)

## Problemas abiertos / limitaciones conocidas
- Desenfoque del modo atenuado: solo Android 12+ e iOS.
- En el resalte cuadrado estilo Pixel asoma el círculo original de la tecla bajo el cuadrado.
- Si la imagen limpia tiene distinta resolución o proporción que la captura, el trozo no cuadra (se recorta por coordenadas normalizadas).
- El emulador se cuelga si se compila con Gradle a la vez (ANR); usar `-no-window -gpu host` y no solapar tareas pesadas.
- En esta máquina los heredocs de Bash con contenido complejo (comillas triples) fallan: escribir los scripts `.py` con la herramienta Write.

## Ficheros clave
- `CLAUDE.md` — reglas del proyecto, arquitectura, decisiones, cómo compilar/probar.
- `docs/SPEC.md` — requisitos funcionales (R1–R14) y decisiones; `docs/ARQUITECTURA.md` — base técnica.
- `shared/src/commonMain/kotlin/com/lokit/ui/`: `Model.kt`, `SettingsStore.kt`, `App.kt`, `Masks.kt`, `KeyFeedback.kt`, `SettingsScreen.kt`.
- `shared/src/{androidMain,iosMain}/.../Platform.*.kt` — `expect/actual` (selector de imágenes, almacenes, barras del sistema).
- `shared/src/commonTest/kotlin/com/lokit/SettingsStoreTest.kt` — tests.
- `iosApp/project.yml` y `.github/workflows/ios.yml` — compilación de iOS.
- `data/fondos/` (no versionado) — capturas del emulador para pruebas; `data/ios_build/`, `data/android_build/` — binarios.

## Cómo retomar
1. `./gradlew.bat :shared:testDebugUnitTest assembleDebug` (JDK 17, ver CLAUDE.md).
2. Emulador AVD `calcu`: instalar el APK; copiar capturas con `adb push` a `/data/local/tmp` y `run-as com.lokit sh -c 'cat … > files/capturas/<lock|pad|home>[_clean].img'`; prefs en `shared_prefs/lokit.xml` (`MSYS_NO_PATHCONV=1` en Git Bash).
3. Reglas del usuario: sin commit/push salvo petición; preguntar antes de lanzar el workflow de iOS (sin tests por defecto); ignorar `ignore/`.
