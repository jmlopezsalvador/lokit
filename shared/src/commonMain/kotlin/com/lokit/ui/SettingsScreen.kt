package com.lokit.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt

private fun pct(v: Float) = "${(v * 100).roundToInt()} %"
private fun pct1(v: Float) = "${(v * 1000).roundToInt() / 10.0} %"
private fun secs(ms: Int) = "${ms / 1000}.${(ms % 1000) / 100} s"

/** Fila «etiqueta  valor  [−] [+]». */
@Composable
private fun Stepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text("$label: $value", fontSize = 14.sp, modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onMinus) { Text("−") }
            OutlinedButton(onClick = onPlus) { Text("+") }
        }
    }
}

/** Fila «etiqueta  deslizador». */
@Composable
private fun SliderRow(
    label: String, value: Float, range: ClosedFloatingPointRange<Float>, shown: String,
    onChange: (Float) -> Unit, textColor: Color = Color.Unspecified,
) {
    Column {
        Text("$label: $shown", fontSize = 12.sp, color = textColor)
        Slider(value = value.coerceIn(range.start, range.endInclusive), onValueChange = onChange, valueRange = range)
    }
}

/** Fila desplazable de chips de color. */
@Composable
private fun ColorChips(options: List<Pair<String, Long>>, selected: Long, onPick: (Long) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for ((name, argb) in options) {
            FilterChip(selected = selected == argb, onClick = { onPick(argb) }, label = { Text(name) })
        }
    }
}

