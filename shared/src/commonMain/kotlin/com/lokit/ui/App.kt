package com.lokit.ui

import androidx.compose.animation.core.Animatable
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Raíz Compose común (Android e iOS). */
@Composable
fun LokitRoot(store: SettingsStore, blobs: BlobStore) {
    SystemBarsEffect()
    var settings by remember { mutableStateOf(store.load()) }
    var effect by remember { mutableStateOf(store.loadEffect()) }
    val images = remember { mutableStateMapOf<ScreenId, ImageBitmap?>() }
    var inSettings by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        for (id in ScreenId.entries) blobs.read(id.file)?.let { images[id] = decodeImage(it) }
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        if (inSettings) {
            SettingsScreen(
                settings = settings,
                effect = effect,
                images = images,
                onSettings = { settings = it; store.save(it) },
                onEffect = { effect = it; store.saveEffect(it) },
                onPickImage = { id, bytes ->
                    blobs.write(id.file, bytes)
                    images[id] = decodeImage(bytes)
                },
                onRemoveImage = { id ->
                    blobs.delete(id.file)
                    images[id] = null
                },
                onClose = { inSettings = false },
            )
        } else {
            FakePhone(
                settings = settings,
                effect = effect,
                images = images,
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
    onEffect: (EffectState) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var screen by remember { mutableStateOf(ScreenId.LOCK) }
    var entered by remember { mutableStateOf("") }
    var failTick by remember { mutableStateOf(0) }
    var reveal by remember { mutableStateOf(false) }
    val shake = remember { Animatable(0f) }
    val feedback = remember { KeyFeedback() }
    val effectNow by rememberUpdatedState(effect)
    val onEffectNow by rememberUpdatedState(onEffect)

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

    fun onTap(nx: Float, ny: Float) {
        if (reveal) { reveal = false; return }
        when (screen) {
            ScreenId.LOCK -> if (settings.box(Specs.lockOpen)?.contains(nx, ny) == true) {
                entered = ""
                screen = ScreenId.PAD
            }
            ScreenId.PAD -> {
                if (settings.box(Specs.padBack)?.contains(nx, ny) == true) {
                    entered = ""
                    screen = ScreenId.LOCK
                    return
                }
                when (val hit = hitKey(settings, nx, ny)?.first) {
                    is KeyHit.Digit -> if (entered.length < settings.pinLength) entered += hit.d
                    KeyHit.Delete -> entered = entered.dropLast(1)
                    null -> {}
                }
            }
            ScreenId.HOME -> {
                if (settings.box(Specs.homeReveal)?.contains(nx, ny) == true) reveal = true
                else if (settings.box(Specs.homeLock)?.contains(nx, ny) == true) screen = ScreenId.LOCK
            }
        }
    }

    Box(
        Modifier.fillMaxSize().background(Color.Black).pointerInput(screen, settings, reveal) {
            detectTapGestures(
                // Acceso secreto a Ajustes: pulsación larga en la esquina superior izquierda.
                onLongPress = { o -> if (o.x < size.width * 0.2f && o.y < size.height * 0.08f) onOpenSettings() },
                onPress = { o ->
                    // Resalte de la tecla mientras se mantiene pulsada.
                    val key = if (screen == ScreenId.PAD) hitKey(settings, o.x / size.width, o.y / size.height) else null
                    if (key != null) feedback.run(settings, key.second) { tryAwaitRelease() }
                },
                onTap = { o -> onTap(o.x / size.width, o.y / size.height) },
            )
        },
    ) {
        val img = images[screen]
        if (img != null) {
            Image(img, null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
        } else {
            Column(
                Modifier.fillMaxSize().background(Color(0xFF14181F)).padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Sin captura para «${screen.title}»", color = Color.White, textAlign = TextAlign.Center)
                Text(
                    "Abre Ajustes (también con pulsación larga en la esquina superior izquierda) y elige una captura.",
                    color = Color(0xFF9AA4B2), fontSize = 13.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
                Button(onClick = onOpenSettings) { Text("Ajustes") }
            }
        }

        if (screen == ScreenId.PAD) {
            Canvas(Modifier.fillMaxSize()) { feedback.draw(this, settings) }
            val dots = settings.box(Specs.padDots)
            if (dots != null) {
                Canvas(Modifier.fillMaxSize()) {
                    val n = settings.pinLength
                    val left = dots.x * size.width
                    val top = dots.y * size.height
                    val wPx = dots.w * size.width
                    val hPx = dots.h * size.height
                    val step = wPx / n
                    val r = minOf(hPx / 2f, step / 2.6f)
                    // Puntos oscuros sobre teclados claros, claros sobre teclados oscuros.
                    val dot = if (settings.fbLight) Color.White else Color(0xFF1B1C18)
                    for (i in 0 until n) {
                        val c = Offset(left + step * (i + 0.5f) + shake.value * size.width * 0.03f, top + hPx / 2f)
                        if (i < entered.length) drawCircle(dot, r, c)
                        else drawCircle(dot.copy(alpha = 0.35f), r, c, style = Stroke(width = r * 0.3f))
                    }
                }
            }
        }

        if (reveal) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.93f)), contentAlignment = Alignment.Center) {
                Text(effect.first.ifEmpty { "—" }, color = Color.White, fontSize = 56.sp)
            }
        }
    }
}
