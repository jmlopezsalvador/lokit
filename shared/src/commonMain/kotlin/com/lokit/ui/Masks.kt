package com.lokit.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import androidx.compose.ui.graphics.toPixelMap
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

/**
 * Relleno automático de una máscara. `box` = (x, y, ancho, alto) en píxeles de la captura. Si hay `src`, se copia de la
 * captura la región de ese tamaño que empieza en `src` (nítida, con la misma textura); si no, se usa el `smooth`.
 */
class MaskFill(val image: ImageBitmap, val iw: Int, val ih: Int, val box: IntArray, val src: IntArray?, val smooth: ImageBitmap?)

private fun profile(pm: androidx.compose.ui.graphics.PixelMap, horizontal: Boolean): FloatArray {
    val n = if (horizontal) pm.width else pm.height
    val out = FloatArray(n * 3)
    val depth = if (horizontal) pm.height else pm.width
    for (i in 0 until n) {
        var r = 0f
        var g = 0f
        var b = 0f
        for (k in 0 until depth) {
            val c = if (horizontal) pm[i, k] else pm[k, i]
            r += c.red; g += c.green; b += c.blue
        }
        out[i * 3] = r / depth; out[i * 3 + 1] = g / depth; out[i * 3 + 2] = b / depth
    }
    return out
}

private fun meanDiff(a: FloatArray, b: FloatArray): Float {
    val n = minOf(a.size, b.size)
    if (n == 0) return 0f
    var sum = 0f
    for (i in 0 until n) sum += kotlin.math.abs(a[i] - b[i])
    return sum / n
}

/**
 * Busca en la propia captura la región del mismo tamaño que mejor encaja con el entorno de la caja (sus bordes se
 * parecen a lo que rodea la caja y no tiene texto ni iconos). Devuelve su esquina, o null si ninguna sirve.
 */
private fun findCloneSource(img: ImageBitmap, x0: Int, y0: Int, w: Int, h: Int): IntArray? {
    val iw = img.width
    val ih = img.height
    if (w < 4 || h < 4) return null
    val hasTop = y0 >= 3
    val hasBottom = y0 + h + 3 <= ih
    val hasLeft = x0 >= 3
    val hasRight = x0 + w + 3 <= iw
    val ringTop = if (hasTop) profile(img.toPixelMap(x0, y0 - 3, w, 2), true) else null
    val ringBottom = if (hasBottom) profile(img.toPixelMap(x0, y0 + h + 1, w, 2), true) else null
    val ringLeft = if (hasLeft) profile(img.toPixelMap(x0 - 3, y0, 2, h), false) else null
    val ringRight = if (hasRight) profile(img.toPixelMap(x0 + w + 1, y0, 2, h), false) else null
    var best: IntArray? = null
    var bestCost = Float.MAX_VALUE
    val shifts = listOf(0 to -1, 0 to 1, -1 to 0, 1 to 0, 0 to -2, 0 to 2, -2 to 0, 2 to 0, -1 to -1, 1 to 1, -1 to 1, 1 to -1)
    for ((kx, ky) in shifts) {
        val sx = x0 + kx * w
        val sy = y0 + ky * h
        if (sx < 0 || sy < 0 || sx + w > iw || sy + h > ih) continue
        val pm = img.toPixelMap(sx, sy, w, h)
        var cost = 0f
        var terms = 0
        ringTop?.let { cost += meanDiff(it, profile(img.toPixelMap(sx, sy, w, 2), true)); terms++ }
        ringBottom?.let { cost += meanDiff(it, profile(img.toPixelMap(sx, sy + h - 2, w, 2), true)); terms++ }
        ringLeft?.let { cost += meanDiff(it, profile(img.toPixelMap(sx, sy, 2, h), false)); terms++ }
        ringRight?.let { cost += meanDiff(it, profile(img.toPixelMap(sx + w - 2, sy, 2, h), false)); terms++ }
        if (terms > 0) cost /= terms
        // Penaliza regiones con detalle (texto, iconos): desviación típica del brillo.
        var sum = 0f
        var sum2 = 0f
        var n = 0
        var yy = 0
        while (yy < h) {
            var xx = 0
            while (xx < w) {
                val c = pm[xx, yy]
                val l = (c.red + c.green + c.blue) / 3f
                sum += l; sum2 += l * l; n++
                xx += 2
            }
            yy += 2
        }
        val mean = sum / n
        val sd = kotlin.math.sqrt((sum2 / n - mean * mean).coerceAtLeast(0f))
        cost += sd * 0.6f
        if (cost < bestCost) { bestCost = cost; best = intArrayOf(sx, sy) }
    }
    return best
}

