package com.lokit.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/** Raíz Compose común (Android e iOS). */
@Composable
fun LokitRoot(store: SettingsStore, blobs: BlobStore) {
    SystemBarsEffect()
    var settings by remember { mutableStateOf(store.load()) }
    var effect by remember { mutableStateOf(store.loadEffect()) }
    val images = remember { mutableStateMapOf<ScreenId, ImageBitmap?>() }
    val cleans = remember { mutableStateMapOf<ScreenId, ImageBitmap?>() }
    var inSettings by remember { mutableStateOf(false) }
    // Modal de arranque: elegir código o patrón (configurable en Ajustes).
    var chooser by remember { mutableStateOf(settings.askAtStart) }
    // Guía rápida de arranque: sale primero; al cerrarla aparece, si procede, el modal de elegir tipo.
    var guide by remember { mutableStateOf(settings.guideAtStart) }

    /** Cambiar de tipo de desbloqueo reinicia el efecto: lo guardado (PIN o patrón) ya no vale para el otro tipo. */
    fun applySettings(new: AppSettings) {
        if (new.unlockMode != settings.unlockMode) {
            effect = EffectState()
            store.saveEffect(effect)
        }
        settings = new
        store.save(new)
    }

    LaunchedEffect(Unit) {
        for (id in ScreenId.entries) {
            blobs.read(id.file)?.let { images[id] = decodeImage(it) }
            blobs.read(id.cleanFile)?.let { cleans[id] = decodeImage(it) }
        }
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        if (inSettings) {
            SettingsScreen(
                settings = settings,
                effect = effect,
                images = images,
                cleans = cleans,
                onSettings = { applySettings(it) },
                onPickImage = { id, bytes ->
                    blobs.write(id.file, bytes)
                    images[id] = decodeImage(bytes)
                },
                onRemoveImage = { id ->
                    blobs.delete(id.file)
                    images[id] = null
                },
                onPickClean = { id, bytes ->
                    blobs.write(id.cleanFile, bytes)
                    cleans[id] = decodeImage(bytes)
                },
                onRemoveClean = { id ->
                    blobs.delete(id.cleanFile)
                    cleans[id] = null
                },
                onClose = { inSettings = false },
            )
        } else {
            FakePhone(
                settings = settings,
                effect = effect,
                images = images,
                cleans = cleans,
                onEffect = { effect = it; store.saveEffect(it) },
                onOpenSettings = { inSettings = true },
            )
            if (guide) {
                GuideDialog(showAtStart = settings.guideAtStart) { show ->
                    if (show != settings.guideAtStart) applySettings(settings.copy(guideAtStart = show))
                    guide = false
                }
            } else if (chooser) {
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text("¿Cómo se desbloquea?") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            for (m in listOf(UnlockMode.PIN to "Código (PIN)", UnlockMode.PATTERN to "Patrón")) {
                                Button(
                                    onClick = { applySettings(settings.copy(unlockMode = m.first)); chooser = false },
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text(m.second) }
                            }
                            Text(
                                if (settings.unlockMode == UnlockMode.PIN) "Ahora: código (PIN)" else "Ahora: patrón",
                                fontSize = 12.sp, color = Color(0xFF9AA4B2),
                            )
                        }
                    },
                    confirmButton = {},
                )
            }
        }
    }
}

