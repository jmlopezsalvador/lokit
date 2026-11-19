package com.lokit.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.min
import kotlin.math.sqrt

private fun color(v: Long) = Color(v.toInt())

/**
 * Rejilla del patrón en la pantalla de desbloqueo: puntos, trazo recorrido y, mientras el dedo sigue pulsando,
 * la línea desde el último punto hasta el dedo (`finger`, en píxeles del lienzo).
 */
fun DrawScope.drawPatternPad(zone: Zone, s: AppSettings, path: List<Int>, finger: Offset?) {
    val g = patternGeometry(s, zone, size.width, size.height)
    val dotColor = color(s.patternDotColor)
    val lineColor = color(s.patternLineColor)
    fun center(i: Int) = Offset(g.cx(i), g.cy(i))
    val stroke = (s.patternLine * size.height).coerceAtLeast(1f)
    for (k in 1 until path.size) drawLine(lineColor, center(path[k - 1]), center(path[k]), stroke, StrokeCap.Round)
    if (finger != null && path.isNotEmpty()) drawLine(lineColor, center(path.last()), finger, stroke, StrokeCap.Round)
    val r = g.dot / 2f
    for (i in 0 until g.n * g.n) {
        if (i in path) {
            drawCircle(dotColor.copy(alpha = 0.25f), r * 2.2f, center(i))
            drawCircle(dotColor, r, center(i))
        } else {
            drawCircle(dotColor.copy(alpha = 0.9f), r, center(i))
        }
    }
}

/**
 * Patrón guardado, pequeño, dentro de un cuadrado centrado en la caja `view`: puntos recorridos numerados por orden,
 * líneas entre ellos y una flecha en cada tramo que marca la dirección.
 */
fun DrawScope.drawPatternReveal(measurer: TextMeasurer, view: Zone, s: AppSettings, encoded: String, withBackground: Boolean = true) {
    if (withBackground) drawRect(Color.Black.copy(alpha = s.revealBgAlpha), Offset.Zero, size)
    val (n, nodes) = Pattern.decode(encoded) ?: return
    val w = view.w * size.width
    val h = view.h * size.height
    val side = min(w, h)
    val left = view.x * size.width + (w - side) / 2f
    val top = view.y * size.height + (h - side) / 2f
    val cell = side / n
    val c = color(s.revealColor)
    fun center(i: Int) = Offset(left + (i % n + 0.5f) * cell, top + (i / n + 0.5f) * cell)
    val dotR = cell * 0.17f

    // Puntos de la rejilla sin usar, muy tenues.
    for (i in 0 until n * n) if (i !in nodes) drawCircle(c.copy(alpha = 0.3f), dotR * 0.45f, center(i))

    // Líneas.
    for (k in 1 until nodes.size) {
        drawLine(c.copy(alpha = 0.6f), center(nodes[k - 1]), center(nodes[k]), cell * 0.05f, StrokeCap.Round)
    }
    // Flechas en el punto medio de cada tramo, apuntando hacia el siguiente punto.
    for (k in 1 until nodes.size) {
        val a = center(nodes[k - 1])
        val b = center(nodes[k])
        val dx = b.x - a.x
        val dy = b.y - a.y
        val len = sqrt(dx * dx + dy * dy)
        if (len < 1f) continue
        val ux = dx / len
        val uy = dy / len
        val mid = Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)
        val l = cell * 0.13f
        val half = cell * 0.09f
        val tip = Offset(mid.x + ux * l, mid.y + uy * l)
        val base = Offset(mid.x - ux * l * 0.6f, mid.y - uy * l * 0.6f)
        val p = Path().apply {
            moveTo(tip.x, tip.y)
            lineTo(base.x - uy * half, base.y + ux * half)
            lineTo(base.x + uy * half, base.y - ux * half)
            close()
        }
        drawPath(p, c)
    }
    // Puntos recorridos con su número de orden (el primero, algo mayor).
    nodes.forEachIndexed { k, i ->
        val r = if (k == 0) dotR * 1.2f else dotR
        drawCircle(c, r, center(i))
        val layout = measurer.measure("${k + 1}", TextStyle(color = Color.Black, fontSize = (r * 1.25f).toSp(), fontWeight = FontWeight.Bold))
        val o = center(i)
        drawText(layout, topLeft = Offset(o.x - layout.size.width / 2f, o.y - layout.size.height / 2f))
    }
}