private fun avgChannels(pm: androidx.compose.ui.graphics.PixelMap, xa: Int, xb: Int, ya: Int, yb: Int): FloatArray {
    var r = 0f
    var g = 0f
    var b = 0f
    var n = 0
    for (yy in ya until yb) for (xx in xa until xb) {
        val c = pm[xx, yy]
        r += c.red; g += c.green; b += c.blue; n++
    }
    return if (n == 0) floatArrayOf(0f, 0f, 0f) else floatArrayOf(r / n, g / n, b / n)
}

/**
 * Muestrea las franjas de la captura justo fuera de la caja (arriba, abajo, izquierda y derecha) y rellena el
 * interior por interpolación entre los cuatro bordes, de forma que la máscara se funde con el fondo.
 */
fun sampleFill(img: ImageBitmap, z: Zone, mode: BgMode = BgMode.AUTO, cols: Int = 24, rows: Int = 32): MaskFill {
    val iw = img.width
    val ih = img.height
    val x0 = (z.x * iw).toInt().coerceIn(0, iw - 2)
    val x1 = ((z.x + z.w) * iw).toInt().coerceIn(x0 + 1, iw)
    val y0 = (z.y * ih).toInt().coerceIn(0, ih - 2)
    val y1 = ((z.y + z.h) * ih).toInt().coerceIn(y0 + 1, ih)
    val w = x1 - x0
    val h = y1 - y0
    if (mode == BgMode.AUTO) {
        val src = findCloneSource(img, x0, y0, w, h)
        if (src != null) return MaskFill(img, iw, ih, intArrayOf(x0, y0, w, h), src, null)
    }
    val top = img.toPixelMap(x0, (y0 - 3).coerceIn(0, ih - 2), w, 2)
    val bottom = img.toPixelMap(x0, (y1 + 1).coerceIn(0, ih - 2), w, 2)
    val left = img.toPixelMap((x0 - 3).coerceIn(0, iw - 2), y0, 2, h)
    val right = img.toPixelMap((x1 + 1).coerceIn(0, iw - 2), y0, 2, h)
    fun span(i: Int, n: Int, total: Int): Pair<Int, Int> {
        val a = i * total / n
        return a to maxOf(a + 1, (i + 1) * total / n)
    }
    // Se suavizan las muestras de cada borde para que la textura (hierba, rocas) no genere un mosaico.
    fun smooth(a: Array<FloatArray>, rad: Int): Array<FloatArray> = Array(a.size) { i ->
        val lo = maxOf(0, i - rad)
        val hi = minOf(a.size - 1, i + rad)
        FloatArray(3) { k ->
            var sum = 0f
            for (j in lo..hi) sum += a[j][k]
            sum / (hi - lo + 1)
        }
    }
    val t = smooth(Array(cols) { c -> span(c, cols, w).let { avgChannels(top, it.first, it.second, 0, 2) } }, 2)
    val bo = smooth(Array(cols) { c -> span(c, cols, w).let { avgChannels(bottom, it.first, it.second, 0, 2) } }, 2)
    val l = smooth(Array(rows) { r -> span(r, rows, h).let { avgChannels(left, 0, 2, it.first, it.second) } }, 2)
    val rg = smooth(Array(rows) { r -> span(r, rows, h).let { avgChannels(right, 0, 2, it.first, it.second) } }, 2)
    fun mix(a: FloatArray, b: FloatArray) = FloatArray(3) { (a[it] + b[it]) / 2f }
    val tl = mix(t[0], l[0])
    val tr = mix(t[cols - 1], rg[0])
    val bl = mix(bo[0], l[rows - 1])
    val br = mix(bo[cols - 1], rg[rows - 1])
    val bmp = ImageBitmap(cols, rows)
    val canvas = androidx.compose.ui.graphics.Canvas(bmp)
    val paint = androidx.compose.ui.graphics.Paint()
    for (r in 0 until rows) for (c in 0 until cols) {
        val u = (c + 0.5f) / cols
        val v = (r + 0.5f) / rows
        val ch = FloatArray(3) { k ->
            // Interpolación transfinita (Coons): bordes opuestos menos la parte bilineal de las esquinas.
            val value = (1 - v) * t[c][k] + v * bo[c][k] + (1 - u) * l[r][k] + u * rg[r][k] -
                ((1 - u) * (1 - v) * tl[k] + u * (1 - v) * tr[k] + (1 - u) * v * bl[k] + u * v * br[k])
            value.coerceIn(0f, 1f)
        }
        paint.color = Color(ch[0], ch[1], ch[2])
        canvas.drawRect(c.toFloat(), r.toFloat(), c + 1f, r + 1f, paint)
    }
    return MaskFill(img, iw, ih, intArrayOf(x0, y0, w, h), null, bmp)
}

