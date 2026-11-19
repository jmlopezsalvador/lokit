package com.lokit.ui

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Cómo se desbloquea el móvil falso. */
enum class UnlockMode(val label: String) {
    PIN("PIN numérico"),
    PATTERN("Patrón"),
}

/**
 * Lógica pura del patrón de desbloqueo: una rejilla de `n × n` puntos numerados por filas (0 = arriba a la izquierda).
 * Un patrón es la lista ordenada de puntos que se recorren. Se guarda como texto `n:p1,p2,…` (en `EffectState.first`).
 */
object Pattern {
    fun encode(n: Int, nodes: List<Int>) = "$n:" + nodes.joinToString(",")

    /** (tamaño de la rejilla, puntos) o null si el texto no es un patrón. */
    fun decode(s: String?): Pair<Int, List<Int>>? {
        val parts = s?.split(":") ?: return null
        if (parts.size != 2) return null
        val n = parts[0].toIntOrNull() ?: return null
        if (n < 2) return null
        val nodes = parts[1].split(",").map { it.toIntOrNull() ?: return null }
        if (nodes.isEmpty() || nodes.any { it < 0 || it >= n * n }) return null
        return n to nodes
    }

    private fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

    /**
     * Añade `node` al camino. Como en Android, si el trazo pasa justo por encima de puntos aún sin visitar
     * (p. ej. de 0 a 2 pasa por el 1), esos puntos intermedios se incluyen antes. Un punto ya visitado se ignora.
     */
    fun extend(path: List<Int>, node: Int, n: Int): List<Int> {
        if (node in path) return path
        if (path.isEmpty()) return listOf(node)
        val last = path.last()
        val dr = node / n - last / n
        val dc = node % n - last % n
        val g = gcd(abs(dr), abs(dc))
        val out = path.toMutableList()
        for (k in 1 until g) {
            val mid = (last / n + k * (dr / g)) * n + (last % n + k * (dc / g))
            if (mid !in out) out += mid
        }
        out += node
        return out
    }

}

/**
 * Posición real de los puntos del patrón, en píxeles. El tamaño NO depende de la forma de la caja: sale de los ajustes
 * (diámetro del punto y separaciones, como fracción de la altura de la pantalla); la caja solo marca el centro.
 */
class PatternGeometry(
    val n: Int,
    private val originX: Float,
    private val originY: Float,
    val stepX: Float,
    val stepY: Float,
    val dot: Float,
) {
    fun cx(i: Int) = originX + (i % n) * stepX
    fun cy(i: Int) = originY + (i / n) * stepY

    /** Punto más cercano a (`px`, `py`) dentro de su radio de acierto, o -1. */
    fun nodeAt(px: Float, py: Float): Int {
        val step = min(stepX, stepY)
        val reach = min(max(dot * 0.9f, step * 0.3f), step * 0.5f)
        var best = -1
        var bestD = reach * reach
        for (i in 0 until n * n) {
            val dx = px - cx(i)
            val dy = py - cy(i)
            val d = dx * dx + dy * dy
            if (d <= bestD) { best = i; bestD = d }
        }
        return best
    }
}

/** Geometría del patrón centrada en la caja `zone` de una pantalla de `w × h` píxeles. */
fun patternGeometry(s: AppSettings, zone: Zone, w: Float, h: Float): PatternGeometry {
    val n = s.patternGrid
    val dot = s.patternDot * h
    val stepX = dot + s.patternGapX * h
    val stepY = dot + s.patternGapY * h
    val centerX = (zone.x + zone.w / 2f) * w
    val centerY = (zone.y + zone.h / 2f) * h
    return PatternGeometry(n, centerX - (n - 1) * stepX / 2f, centerY - (n - 1) * stepY / 2f, stepX, stepY, dot)
}
