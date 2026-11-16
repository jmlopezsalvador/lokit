package com.lokit.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun currentTime(): LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

/** Hora actual real, refrescada cada segundo. */
@Composable
fun rememberNow(): LocalDateTime {
    var now by remember { mutableStateOf(currentTime()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = currentTime()
        }
    }
    return now
}

private fun argb(v: Long) = Color(v.toInt())

/** Pinta el fondo de una máscara: el mismo trozo de la captura limpia (misma zona normalizada, a la misma escala). */
fun DrawScope.drawMaskBackground(zone: Zone, style: MaskStyle, clean: ImageBitmap?) {
    if (style.bgMode != BgMode.IMAGE || clean == null) return
    val sx = (zone.x * clean.width).toInt().coerceIn(0, clean.width - 1)
    val sy = (zone.y * clean.height).toInt().coerceIn(0, clean.height - 1)
    val sw = (zone.w * clean.width).toInt().coerceIn(1, clean.width - sx)
    val sh = (zone.h * clean.height).toInt().coerceIn(1, clean.height - sy)
    val dx = (zone.x * size.width).toInt()
    val dy = (zone.y * size.height).toInt()
    drawImage(
        clean,
        srcOffset = IntOffset(sx, sy), srcSize = IntSize(sw, sh),
        dstOffset = IntOffset(dx, dy),
        dstSize = IntSize(((zone.x + zone.w) * size.width).toInt() - dx, ((zone.y + zone.h) * size.height).toInt() - dy),
        filterQuality = FilterQuality.Low,
    )
}

private val fonts = listOf(FontFamily.Default, FontFamily.Serif, FontFamily.Monospace, FontFamily.Cursive)

/**
 * Pinta las líneas de texto de una máscara. El tamaño de letra es absoluto (`style.size` × alto de pantalla): la caja
 * solo decide dónde se alinea el texto, así que cambiarla de tamaño no altera el contenido.
 */
fun DrawScope.drawMaskText(measurer: TextMeasurer, zone: Zone, style: MaskStyle, lines: List<String>) {
    if (lines.isEmpty()) return
    val left = zone.x * size.width
    val top = zone.y * size.height
    val w = zone.w * size.width
    val h = zone.h * size.height
    val fontPx = style.size * size.height
    val ts = TextStyle(
        color = argb(style.textColor),
        fontSize = fontPx.toSp(),
        fontFamily = fonts[style.font.coerceIn(0, fonts.size - 1)],
        fontWeight = when (style.weight) { 2 -> FontWeight.Bold; 1 -> FontWeight.Normal; else -> FontWeight.Light },
        letterSpacing = (style.spacing * fontPx).toSp(),
    )
    val layouts = lines.map { measurer.measure(it, ts) }
    val step = layouts.first().size.height * 0.85f
    val total = step * (layouts.size - 1) + layouts.last().size.height
    var y = top + (h - total) / 2f
    for (l in layouts) {
        val x = when (style.align) {
            0 -> left
            2 -> left + w - l.size.width
            else -> left + (w - l.size.width) / 2f
        }
        drawText(l, topLeft = Offset(x, y))
        y += step
    }
}

/**
 * Capa de una máscara: fondo (trozo de la captura limpia, si el modo lo pide) y, si hay `lines`, el texto.
 * `showBg = false` dibuja solo el texto (el fondo va con la captura cuando esta se desenfoca).
 */
@Composable
fun MaskLayer(zone: Zone, style: MaskStyle, clean: ImageBitmap?, lines: List<String>?, showBg: Boolean = true) {
    val measurer = rememberTextMeasurer()
    Canvas(Modifier.fillMaxSize()) {
        if (showBg) drawMaskBackground(zone, style, clean)
        if (lines != null) drawMaskText(measurer, zone, style, lines)
    }
}

/** Puntos del PIN: `filled` rellenos y el resto como anillos, con tamaño, separación y borde configurables. */
fun DrawScope.drawPinDots(zone: Zone, s: AppSettings, filled: Int, shakePx: Float) {
    val n = s.pinLength
    val left = zone.x * size.width
    val top = zone.y * size.height
    val w = zone.w * size.width
    val h = zone.h * size.height
    val d = h * s.dotSize
    val r = d / 2f
    val gap = d * s.dotGap
    val total = n * d + (n - 1) * gap
    val startX = left + (w - total) / 2f + r
    // Oscuros sobre teclados claros, claros sobre teclados oscuros.
    val dot = if (s.fbLight) Color.White else Color(0xFF1B1C18)
    val stroke = r * s.dotBorder
    for (i in 0 until n) {
        val c = Offset(startX + i * (d + gap) + shakePx, top + h / 2f)
        if (i < filled) drawCircle(dot, r, c)
        else drawCircle(dot.copy(alpha = 0.35f), (r - stroke / 2f).coerceAtLeast(1f), c, style = Stroke(width = stroke))
    }
}

/** Fondo oscurecido y número del PIN revelado, ajustado a la caja `view`. */
fun DrawScope.drawReveal(measurer: TextMeasurer, view: Zone, s: AppSettings, text: String, withBackground: Boolean = true) {
    if (withBackground) drawRect(Color.Black.copy(alpha = s.revealBgAlpha), Offset.Zero, size)
    val left = view.x * size.width
    val top = view.y * size.height
    val w = view.w * size.width
    val h = view.h * size.height
    val fontPx = minOf(h * 0.85f, w / (text.length.coerceAtLeast(1) * 0.6f))
    val layout = measurer.measure(text, TextStyle(color = argb(s.revealColor), fontSize = fontPx.toSp()))
    drawText(layout, topLeft = Offset(left + (w - layout.size.width) / 2f, top + (h - layout.size.height) / 2f))
}
