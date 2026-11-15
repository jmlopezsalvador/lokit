package com.lokit.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.min

/** Estado de la animación de resalte de la tecla pulsada (sirve para la app y para probarlo en Ajustes). */
class KeyFeedback {
    private val alpha = Animatable(0f)
    private val progress = Animatable(1f)
    private var zone by mutableStateOf<Zone?>(null)

    /** Resalta `z` mientras dure el toque (`awaitRelease` espera a que se suelte) y luego lo desvanece. */
    suspend fun run(settings: AppSettings, z: Zone, awaitRelease: suspend () -> Unit) {
        if (settings.fbStyle == FeedbackStyle.NONE) {
            awaitRelease()
            return
        }
        zone = z
        alpha.snapTo(1f)
        val ripple = settings.fbStyle == FeedbackStyle.RIPPLE
        progress.snapTo(if (ripple) 0f else 1f)
        coroutineScope {
            if (ripple) launch { progress.animateTo(1f, tween(320)) }
            awaitRelease()
            alpha.animateTo(0f, tween(240))
        }
    }

    fun draw(scope: DrawScope, settings: AppSettings) {
        val z = zone ?: return
        val a = alpha.value
        if (a <= 0.001f || settings.fbStyle == FeedbackStyle.NONE) return
        with(scope) { drawNow(z, a, settings) }
    }

    private fun DrawScope.drawNow(z: Zone, a: Float, settings: AppSettings) {
        val base = if (settings.fbTint != 0L) Color(settings.fbTint.toInt()) else if (settings.fbLight) Color.White else Color.Black
        val color = base.copy(alpha = (settings.fbIntensity * a).coerceIn(0f, 1f))
        val left = z.x * size.width
        val top = z.y * size.height
        val w = z.w * size.width
        val h = z.h * size.height
        val center = Offset(left + w / 2f, top + h / 2f)
        val inscribed = min(w, h) / 2f
        // Mezcla con la captura para que el número de la tecla no se tape: Multiplicar en teclados claros, Trama en oscuros.
        val bm = if (settings.fbLight) BlendMode.Screen else BlendMode.Multiply
        when (settings.fbStyle) {
            FeedbackStyle.CIRCLE -> drawCircle(color, inscribed, center, blendMode = bm)
            FeedbackStyle.PILL -> drawRoundRect(color, Offset(left, top), Size(w, h), CornerRadius(min(w, h) / 2f), blendMode = bm)
            FeedbackStyle.SQUARE -> drawRoundRect(color, Offset(left, top), Size(w, h), CornerRadius(min(w, h) * 0.28f), blendMode = bm)
            FeedbackStyle.RIPPLE -> drawCircle(color, inscribed * (0.25f + 0.85f * progress.value), center, blendMode = bm)
            FeedbackStyle.NONE -> {}
        }
    }
}
