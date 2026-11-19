# lokit — Especificación funcional

App de magia para Android e iOS (un solo código KMP). Efecto: **adivinar/transmitir el PIN del móvil del espectador**.

## El efecto (cara al público)
1. Se enseña al espectador el móvil con la pantalla de bloqueo y se dice que sería casualidad que su PIN desbloqueara el del mago.
2. El espectador prueba su PIN y **falla** (obviamente).
3. Se le da el móvil: «voy a transmitirte mentalmente mi PIN». Teclea un número y el móvil **se desbloquea**.
4. Se le dice que ahora se hace al revés: se coge el móvil del espectador y se desbloquea.

## El secreto
- La app imita el móvil del mago: escritorio (fondo, iconos), pantalla de bloqueo y teclado de PIN, todo con **capturas de pantalla**.
- El **primer** PIN tecleado (el del espectador) se guarda internamente y **siempre falla**.
- Los PIN siguientes, sean cuales sean, **desbloquean** y llevan al escritorio falso.
- En el escritorio hay una **caja invisible** que, al tocarla el mago, muestra el PIN que tecleó el espectador.

## Requisitos
1. R1 — Tres pantallas: bloqueo, teclado de PIN y escritorio; cada una con su captura (galería del móvil).
2. R2 — Flujo: bloqueo → (toque en caja «Abrir teclado») → teclado → PIN completo → fallo (1.º) o escritorio (siguientes).
3. R3 — PIN de longitud variable con ajuste «Máximo de dígitos» (por defecto 12, 1–20, siempre ≥ mínimo): a partir de ahí las teclas numéricas se ignoran. Se valida al pulsar la tecla **OK** (caja configurable, por defecto abajo a la izquierda del teclado). Ajuste «Mínimo de dígitos para OK» (por defecto 4, rango 1–12): con menos dígitos, OK no hace nada. Los puntos aparecen uno por dígito, centrados y sin huecos vacíos (se encogen si no caben).
3b. R3b — Tipo de desbloqueo (Ajustes): PIN numérico o **patrón**. Patrón: rejilla 3×3 a 5×5 (por defecto 3×3), mínimo de puntos (por defecto 4) que se valida al soltar el dedo; incluye los puntos intermedios del trazo como Android. El patrón tiene su **propia pantalla** (captura y fondo limpio independientes de las del teclado de PIN). Su caja «Rejilla del patrón» lleva máscara (por defecto, imagen de fondo limpia) que tapa la rejilla de la captura; la app dibuja la suya con **tamaño propio, independiente de la caja** (la caja solo la centra y marca la zona táctil): diámetro del punto, separación horizontal y vertical y grosor de línea (en % de la altura de pantalla), y color de puntos y de línea por separado. Cambiar de tipo de desbloqueo reinicia el efecto.
3c. R3c — Modal de arranque (ajuste «Preguntar al abrir la app», activo por defecto): al abrir la app se elige código o patrón, igual que en Ajustes.
3d. R3d — Vibración (Android e iOS): doble al activar los botones ocultos de Ajustes y de reset (pulsación larga); simple cuando un intento falla (PIN o patrón). En el escritorio, la caja «Patrón revelado» enseña el patrón guardado en pequeño: puntos numerados por orden, líneas y flechas de dirección. Mismos intentos y mismo reinicio que el PIN.
4. R4 — El primer PIN se guarda en local (persiste si se cierra la app) y no se sobrescribe después.
   - Intentos configurables (Ajustes, 1–10, por defecto 2): los `N-1` primeros fallan y el N.º desbloquea; con 1, el primer PIN ya desbloquea y se guarda.
