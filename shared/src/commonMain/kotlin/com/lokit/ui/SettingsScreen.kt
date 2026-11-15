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

/** Ajustes: capturas, número de dígitos, reinicio del efecto y editor de cajas. */
@Composable
fun SettingsScreen(
    settings: AppSettings,
    effect: EffectState,
    images: Map<ScreenId, ImageBitmap?>,
    onSettings: (AppSettings) -> Unit,
    onEffect: (EffectState) -> Unit,
    onPickImage: (ScreenId, ByteArray) -> Unit,
    onRemoveImage: (ScreenId) -> Unit,
    onClose: () -> Unit,
) {
    var target by remember { mutableStateOf<ScreenId?>(null) }
    var editing by remember { mutableStateOf<ScreenId?>(null) }
    val pick = rememberImagePicker { bytes -> target?.let { onPickImage(it, bytes) } }

    val editId = editing
    if (editId != null) {
        BoxEditor(
            id = editId,
            image = images[editId],
            settings = settings,
            onDone = { newSettings ->
                if (newSettings != null) onSettings(newSettings)
                editing = null
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
                Text("Efecto", fontSize = 16.sp)
                val status = when (effect.stage) {
                    0 -> "Esperando el primer PIN del espectador (fallará y se guardará)."
                    1 -> "PIN guardado: ${effect.first}. El siguiente PIN desbloquea."
                    else -> "Desbloqueado. PIN guardado: ${effect.first}."
                }
                Text(status, fontSize = 13.sp)
                Button(onClick = { onEffect(EffectState()) }, enabled = effect.stage != 0) {
                    Text("Reiniciar efecto (borra el PIN guardado)")
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Row(
                Modifier.padding(12.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Dígitos del PIN: ${settings.pinLength}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { if (settings.pinLength > 3) onSettings(settings.copy(pinLength = settings.pinLength - 1)) }) { Text("−") }
                    OutlinedButton(onClick = { if (settings.pinLength < 10) onSettings(settings.copy(pinLength = settings.pinLength + 1)) }) { Text("+") }
                }
            }
        }

        for (id in ScreenId.entries) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(id.title, fontSize = 16.sp)
                    Text(if (images[id] != null) "Captura cargada" else "Sin captura", fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { target = id; pick() }) { Text("Captura") }
                        OutlinedButton(onClick = { onRemoveImage(id) }, enabled = images[id] != null) { Text("Quitar") }
                        OutlinedButton(onClick = { editing = id }) { Text("Cajas") }
                    }
                }
            }
        }

        Text(
            "Acceso a Ajustes: pulsación larga en la esquina superior izquierda de cualquier pantalla. " +
                "Las capturas se ven estiradas a pantalla completa; haz los pantallazos con el mismo móvil que usarás. " +
                "El resalte de las teclas se configura dentro de «Cajas» del teclado de PIN.",
            fontSize = 12.sp, color = Color(0xFF9AA4B2),
        )
        Button(onClick = onClose, Modifier.fillMaxWidth()) { Text("Volver a la app") }
    }
}

/** Tintes del resalte: automático (0) y algunos tonales habituales en teclados. */
private val tints = listOf(
    "Automático" to 0L,
    "Verde Pixel" to 0xFFC5E48AL,
    "Azul" to 0xFFA8C7FAL,
    "Rosa" to 0xFFF2B8D0L,
    "Gris" to 0xFF9E9E9EL,
)

private val boxColors = listOf(Color(0xFF4FC3F7), Color(0xFFFFB74D), Color(0xFF81C784), Color(0xFFE57373))

/**
 * Editor de cajas invisibles sobre la captura: arrastrar para mover, esquina inferior derecha para redimensionar.
 * En el teclado de PIN también configura y prueba el resalte de las teclas.
 */