/** Ajustes: capturas, botones ocultos, modo atenuado y editor de cajas. */
@Composable
fun SettingsScreen(
    settings: AppSettings,
    effect: EffectState,
    images: Map<ScreenId, ImageBitmap?>,
    cleans: Map<ScreenId, ImageBitmap?>,
    onSettings: (AppSettings) -> Unit,
    onPickImage: (ScreenId, ByteArray) -> Unit,
    onRemoveImage: (ScreenId) -> Unit,
    onPickClean: (ScreenId, ByteArray) -> Unit,
    onRemoveClean: (ScreenId) -> Unit,
    onClose: () -> Unit,
) {
    var target by remember { mutableStateOf<ScreenId?>(null) }
    var targetClean by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ScreenId?>(null) }
    var editingHidden by remember { mutableStateOf(false) }
    val pick = rememberImagePicker { bytes -> target?.let { if (targetClean) onPickClean(it, bytes) else onPickImage(it, bytes) } }

    var showInstructions by remember { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(false) }
    if (showInstructions) {
        InstructionsScreen { showInstructions = false }
        return
    }

    val editId = editing
    if (editId != null || editingHidden) {
        BoxEditor(
            title = if (editId != null) editId.title else "Botones ocultos",
            image = images[editId ?: ScreenId.LOCK],
            clean = editId?.let { cleans[it] },
            specs = if (editId != null) Specs.forScreen(editId) else Specs.hidden,
            screenId = editId,
            settings = settings,
            onDone = { newSettings ->
                if (newSettings != null) onSettings(newSettings)
                editing = null
                editingHidden = false
            },
        )
        return
    }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding()
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Ajustes de lokit", fontSize = 22.sp, color = MaterialTheme.colorScheme.onBackground)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Ayuda", fontSize = 16.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { showInstructions = true }) { Text("Instrucciones") }
                    OutlinedButton(onClick = { showGuide = true }) { Text("Guía rápida") }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Switch(checked = settings.guideAtStart, onCheckedChange = { onSettings(settings.copy(guideAtStart = it)) })
                    Text("Mostrar la guía rápida al abrir la app", fontSize = 14.sp)
                }
            }
        }
        if (showGuide) {
            GuideDialog(showAtStart = settings.guideAtStart) { show ->
                if (show != settings.guideAtStart) onSettings(settings.copy(guideAtStart = show))
                showGuide = false
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Efecto", fontSize = 16.sp)
                val what = if (settings.unlockMode == UnlockMode.PATTERN) "patrón" else "PIN"
                val saved = Pattern.decode(effect.first)?.let { "patrón de ${it.second.size} puntos" } ?: effect.first
                val status = when (effect.stage) {
                    0 -> "Esperando el primer $what del espectador (se guardará)."
                    1 -> "Guardado: $saved. Fallos hasta ahora: ${effect.count} de ${settings.attempts - 1}; después desbloquea."
                    else -> "Desbloqueado. Guardado: $saved."
                }
                Text(status, fontSize = 13.sp)
                Text("Para reiniciar usa el botón oculto de reset (por defecto, esquina superior izquierda) o el de bloquear del escritorio.", fontSize = 12.sp, color = Color(0xFF9AA4B2))
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Switch(checked = settings.askAtStart, onCheckedChange = { onSettings(settings.copy(askAtStart = it)) })
                    Text("Preguntar al abrir la app (código o patrón)", fontSize = 14.sp)
                }
                Text("Tipo de desbloqueo", fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (m in UnlockMode.entries) {
                        FilterChip(
                            selected = settings.unlockMode == m,
                            onClick = { onSettings(settings.copy(unlockMode = m)) },
                            label = { Text(m.label) },
                        )
                    }
                }
                if (settings.unlockMode == UnlockMode.PATTERN) {
                    Stepper(
                        "Rejilla", "${settings.patternGrid} × ${settings.patternGrid}",
                        { if (settings.patternGrid > 3) onSettings(settings.copy(patternGrid = settings.patternGrid - 1, patternMin = settings.patternMin.coerceAtMost((settings.patternGrid - 1) * (settings.patternGrid - 1)))) },
                        { if (settings.patternGrid < 5) onSettings(settings.copy(patternGrid = settings.patternGrid + 1)) },
                    )
                    Stepper(
                        "Mínimo de puntos", "${settings.patternMin}",
                        { if (settings.patternMin > 2) onSettings(settings.copy(patternMin = settings.patternMin - 1)) },
                        { if (settings.patternMin < settings.patternGrid * settings.patternGrid) onSettings(settings.copy(patternMin = settings.patternMin + 1)) },
                    )
                    Text(
                        "El patrón se valida al soltar el dedo, si tiene al menos ese número de puntos. " +
                            "Colócalo y dale aspecto en «Pantalla de patrón» → «Cajas» → «Rejilla del patrón».",
                        fontSize = 12.sp, color = Color(0xFF9AA4B2),
                    )
                } else {
                Stepper(
                    "Mínimo de dígitos para OK", "${settings.minDigits}",
                    { if (settings.minDigits > 1) onSettings(settings.copy(minDigits = settings.minDigits - 1)) },
                    // Subir el mínimo arrastra el máximo para que nunca quede por debajo.
                    { if (settings.minDigits < 20) onSettings(settings.copy(minDigits = settings.minDigits + 1, maxDigits = maxOf(settings.maxDigits, settings.minDigits + 1))) },
                )
                Stepper(
                    "Máximo de dígitos", "${settings.maxDigits}",
                    { if (settings.maxDigits > settings.minDigits) onSettings(settings.copy(maxDigits = settings.maxDigits - 1)) },
                    { if (settings.maxDigits < 20) onSettings(settings.copy(maxDigits = settings.maxDigits + 1)) },
                )
                Text("OK no hace nada hasta que se teclean al menos el mínimo; a partir del máximo, las teclas numéricas se ignoran.", fontSize = 12.sp, color = Color(0xFF9AA4B2))
                }
                Stepper(
                    "Intentos del espectador", "${settings.attempts}",
                    { if (settings.attempts > 1) onSettings(settings.copy(attempts = settings.attempts - 1)) },
                    { if (settings.attempts < 10) onSettings(settings.copy(attempts = settings.attempts + 1)) },
                )
                Text("Los intentos anteriores al último fallan; el último desbloquea. Con 1, el primer intento ya desbloquea (y se guarda).", fontSize = 12.sp, color = Color(0xFF9AA4B2))
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Botones ocultos", fontSize = 16.sp)
                Text(
                    "Se activan con pulsación larga en cualquier pantalla. Ajustes: esquina superior derecha. Reset: esquina superior izquierda.",
                    fontSize = 12.sp, color = Color(0xFF9AA4B2),
                )
                Stepper(
                    "Pulsación para Ajustes", secs(settings.settingsHoldMs),
                    { onSettings(settings.copy(settingsHoldMs = (settings.settingsHoldMs - 100).coerceAtLeast(200))) },
                    { onSettings(settings.copy(settingsHoldMs = (settings.settingsHoldMs + 100).coerceAtMost(5000))) },
                )
                Stepper(
                    "Pulsación para reset", secs(settings.resetHoldMs),
                    { onSettings(settings.copy(resetHoldMs = (settings.resetHoldMs - 100).coerceAtLeast(200))) },
                    { onSettings(settings.copy(resetHoldMs = (settings.resetHoldMs + 100).coerceAtMost(5000))) },
                )
                Button(onClick = { editingHidden = true }) { Text("Posición y tamaño de las cajas") }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Switch(checked = settings.dimEnabled, onCheckedChange = { onSettings(settings.copy(dimEnabled = it)) })
                    Text("Modo atenuado en el bloqueo", fontSize = 16.sp)
                }
                Text(
                    "El bloqueo arranca desenfocado y oscuro. Un toque lo despierta; si pasan unos segundos sin tocar vuelve a apagarse; con la pantalla despierta, otro toque abre el teclado.",
                    fontSize = 12.sp, color = Color(0xFF9AA4B2),
                )
                if (settings.dimEnabled) {
                    Stepper(
                        "Se apaga tras", "${settings.dimSeconds} s",
                        { onSettings(settings.copy(dimSeconds = (settings.dimSeconds - 1).coerceAtLeast(1))) },
                        { onSettings(settings.copy(dimSeconds = (settings.dimSeconds + 1).coerceAtMost(120))) },
                    )
                    SliderRow("Oscuridad", settings.dimLevel, 0f..0.95f, pct(settings.dimLevel), { onSettings(settings.copy(dimLevel = it)) })
                    SliderRow(
                        "Desenfoque", settings.dimBlur.toFloat(), 0f..40f, "${settings.dimBlur}",
                        { onSettings(settings.copy(dimBlur = it.roundToInt())) },
                    )
                    Text("El desenfoque solo se ve en Android 12 o posterior y en iOS.", fontSize = 11.sp, color = Color(0xFF9AA4B2))
                }
            }
        }

        for (id in ScreenId.entries) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(id.title, fontSize = 16.sp)
                    Text(if (images[id] != null) "Captura cargada" else "Sin captura", fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { target = id; targetClean = false; pick() }) { Text("Captura") }
                        OutlinedButton(onClick = { onRemoveImage(id) }, enabled = images[id] != null) { Text("Quitar") }
                        OutlinedButton(onClick = { editing = id }) { Text("Cajas") }
                    }
                    Text(
                        if (cleans[id] != null) "Imagen de fondo limpia cargada" else "Sin imagen de fondo limpia (las máscaras con fondo no tapan nada)",
                        fontSize = 12.sp, color = Color(0xFF9AA4B2),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { target = id; targetClean = true; pick() }) { Text("Fondo limpio") }
                        OutlinedButton(onClick = { onRemoveClean(id) }, enabled = cleans[id] != null) { Text("Quitar fondo") }
                    }
                }
            }
        }

        Text(
            "Las capturas se ven estiradas a pantalla completa; haz los pantallazos con el mismo móvil que usarás. " +
                "El resalte de las teclas, los puntos del PIN y las máscaras de hora y fecha se configuran dentro de «Cajas».",
            fontSize = 12.sp, color = Color(0xFF9AA4B2),
        )
        Button(onClick = onClose, Modifier.fillMaxWidth()) { Text("Volver a la app") }
    }
}