/** El móvil falso: bloqueo → teclado → escritorio. Todo se compone con capturas y cajas invisibles. */
@Composable
private fun FakePhone(
    settings: AppSettings,
    effect: EffectState,
    images: Map<ScreenId, ImageBitmap?>,
    cleans: Map<ScreenId, ImageBitmap?>,
    onEffect: (EffectState) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var screen by remember { mutableStateOf(ScreenId.LOCK) }
    var entered by remember { mutableStateOf("") }
    var patternPath by remember { mutableStateOf(emptyList<Int>()) }
    var patternFinger by remember { mutableStateOf<Offset?>(null) }
    var failTick by remember { mutableStateOf(0) }
    var reveal by remember { mutableStateOf(false) }
    var lockActive by remember { mutableStateOf(false) } // pantalla de bloqueo "despierta" (modo atenuado)
    var touchTick by remember { mutableStateOf(0) }
    val holdConsumed = remember { BooleanArray(1) }
    val shake = remember { Animatable(0f) }
    val feedback = remember { KeyFeedback() }
    val effectNow by rememberUpdatedState(effect)
    val onEffectNow by rememberUpdatedState(onEffect)
    val onOpenSettingsNow by rememberUpdatedState(onOpenSettings)
    val now = rememberNow()
    val measurer = rememberTextMeasurer()
    val vibrate = rememberVibrator()

    // Sacudida de los puntos cuando el PIN "falla".
    LaunchedEffect(failTick) {
        if (failTick > 0) {
            repeat(3) {
                shake.animateTo(1f, tween(45))
                shake.animateTo(-1f, tween(45))
            }
            shake.animateTo(0f, tween(45))
        }
    }

    // Intento de desbloqueo: el primero se guarda; fallan hasta el último intento, que desbloquea.
    fun attempt(value: String) {
        val (next, unlocked) = effectNow.submit(value, settings.attempts)
        onEffectNow(next)
        entered = ""
        patternPath = emptyList()
        if (unlocked) screen = ScreenId.HOME else { failTick++; vibrate(1) } // fallo: una vibración
    }

    // Tecla OK del PIN: solo vale con el mínimo de dígitos.
    fun submitPin() {
        if (entered.length >= settings.minDigits) attempt(entered)
    }

    // Al soltar el dedo en el patrón: solo vale con el mínimo de puntos; si no, se borra el trazo.
    fun submitPattern(path: List<Int>) {
        if (path.size >= settings.patternMin) attempt(Pattern.encode(settings.patternGrid, path)) else patternPath = emptyList()
    }

    // Modo atenuado: tras X segundos sin tocar, la pantalla de bloqueo vuelve a apagarse.
    LaunchedEffect(screen, lockActive, touchTick, settings.dimEnabled, settings.dimSeconds) {
        if (screen == ScreenId.LOCK && settings.dimEnabled && lockActive) {
            delay(settings.dimSeconds * 1000L)
            lockActive = false
        }
    }

    val dimmed = screen == ScreenId.LOCK && settings.dimEnabled && !lockActive
    val dimAlpha by animateFloatAsState(if (dimmed) settings.dimLevel else 0f, tween(350))
    val blurDp by animateDpAsState(if (dimmed) settings.dimBlur.dp else 0.dp, tween(350))

    /** Reinicia el efecto: borra el PIN guardado y vuelve a la pantalla de bloqueo apagada. */
    fun resetAll() {
        onEffectNow(EffectState())
        entered = ""
        patternPath = emptyList()
        reveal = false
        lockActive = false
        screen = ScreenId.LOCK
    }

    fun onTap(nx: Float, ny: Float) {
        when (screen) {
            ScreenId.LOCK -> when {
                settings.dimEnabled && !lockActive -> { lockActive = true; touchTick++ }
                settings.box(Specs.lockOpen)?.contains(nx, ny) == true -> {
                    entered = ""
                    screen = if (settings.unlockMode == UnlockMode.PATTERN) ScreenId.PATTERN else ScreenId.PAD
                }
                else -> touchTick++
            }
            ScreenId.PAD, ScreenId.PATTERN -> {
                if (settings.box(Specs.padBack)?.contains(nx, ny) == true) {
                    entered = ""
                    patternPath = emptyList()
                    lockActive = false
                    screen = ScreenId.LOCK
                    return
                }
                if (screen == ScreenId.PAD) {
                    when (val hit = hitKey(settings, nx, ny)?.first) {
                        is KeyHit.Digit -> if (entered.length < settings.maxDigits) entered += hit.d
                        KeyHit.Delete -> entered = entered.dropLast(1)
                        KeyHit.Ok -> submitPin()
                        null -> {}
                    }
                }
            }
            ScreenId.HOME -> if (settings.box(Specs.homeLock)?.contains(nx, ny) == true) resetAll()
        }
    }

    Box(
        Modifier.fillMaxSize().background(Color.Black).pointerInput(screen, settings) {
            detectTapGestures(
                onPress = { o ->
                    holdConsumed[0] = false
                    val nx = o.x / size.width
                    val ny = o.y / size.height
                    val onSettingsBox = settings.box(Specs.settingsBox)?.contains(nx, ny) == true
                    val onResetBox = !onSettingsBox && settings.box(Specs.resetBox)?.contains(nx, ny) == true
                    if (onSettingsBox || onResetBox) {
                        // Botones ocultos: acción tras mantener la pulsación el tiempo configurado.
                        val ms = if (onSettingsBox) settings.settingsHoldMs else settings.resetHoldMs
                        val released = withTimeoutOrNull(ms.toLong()) { tryAwaitRelease() }
                        if (released == null) {
                            holdConsumed[0] = true
                            vibrate(2) // doble vibración al activar el botón oculto
                            if (onSettingsBox) onOpenSettingsNow() else resetAll()
                            tryAwaitRelease()
                        }
                    } else if (screen == ScreenId.HOME && settings.box(Specs.homeReveal)?.contains(nx, ny) == true) {
                        // El PIN se ve solo mientras se mantiene pulsado.
                        reveal = true
                        try {
                            tryAwaitRelease()
                        } finally {
                            reveal = false
                        }
                    } else if (screen == ScreenId.PAD) {
                        // Resalte de la tecla mientras se mantiene pulsada.
                        val key = hitKey(settings, nx, ny)
                        if (key != null) feedback.run(settings, key.second) { tryAwaitRelease() }
                    }
                },
                onTap = { o ->
                    if (holdConsumed[0]) holdConsumed[0] = false else onTap(o.x / size.width, o.y / size.height)
                },
            )
        },
    ) {
        val img = images[screen]
        if (img != null) {
            val blurMod = if (blurDp > 0.dp) Modifier.blur(blurDp, BlurredEdgeTreatment.Rectangle) else Modifier
            // La captura y los fondos de las máscaras se desenfocan juntos; el texto real queda nítido encima.
            Box(Modifier.fillMaxSize().then(blurMod)) {
                Image(img, null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                for (m in settings.masksOf(screen)) {
                    key(m.id) { MaskLayer(m.zone, m.style, cleans[screen], null) }
                }
            }
        } else {
            Column(
                Modifier.fillMaxSize().background(Color(0xFF14181F)).padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Sin captura para «${screen.title}»", color = Color.White, textAlign = TextAlign.Center)
                Text(
                    "Abre Ajustes (pulsación larga en la esquina superior derecha) y elige una captura.",
                    color = Color(0xFF9AA4B2), fontSize = 13.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
                Button(onClick = onOpenSettings) { Text("Ajustes") }
            }
        }

        // Máscaras con la fecha y la hora reales (tapan las fijas de la captura).
        for (m in settings.masksOf(screen)) {
            key(m.id) { MaskLayer(m.zone, m.style, cleans[screen], maskLines(m.style.kind, now), showBg = false) }
        }

        if (screen == ScreenId.PAD) {
            Canvas(Modifier.fillMaxSize()) { feedback.draw(this, settings) }
            val dots = settings.box(Specs.padDots)
            if (dots != null) {
                MaskLayer(dots, settings.mask(Specs.padDots), cleans[screen], null)
                Canvas(Modifier.fillMaxSize()) {
                    drawPinDots(dots, settings, entered.length, shake.value * size.width * 0.03f)
                }
            }
        }

        // Modo patrón: la caja tapa la rejilla de la captura y la app dibuja la suya; el dedo traza el patrón.
        if (screen == ScreenId.PATTERN) {
            val z = settings.box(Specs.padPattern)
            if (z != null) {
                MaskLayer(z, settings.mask(Specs.padPattern), cleans[screen], null)
                Canvas(Modifier.fillMaxSize()) { drawPatternPad(z, settings, patternPath, patternFinger) }
                Box(
                    Modifier.fillMaxSize().pointerInput(z, settings) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()
                            // Un toque fuera de la rejilla no es para el patrón: lo recibe el resto (p. ej. el botón atrás).
                            if (!z.contains(down.position.x / w, down.position.y / h)) return@awaitEachGesture
                            down.consume()
                            val n = settings.patternGrid
                            // La rejilla tiene tamaño propio (ajustes): la caja solo la centra y marca la zona táctil.
                            val geo = patternGeometry(settings, z, w, h)
                            var path = emptyList<Int>()
                            fun touch(o: Offset) {
                                val node = geo.nodeAt(o.x, o.y)
                                if (node >= 0) path = Pattern.extend(path, node, n)
                                patternPath = path
                                patternFinger = o
                            }
                            touch(down.position)
                            while (true) {
                                val ch = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                                ch.consume()
                                if (!ch.pressed) break
                                touch(ch.position)
                            }
                            patternFinger = null
                            submitPattern(path)
                        }
                    },
                )
            }
        }

        // Modo atenuado: toda la escena baja de intensidad.
        if (dimAlpha > 0.001f) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dimAlpha)))

        if (reveal) {
            if (settings.unlockMode == UnlockMode.PATTERN) {
                val view = settings.box(Specs.homePattern) ?: Specs.homePattern.default!!
                Canvas(Modifier.fillMaxSize()) { drawPatternReveal(measurer, view, settings, effect.first) }
            } else {
                val view = settings.box(Specs.homeRevealView) ?: Specs.homeRevealView.default!!
                Canvas(Modifier.fillMaxSize()) {
                    drawReveal(measurer, view, settings, effect.first.ifEmpty { "—" })
                }
            }
        }
    }
}