@Composable
private fun BoxEditor(id: ScreenId, image: ImageBitmap?, settings: AppSettings, onDone: (AppSettings?) -> Unit) {
    val specs = Specs.forScreen(id)
    var current by remember { mutableStateOf(settings) }
    var selected by remember { mutableStateOf(specs.first().key) }
    var panelTop by remember { mutableStateOf(false) }
    var panelHidden by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    val feedback = remember { KeyFeedback() }
    val measurer = rememberTextMeasurer()
    val spec = specs.first { it.key == selected }
    val isPad = id == ScreenId.PAD

    Box(Modifier.fillMaxSize().background(Color(0xFF222222))) {
        if (image != null) Image(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)

        Canvas(
            Modifier.fillMaxSize().pointerInput(testing) {
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
                            val sel = current.box(specs.first { it.key == selected })
                            val nx = o.x / size.width
                            val ny = o.y / size.height
                            mode = when {
                                sel != null && abs(o.x - (sel.x + sel.w) * size.width) < 72f &&
                                    abs(o.y - (sel.y + sel.h) * size.height) < 72f -> 2
                                sel != null && sel.contains(nx, ny) -> 1
                                else -> {
                                    val hit = specs.firstOrNull { current.box(it)?.contains(nx, ny) == true }
                                    if (hit != null) { selected = hit.key; 1 } else 0
                                }
                            }
                        },
                        onDrag = { change, delta ->
                            change.consume()
                            val sp = specs.first { it.key == selected }
                            val b = current.box(sp)
                            if (b != null && mode != 0) {
                                val dx = delta.x / size.width
                                val dy = delta.y / size.height
                                val nb = if (mode == 1) {
                                    b.copy(x = (b.x + dx).coerceIn(0f, 1f - b.w), y = (b.y + dy).coerceIn(0f, 1f - b.h))
                                } else {
                                    b.copy(w = (b.w + dx).coerceIn(0.02f, 1f - b.x), h = (b.h + dy).coerceIn(0.02f, 1f - b.y))
                                }
                                current = current.withBox(sp.key, nb)
                            }
                        },
                    )
                }
            },
        ) {
            specs.forEachIndexed { i, sp ->
                val b = current.box(sp) ?: return@forEachIndexed
                val color = boxColors[i % boxColors.size]
                val isSel = sp.key == selected && !testing
                val topLeft = Offset(b.x * size.width, b.y * size.height)
                val sz = Size(b.w * size.width, b.h * size.height)
                if (!testing) {
                    drawRect(color.copy(alpha = if (isSel) 0.30f else 0.15f), topLeft, sz)
                    drawRect(color, topLeft, sz, style = Stroke(width = if (isSel) 5f else 2f))
                    if (sp.tag.isNotEmpty()) {
                        val layout = measurer.measure(sp.tag, TextStyle(color = Color.White, fontSize = 12.sp))
                        drawText(
                            layout,
                            topLeft = Offset(
                                topLeft.x + (sz.width - layout.size.width) / 2f,
                                topLeft.y + (sz.height - layout.size.height) / 2f,
                            ),
                        )
                    }
                    if (isSel) drawCircle(color, 22f, Offset(topLeft.x + sz.width, topLeft.y + sz.height))
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
                    .heightIn(max = 330.dp).verticalScroll(rememberScrollState()).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Cajas de «${id.title}»", fontSize = 14.sp, color = Color.White)
                if (image == null) Text("Sin captura: las cajas se colocan sobre fondo gris.", fontSize = 11.sp, color = Color(0xFFFFB74D))

                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (sp in specs) {
                        FilterChip(selected = sp.key == selected, onClick = { selected = sp.key; testing = false }, label = { Text(sp.label) })
                    }
                }
                Text(spec.help, fontSize = 11.sp, color = Color(0xFFB0B8C4))
                if (spec.optional) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Switch(
                            checked = current.box(spec) != null,
                            onCheckedChange = { on ->
                                current = current.withBox(spec.key, if (on) (spec.default ?: Zone(0.3f, 0.4f, 0.4f, 0.1f)) else null)
                            },
                        )
                        Text("Activa", fontSize = 13.sp, color = Color.White)
                    }
                }

                if (isPad) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            val b = current.box(spec)
                            if (b != null) {
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

                    Text("Resalte al pulsar una tecla", fontSize = 13.sp, color = Color.White)
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
                            fontSize = 12.sp, color = Color.White,
                        )
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for ((name, argb) in tints) {
                            FilterChip(selected = current.fbTint == argb, onClick = { current = current.copy(fbTint = argb) }, label = { Text(name) })
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Intensidad", fontSize = 12.sp, color = Color.White)
                        Slider(
                            value = current.fbIntensity,
                            onValueChange = { current = current.copy(fbIntensity = it) },
                            valueRange = 0.05f..0.9f,
                            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Switch(checked = testing, onCheckedChange = { testing = it })
                        Text("Probar resalte (toca las teclas)", fontSize = 12.sp, color = Color.White)
                    }
                }

                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onDone(current) }) { Text("Guardar") }
                    OutlinedButton(onClick = { onDone(null) }) { Text("Cancelar") }
                    OutlinedButton(onClick = { current = current.resetBox(spec.key) }) { Text("Restablecer caja") }
                    OutlinedButton(onClick = { panelTop = !panelTop }) { Text(if (panelTop) "Panel abajo" else "Panel arriba") }
                    OutlinedButton(onClick = { panelHidden = true }) { Text("Ocultar panel") }
                }
            }
        }
    }
}
