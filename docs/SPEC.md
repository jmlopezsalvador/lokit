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
3. R3 — PIN de longitud configurable (por defecto 6, rango 3–10). Valida solo al completar los dígitos.
4. R4 — El primer PIN se guarda en local (persiste si se cierra la app) y no se sobrescribe después.
5. R5 — Escritorio: caja invisible «Revelar PIN» muestra el PIN guardado **solo mientras se mantiene pulsada** (se oculta al soltar). La posición/tamaño del número, la opacidad del fondo y el color del número son configurables.
6. R6 — Cajas invisibles configurables (mover y redimensionar sobre la captura): abrir teclado, **una caja por tecla (0-9 y borrar)**, puntos del PIN, atrás/cancelar (opcional), revelar PIN, bloquear (opcional).
7. R7 — Botones ocultos por pulsación larga en cualquier pantalla: **Ajustes** (caja por defecto en la esquina superior derecha) y **Reset** (esquina superior izquierda; borra el PIN guardado y vuelve al bloqueo). Posición, tamaño y duración de la pulsación configurables. El botón «Bloquear» del escritorio también reinicia. (Hay botón visible de Ajustes si no hay captura.)
8. R8 — Ajustes: elegir/quitar captura por pantalla, editar cajas, nº de dígitos, duración de las pulsaciones ocultas y modo atenuado. El reset ya no está dentro de Ajustes.
9. R9 — Todo en local; sin red ni cuentas. Vertical. Modo inmersivo (sin barras del sistema).
10. R10 — Android e iOS con el mismo código.
11. R11 — Feedback visual configurable al pulsar cada tecla, para imitar el del móvil del mago.

12. R12 — Máscaras de hora y fecha reales (hasta 3 en cada una de las tres pantallas, también para sustituir la hora de la barra superior): tapan la hora/fecha fija de la captura y dibujan la actual (24 h, en español). Tipo (hora 24 h, 24 h sin cero, 12 h, 12 h con AM/PM, hora en dos líneas, fecha), alineación, grosor (fina/normal/negrita), color y tamaño del texto; fondo automático (interpolado de los bordes de la captura), color sólido o sin fondo.
13. R13 — Modo atenuado del bloqueo: arranca desenfocado y oscuro; un toque lo despierta; sin tocar durante X s vuelve a apagarse; con la pantalla despierta, otro toque abre el teclado. Oscuridad, desenfoque y segundos configurables.
14. R14 — Puntos del PIN configurables: tamaño, separación, grosor del borde y máscara opcional que tapa los del pantallazo.

## Decisiones
- Aspecto por capturas (sirve para cualquier modelo/SO); los puntos del PIN se dibujan en una caja configurable, con sacudida en el fallo.
- Teclado con una caja por tecla (rejilla 3×4 por defecto, ajustable tecla a tecla; herramientas «Mismo tamaño para todas» y «Rejilla por defecto»).
- Resalte al pulsar (R11): estilos Ninguno / Círculo (iPhone) / Píldora / Cuadrado redondeado (Pixel) / Onda (Android clásico, Samsung), claro u oscuro, tinte (automático, verde Pixel, azul, rosa, gris) e intensidad; se mezcla con la captura (Multiplicar/Trama) para no tapar el número; se prueba en el editor. Limitación: el círculo original de la tecla asoma bajo el cuadrado.
- Tras desbloquear se muestra el escritorio falso; el reinicio es manual desde Ajustes.

- El desenfoque del modo atenuado solo se ve en Android 12+ y en iOS; el relleno automático de las máscaras funciona mejor con cajas ajustadas al texto.

## Preguntas abiertas
- ¿Hace falta vibración/sonido al fallar el PIN?
- ¿Revelar también con otro gesto (p. ej. botón de volumen en Android)?
