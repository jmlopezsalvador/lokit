package com.lokit.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
                onSettings = { settings = it; store.save(it) },
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

    // Al completar el PIN: el primero se guarda y falla; los siguientes desbloquean.
    LaunchedEffect(entered, settings.pinLength) {
        if (entered.length >= settings.pinLength) {
            delay(180)
            val (next, unlocked) = effectNow.submit(entered)
            onEffectNow(next)
            entered = ""
            if (unlocked) screen = ScreenId.HOME else failTick++
        }
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
        reveal = false
        lockActive = false
        screen = ScreenId.LOCK
    }

    fun onTap(nx: Float, ny: Float) {
        when (screen) {
            ScreenId.LOCK -> when {
                settings.dimEnabled && !lockActive -> { lockActive = true; touchTick++ }
                settings.box(Specs.lockOpen)?.contains(nx, ny) == true -> { entered = ""; screen = ScreenId.PAD }
                else -> touchTick++
            }
            ScreenId.PAD -> {
                if (settings.box(Specs.padBack)?.contains(nx, ny) == true) {
                    entered = ""
                    lockActive = false
                    screen = ScreenId.LOCK
                    return
                }
                when (val hit = hitKey(settings, nx, ny)?.first) {
                    is KeyHit.Digit -> if (entered.length < settings.pinLength) entered += hit.d
                    KeyHit.Delete -> entered = entered.dropLast(1)
                    null -> {}
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

        // Modo atenuado: toda la escena baja de intensidad.
        if (dimAlpha > 0.001f) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dimAlpha)))

        if (reveal) {
            val view = settings.box(Specs.homeRevealView) ?: Specs.homeRevealView.default!!
            Canvas(Modifier.fillMaxSize()) {
                drawReveal(measurer, view, settings, effect.first.ifEmpty { "—" })
            }
        }
    }
}