private val boxColors = listOf(Color(0xFF4FC3F7), Color(0xFFFFB74D), Color(0xFF81C784), Color(0xFFE57373))

/** Tintes del resalte: automático (0) y algunos tonales habituales en teclados. */
private val tints = listOf(
    "Automático" to 0L,
    "Verde Pixel" to 0xFFC5E48AL,
    "Azul" to 0xFFA8C7FAL,
    "Rosa" to 0xFFF2B8D0L,
    "Gris" to 0xFF9E9E9EL,
)

private val aligns = listOf("Izquierda", "Centro", "Derecha")
private val weights = listOf("Fina", "Normal", "Negrita")
private val fontNames = listOf("Sistema", "Serif", "Monoespaciada", "Cursiva")
private val maskColors = listOf(Color(0xFFBA68C8), Color(0xFFFF8A65), Color(0xFF4DB6AC), Color(0xFFFFD54F))

private fun sizePct(v: Float) = "${(v * 1000).roundToInt() / 10f} %"

/** Una caja editable: caja fija (spec) o máscara añadida por el usuario. */
private class Ent(val key: String, val zone: Zone, val tag: String, val color: Color)

/**
 * Editor de cajas invisibles sobre la captura: tocar una caja para seleccionarla, arrastrar para moverla y la esquina
 * inferior derecha para redimensionarla. Las máscaras de hora/fecha se añaden y se quitan libremente.
 */