/** Pinta el fondo de una máscara sobre la zona. */
fun DrawScope.drawMaskBackground(zone: Zone, style: MaskStyle, fill: MaskFill?) {
    val left = zone.x * size.width
    val top = zone.y * size.height
    val w = zone.w * size.width
    val h = zone.h * size.height
    when (style.bgMode) {
        BgMode.NONE -> {}
        BgMode.SOLID -> drawRect(argb(style.bgColor), Offset(left, top), Size(w, h))
        BgMode.AUTO, BgMode.SMOOTH -> {
            if (fill == null) return
            // La región se calcula en píxeles de la captura y se coloca con la misma escala que la captura.
            val sx = size.width / fill.iw
            val sy = size.height / fill.ih
            val dx = (fill.box[0] * sx).toInt()
            val dy = (fill.box[1] * sy).toInt()
            val dw = ((fill.box[0] + fill.box[2]) * sx).toInt() + 1 - dx
            val dh = ((fill.box[1] + fill.box[3]) * sy).toInt() + 1 - dy
            val src = fill.src
            val smooth = fill.smooth
            if (src != null) {
                drawImage(
                    fill.image,
                    srcOffset = IntOffset(src[0], src[1]), srcSize = IntSize(fill.box[2], fill.box[3]),
                    dstOffset = IntOffset(dx, dy), dstSize = IntSize(dw, dh),
                    filterQuality = FilterQuality.Low,
                )
            } else if (smooth != null) {
                drawImage(smooth, dstOffset = IntOffset(dx, dy), dstSize = IntSize(dw, dh), filterQuality = FilterQuality.High)
            }
        }
    }
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
 * Capa de una máscara: fondo (según `style.bgMode`) y, si hay `lines`, el texto. `debounceMs` retrasa el cálculo del
 * relleno automático mientras se arrastra la caja en el editor.
 */
@Composable
fun MaskLayer(zone: Zone, style: MaskStyle, image: ImageBitmap?, lines: List<String>?, debounceMs: Long = 0L) {
    val measurer = rememberTextMeasurer()
    val auto = style.bgMode == BgMode.AUTO || style.bgMode == BgMode.SMOOTH
    val fill by produceState<MaskFill?>(null, image, zone, style.bgMode) {
        value = null
        if (auto && image != null) {
            if (debounceMs > 0) delay(debounceMs)
            value = runCatching { sampleFill(image, zone, style.bgMode) }.getOrNull()
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        drawMaskBackground(zone, style, fill)
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