5. R5 — Escritorio: caja invisible «Revelar PIN» muestra el PIN guardado **solo mientras se mantiene pulsada** (se oculta al soltar). La posición/tamaño del número, la opacidad del fondo y el color del número son configurables.
6. R6 — Cajas invisibles configurables (mover y redimensionar sobre la captura): abrir teclado, **una caja por tecla (0-9, borrar y OK)**, puntos del PIN, atrás/cancelar (opcional), revelar PIN, bloquear (opcional).
7. R7 — Botones ocultos por pulsación larga en cualquier pantalla: **Ajustes** (caja por defecto en la esquina superior derecha) y **Reset** (esquina superior izquierda; borra el PIN guardado y vuelve al bloqueo). Posición, tamaño y duración de la pulsación configurables. El botón «Bloquear» del escritorio también reinicia. (Hay botón visible de Ajustes si no hay captura.)
8. R8 — Ajustes: elegir/quitar captura por pantalla, editar cajas, nº de dígitos, duración de las pulsaciones ocultas y modo atenuado. El reset ya no está dentro de Ajustes.
9. R9 — Todo en local; sin red ni cuentas. Vertical. Modo inmersivo (sin barras del sistema).
10. R10 — Android e iOS con el mismo código.
11. R11 — Feedback visual configurable al pulsar cada tecla, para imitar el del móvil del mago.

12. R12 — Máscaras de hora y fecha reales, libres: en el editor de cajas de cada pantalla se pulsa «＋ Añadir máscara» (sin límite) y se elige Valor (hora o fecha), Formato (hora 24 h, 24 h sin cero, 12 h, 12 h con AM/PM, vertical en dos líneas; fecha corta, normal o larga), posición del texto en la caja, tipo de letra, grosor, tamaño y separación entre caracteres, color y fondo: transparente o imagen (el mismo trozo de la «imagen de fondo limpia» de esa pantalla, una captura sin hora, fecha ni iconos que se carga en Ajustes con «Fondo limpio»). El tamaño del texto es absoluto: cambiar la caja no lo altera. Se eliminan con «Eliminar máscara». Las cajas se seleccionan con un toque. Las máscaras fijas de versiones antiguas se migran solas.
13. R13 — Modo atenuado del bloqueo: arranca desenfocado y oscuro; un toque lo despierta; sin tocar durante X s vuelve a apagarse; con la pantalla despierta, otro toque abre el teclado. Oscuridad, desenfoque y segundos configurables.
14. R14 — Puntos del PIN configurables: tamaño, separación y máscara opcional que tapa los del pantallazo (uno por dígito, sin huecos).
15. R15 — Guía rápida: modal de 6 pasos con ilustraciones (qué es Lokit, capturas, cómo entrar en Ajustes, durante el efecto, descubrir el número, configuración). Sale al abrir la app mientras esté activo «Mostrar la guía rápida al abrir la app» (activo por defecto, así que sale la primera vez); en el último paso se puede desmarcar. Se puede abrir siempre desde Ajustes → «Guía rápida». Si además está activo el modal de elegir PIN/patrón, este sale al cerrar la guía.
16. R16 — Instrucciones: página en Ajustes → «Instrucciones» con el objetivo de la app, el uso paso a paso y la descripción de todos los ajustes. Hay que mantenerla al día al cambiar cualquier ajuste (`Instructions.kt`).
17. R17 — Icono definitivo (escudo con cerradura, fondo esmeralda) a partir de `data/icons/`: icono adaptable en Android (`mipmap-*`) e icono de 1024 px en iOS.

## Decisiones
- Aspecto por capturas (sirve para cualquier modelo/SO); los puntos del PIN se dibujan en una caja configurable, con sacudida en el fallo.
- Teclado con una caja por tecla (rejilla 3×4 por defecto, ajustable tecla a tecla; herramientas «Mismo tamaño para todas» y «Rejilla por defecto»).
- Resalte al pulsar (R11): estilos Ninguno / Círculo (iPhone) / Píldora / Cuadrado redondeado (Pixel) / Onda (Android clásico, Samsung), claro u oscuro, tinte (automático, verde Pixel, azul, rosa, gris) e intensidad; se mezcla con la captura (Multiplicar/Trama) para no tapar el número; se prueba en el editor. Limitación: el círculo original de la tecla asoma bajo el cuadrado.
- Tras desbloquear se muestra el escritorio falso; el reinicio es manual desde Ajustes.

- El desenfoque del modo atenuado solo se ve en Android 12+ y en iOS. El fondo «Imagen» necesita la imagen de fondo limpia de la pantalla, hecha con el mismo móvil y a la misma resolución que la captura.

## Preguntas abiertas
- ¿Hace falta vibración/sonido al fallar el PIN?
- ¿Revelar también con otro gesto (p. ej. botón de volumen en Android)?