@Composable
private fun BoxEditor(
    title: String,
    image: ImageBitmap?,
    clean: ImageBitmap?,
    specs: List<BoxSpec>,
    screenId: ScreenId?,
    settings: AppSettings,
    onDone: (AppSettings?) -> Unit,
) {
    var current by remember { mutableStateOf(settings) }
    var selected by remember { mutableStateOf(specs.first().key) }
    var panelTop by remember { mutableStateOf(false) }
    var panelHidden by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    val feedback = remember { KeyFeedback() }
    val measurer = rememberTextMeasurer()
    val now = rememberNow()
    val masks = if (screenId != null) current.masksOf(screenId) else emptyList()
    val selMask = masks.firstOrNull { "m${it.id}" == selected }
    val spec = specs.firstOrNull { it.key == selected } ?: if (selMask == null) specs.first() else null
    val isPattern = screenId == ScreenId.PATTERN
    val isPad = screenId == ScreenId.PAD // teclado de PIN (el patrón tiene su propio panel)
    val white = Color.White

    fun ents(): List<Ent> {
        val out = ArrayList<Ent>()
        specs.forEachIndexed { i, sp -> current.box(sp)?.let { out += Ent(sp.key, it, sp.tag, boxColors[i % boxColors.size]) } }
        if (screenId != null) {
            current.masksOf(screenId).forEachIndexed { i, m ->
                out += Ent("m${m.id}", m.zone, if (m.style.kind.date) "Fecha" else "Hora", maskColors[i % maskColors.size])
            }
        }
        return out
    }

    fun setZone(key: String, z: Zone) {
        current = if (key.startsWith("m") && screenId != null) {
            val m = current.masksOf(screenId).first { "m${it.id}" == key }
            current.updateMask(screenId, m.copy(zone = z))
        } else current.withBox(key, z)
    }

    fun updMask(f: (MaskStyle) -> MaskStyle) {
        val m = selMask ?: return
        current = current.updateMask(screenId!!, m.copy(style = f(m.style)))
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF222222))) {
        if (image != null) Image(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)

        // Vista previa real de las máscaras de hora/fecha, los puntos del PIN y el número revelado.
        if (!testing) {
            for (m in masks) key(m.id) { MaskLayer(m.zone, m.style, clean, maskLines(m.style.kind, now)) }
            if (isPad) {
                current.box(Specs.padDots)?.let { z ->
                    MaskLayer(z, current.mask(Specs.padDots), clean, null)
                    Canvas(Modifier.fillMaxSize()) { drawPinDots(z, current, 3, 0f) }
                }
            }
            if (isPattern) {
                current.box(Specs.padPattern)?.let { z ->
                    MaskLayer(z, current.mask(Specs.padPattern), clean, null)
                    val sample = listOf(0, 3, 4, 5, 8).filter { it < current.patternGrid * current.patternGrid }
                    Canvas(Modifier.fillMaxSize()) { drawPatternPad(z, current, sample, null) }
                }
            }
            if (screenId == ScreenId.HOME && selected == Specs.homeRevealView.key) {
                val view = current.box(Specs.homeRevealView) ?: Specs.homeRevealView.default!!
                Canvas(Modifier.fillMaxSize()) { drawReveal(measurer, view, current, "427193") }
            }
            if (screenId == ScreenId.HOME && selected == Specs.homePattern.key) {
                val view = current.box(Specs.homePattern) ?: Specs.homePattern.default!!
                val n = current.patternGrid
                val sample = Pattern.encode(n, listOf(0, n, n + 1, 1, 2, n + 2).filter { it < n * n })
                Canvas(Modifier.fillMaxSize()) { drawPatternReveal(measurer, view, current, sample) }
            }
        }

        Canvas(
            Modifier.fillMaxSize()
                .pointerInput(testing) {
                    // Un toque selecciona la caja más pequeña que haya bajo el dedo (así se alcanzan las que están encima de otras).
                    if (!testing) {
                        detectTapGestures(onTap = { o ->
                            val nx = o.x / size.width
                            val ny = o.y / size.height
                            val hit = ents().filter { it.zone.contains(nx, ny) }.minByOrNull { it.zone.w * it.zone.h }
                            if (hit != null) selected = hit.key
                        })
                    }
                }
                .pointerInput(testing) {
                    if (testing) {
                        // Modo prueba: tocar una tecla enseña el resalte tal y como se verá.
                        detectTapGestures(onPress = { o ->
                            val key = hitKey(current, o.x / size.width, o.y / size.height)
                            if (key != null) feedback.run(current, key.second) { tryAwaitRelease() }
                        })
                    } else {
                        var mode = 0 // 0 nada, 1 mover, 2 redimensionar
                        detectDragGestures(
                            onDragStart = { o ->
                                val all = ents()
                                val sel = all.firstOrNull { it.key == selected }?.zone
                                val nx = o.x / size.width
                                val ny = o.y / size.height
                                mode = when {
                                    sel != null && abs(o.x - (sel.x + sel.w) * size.width) < 72f &&
                                        abs(o.y - (sel.y + sel.h) * size.height) < 72f -> 2
                                    sel != null && sel.contains(nx, ny) -> 1
                                    else -> {
                                        val hit = all.filter { it.zone.contains(nx, ny) }.minByOrNull { it.zone.w * it.zone.h }
                                        if (hit != null) { selected = hit.key; 1 } else 0
                                    }
                                }
                            },
                            onDrag = { change, delta ->
                                change.consume()
                                val b = ents().firstOrNull { it.key == selected }?.zone
                                if (b != null && mode != 0) {
                                    val dx = delta.x / size.width
                                    val dy = delta.y / size.height
                                    val nb = if (mode == 1) {
                                        b.copy(x = (b.x + dx).coerceIn(0f, 1f - b.w), y = (b.y + dy).coerceIn(0f, 1f - b.h))
                                    } else {
                                        b.copy(w = (b.w + dx).coerceIn(0.02f, 1f - b.x), h = (b.h + dy).coerceIn(0.02f, 1f - b.y))
                                    }
                                    setZone(selected, nb)
                                }
                            },
                        )
                    }
                },
        ) {
            for (e in ents()) {
                val b = e.zone
                val isSel = e.key == selected && !testing
                val topLeft = Offset(b.x * size.width, b.y * size.height)
                val sz = Size(b.w * size.width, b.h * size.height)
                if (!testing) {
                    drawRect(e.color.copy(alpha = if (isSel) 0.30f else 0.12f), topLeft, sz)
                    drawRect(e.color, topLeft, sz, style = Stroke(width = if (isSel) 5f else 2f))
                    if (e.tag.isNotEmpty()) {
                        val layout = measurer.measure(e.tag, TextStyle(color = Color.White, fontSize = 12.sp))
                        drawText(
                            layout,
                            topLeft = Offset(
                                topLeft.x + (sz.width - layout.size.width) / 2f,
                                topLeft.y + (sz.height - layout.size.height) / 2f,
                            ),
                        )
                    }
                    if (isSel) drawCircle(e.color, 22f, Offset(topLeft.x + sz.width, topLeft.y + sz.height))
                }
            }
            if (testing) feedback.draw(this, current)
        }

        if (panelHidden) {
            OutlinedButton(
                onClick = { panelHidden = false },
                modifier = Modifier.align(if (panelTop) Alignment.TopEnd else Alignment.BottomEnd).safeDrawingPadding().padding(12.dp),
            ) { Text("Mostrar panel") }
        } else {
            Column(
                Modifier.align(if (panelTop) Alignment.TopCenter else Alignment.BottomCenter).fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.88f)).safeDrawingPadding()
                    .heightIn(max = 380.dp).verticalScroll(rememberScrollState()).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Cajas de «$title» (toca una caja para seleccionarla)", fontSize = 14.sp, color = white)
                if (image == null) Text("Sin captura: las cajas se colocan sobre fondo gris.", fontSize = 11.sp, color = Color(0xFFFFB74D))

                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (sp in specs) {
                        FilterChip(selected = sp.key == selected, onClick = { selected = sp.key; testing = false }, label = { Text(sp.label) })
                    }
                    for (m in masks) {
                        FilterChip(
                            selected = "m${m.id}" == selected,
                            onClick = { selected = "m${m.id}"; testing = false },
                            label = { Text("▣ ${m.style.kind.label.substringBefore(" (")}") },
                        )
                    }
                    if (screenId != null) {
                        OutlinedButton(onClick = {
                            val (ns, item) = current.addMask(
                                screenId, Zone(0.25f, 0.08f, 0.5f, 0.06f),
                                MaskStyle(kind = MaskKind.TIME_NOZERO, weight = 1, size = MaskKind.TIME_NOZERO.defaultSize),
                            )
                            current = ns
                            selected = "m${item.id}"
                            testing = false
                        }) { Text("＋ Añadir máscara") }
                    }
                }

                if (selMask != null) {
                    val st = selMask.style
                    Text(
                        "Máscara de hora o fecha: tapa lo que trae la captura y dibuja el valor real. La caja marca dónde se coloca; " +
                            "el tamaño del texto se cambia aquí abajo, no con la caja.",
                        fontSize = 11.sp, color = Color(0xFFB0B8C4),
                    )
                    Text("Valor", fontSize = 12.sp, color = white)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for ((name, isDate) in listOf("Hora" to false, "Fecha" to true)) {
                            FilterChip(
                                selected = st.kind.date == isDate,
                                onClick = {
                                    if (st.kind.date != isDate) updMask {
                                        val nk = if (isDate) MaskKind.DATE else MaskKind.TIME_NOZERO
                                        it.copy(kind = nk, size = if (it.size == it.kind.defaultSize) nk.defaultSize else it.size)
                                    }
                                },
                                label = { Text(name) },
                            )
                        }
                    }
                    Text("Formato", fontSize = 12.sp, color = white)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (k in MaskKind.entries.filter { it.date == st.kind.date }) {
                            FilterChip(
                                selected = st.kind == k,
                                onClick = { updMask { it.copy(kind = k, size = if (it.size == it.kind.defaultSize) k.defaultSize else it.size) } },
                                label = { Text(k.label) },
                            )
                        }
                    }
                    Text("Posición del texto en la caja", fontSize = 12.sp, color = white)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        aligns.forEachIndexed { i, name ->
                            FilterChip(selected = st.align == i, onClick = { updMask { it.copy(align = i) } }, label = { Text(name) })
                        }
                    }
                    Text("Tipo de letra y grosor", fontSize = 12.sp, color = white)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        fontNames.forEachIndexed { i, name ->
                            FilterChip(selected = st.font == i, onClick = { updMask { it.copy(font = i) } }, label = { Text(name) })
                        }
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        weights.forEachIndexed { i, name ->
                            FilterChip(selected = st.weight == i, onClick = { updMask { it.copy(weight = i) } }, label = { Text(name) })
                        }
                    }
                    SliderRow("Tamaño del texto", st.size, 0.008f..0.16f, sizePct(st.size), { v -> updMask { it.copy(size = v) } }, white)
                    SliderRow(
                        "Separación entre caracteres", st.spacing, -0.1f..0.6f, "${(st.spacing * 100).roundToInt()} %",
                        { v -> updMask { it.copy(spacing = v) } }, white,
                    )
                    Text("Color del texto", fontSize = 12.sp, color = white)
                    ColorChips(Palette.text, st.textColor) { c -> updMask { it.copy(textColor = c) } }
                    Text("Fondo de la máscara", fontSize = 12.sp, color = white)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (m in BgMode.entries) {
                            FilterChip(selected = st.bgMode == m, onClick = { updMask { it.copy(bgMode = m) } }, label = { Text(m.label) })
                        }
                    }
                    if (st.bgMode == BgMode.IMAGE) {
                        Text(
                            if (clean != null) "Tapa con el mismo trozo de la imagen de fondo limpia de esta pantalla (se carga en Ajustes con «Fondo limpio»)."
                            else "Falta la imagen de fondo limpia de esta pantalla: cárgala en Ajustes con «Fondo limpio».",
                            fontSize = 11.sp, color = if (clean != null) Color(0xFFB0B8C4) else Color(0xFFFFB74D),
                        )
                    }
                }

                if (spec != null) {
                    Text(spec.help, fontSize = 11.sp, color = Color(0xFFB0B8C4))
                    if (spec.optional) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Switch(
                                checked = current.box(spec) != null,
                                onCheckedChange = { on ->
                                    current = current.withBox(spec.key, if (on) (spec.default ?: Zone(0.3f, 0.4f, 0.4f, 0.1f)) else null)
                                },
                            )
                            Text("Activa", fontSize = 13.sp, color = white)
                        }
                    }

                    // Rejilla del patrón: tamaño absoluto (no depende de la caja), separaciones y colores independientes.
                    if (spec.key == Specs.padPattern.key) {
                        Text("Tamaños en % de la altura de la pantalla: no cambian al mover o redimensionar la caja.", fontSize = 11.sp, color = Color(0xFFB0B8C4))
                        SliderRow("Tamaño del punto", current.patternDot, 0.008f..0.1f, pct1(current.patternDot), { current = current.copy(patternDot = it) }, white)
                        SliderRow("Separación horizontal", current.patternGapX, 0f..0.3f, pct1(current.patternGapX), { current = current.copy(patternGapX = it) }, white)
                        SliderRow("Separación vertical", current.patternGapY, 0f..0.3f, pct1(current.patternGapY), { current = current.copy(patternGapY = it) }, white)
                        SliderRow("Grosor de la línea", current.patternLine, 0.001f..0.03f, pct1(current.patternLine), { current = current.copy(patternLine = it) }, white)
                        Text("Color de los puntos", fontSize = 12.sp, color = white)
                        ColorChips(Palette.text, current.patternDotColor) { current = current.copy(patternDotColor = it) }
                        Text("Color de la línea", fontSize = 12.sp, color = white)
                        ColorChips(Palette.text, current.patternLineColor) { current = current.copy(patternLineColor = it) }
                    }
                    // Puntos del PIN: tamaño, separación, borde y máscara.
                    if (spec.maskDefault != null && current.box(spec) != null) {
                        val st = current.mask(spec)
                        if (spec.key == Specs.padDots.key) {
                            SliderRow("Tamaño de los círculos", current.dotSize, 0.2f..1.5f, pct(current.dotSize), { current = current.copy(dotSize = it) }, white)
                            SliderRow("Separación", current.dotGap, 0f..3f, pct(current.dotGap), { current = current.copy(dotGap = it) }, white)
                        }
                        Text("Máscara: tapa lo que traiga la captura en esa zona (puntos, recuadro o rejilla). Con «Imagen de fondo limpia» hace falta cargarla para esta pantalla.", fontSize = 11.sp, color = Color(0xFFB0B8C4))
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (m in BgMode.entries) {
                                FilterChip(
                                    selected = st.bgMode == m,
                                    onClick = { current = current.withMask(spec.key, st.copy(bgMode = m)) },
                                    label = { Text(if (m == BgMode.NONE) "Sin máscara" else m.label) },
                                )
                            }
                        }
                    }

                    // PIN revelado: opacidad del fondo y color del número.
                    if (spec.key == Specs.homeRevealView.key || spec.key == Specs.homePattern.key) {
                        SliderRow("Opacidad del fondo", current.revealBgAlpha, 0f..1f, pct(current.revealBgAlpha), { current = current.copy(revealBgAlpha = it) }, white)
                        Text(if (spec.key == Specs.homePattern.key) "Color del patrón" else "Color del número", fontSize = 12.sp, color = white)
                        ColorChips(Palette.text, current.revealColor) { current = current.copy(revealColor = it) }
                    }
                }

                if (isPad) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            val b = spec?.let { current.box(it) }
                            if (b != null && Specs.hitOf(spec) != null) {
                                // Copia el tamaño de la tecla elegida a todas las demás, manteniendo su posición.
                                var s = current
                                for (k in Specs.keys) {
                                    val z = s.box(k) ?: continue
                                    s = s.withBox(k.key, Zone(z.x.coerceAtMost(1f - b.w), z.y.coerceAtMost(1f - b.h), b.w, b.h))
                                }
                                current = s
                            }
                        }) { Text("Mismo tamaño para todas") }
                        OutlinedButton(onClick = { current = current.resetBoxes(Specs.keys.map { it.key }) }) { Text("Rejilla por defecto") }
                    }

                    Text("Resalte al pulsar una tecla", fontSize = 13.sp, color = white)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (st in FeedbackStyle.entries) {
                            FilterChip(
                                selected = current.fbStyle == st,
                                onClick = { current = current.copy(fbStyle = st) },
                                label = { Text(st.label) },
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Switch(checked = current.fbLight, onCheckedChange = { current = current.copy(fbLight = it) })
                        Text(
                            if (current.fbLight) "Resalte claro (teclado oscuro)" else "Resalte oscuro (teclado claro)",
                            fontSize = 12.sp, color = white,
                        )
                    }
                    ColorChips(tints, current.fbTint) { current = current.copy(fbTint = it) }
                    SliderRow("Intensidad", current.fbIntensity, 0.05f..0.9f, pct(current.fbIntensity), { current = current.copy(fbIntensity = it) }, white)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Switch(checked = testing, onCheckedChange = { testing = it })
                        Text("Probar resalte (toca las teclas)", fontSize = 12.sp, color = white)
                    }
                }

                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onDone(current) }) { Text("Guardar") }
                    OutlinedButton(onClick = { onDone(null) }) { Text("Cancelar") }
                    if (selMask != null) {
                        OutlinedButton(onClick = {
                            current = current.removeMask(screenId!!, selMask.id)
                            selected = specs.first().key
                        }) { Text("Eliminar máscara") }
                    } else if (spec != null) {
                        OutlinedButton(onClick = { current = current.resetBox(spec.key) }) { Text("Restablecer caja") }
                    }
                    OutlinedButton(onClick = { panelTop = !panelTop }) { Text(if (panelTop) "Panel abajo" else "Panel arriba") }
                    OutlinedButton(onClick = { panelHidden = true }) { Text("Ocultar panel") }
                }
            }
        }
    }
}
