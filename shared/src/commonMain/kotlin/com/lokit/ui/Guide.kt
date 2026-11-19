package com.lokit.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Qué ilustración acompaña a cada paso de la guía (dibujadas con Canvas, valen para Android e iOS). */
private enum class Art { WELCOME, CAPTURES, SETTINGS, EFFECT, REVEAL, DONE }

private class GuideStep(val title: String, val text: String, val art: Art)

private val guideSteps = listOf(
    GuideStep(
        "Bienvenido a Lokit",
        "Lokit imita el móvil del mago —pantalla de bloqueo, teclado y escritorio— usando capturas de tu propio teléfono. " +
            "El espectador teclea un número (o dibuja un patrón) y, sin que nadie lo note, tú acabas sabiéndolo.",
        Art.WELCOME,
    ),
    GuideStep(
        "1. Prepara las capturas",
        "En Ajustes carga una captura de cada pantalla: bloqueo, teclado de PIN, pantalla de patrón y escritorio. " +
            "Para tapar la hora, la fecha o la rejilla original puedes cargar además un «Fondo limpio»: la misma captura sin esos elementos.",
        Art.CAPTURES,
    ),
    GuideStep(
        "2. Cómo entrar en Ajustes",
        "Mantén pulsada la esquina superior derecha de cualquier pantalla (por defecto, 0,8 segundos) y notarás una doble vibración. " +
            "La esquina superior izquierda, con la misma pulsación larga, reinicia el efecto y borra lo guardado.",
        Art.SETTINGS,
    ),
    GuideStep(
        "3. Durante el efecto",
        "El espectador toca la pantalla de bloqueo, teclea su número y pulsa OK (o dibuja su patrón). " +
            "El primer intento se guarda en secreto. Los intentos anteriores al último fallan con una vibración y el último desbloquea y lleva al escritorio. " +
            "Cuántos intentos son lo decides en Ajustes.",
        Art.EFFECT,
    ),
    GuideStep(
        "4. Descubre el número",
        "En el escritorio, mantén pulsada la zona secreta (por defecto, el centro de la pantalla): mientras la mantengas aparece el número, " +
            "o el patrón dibujado con números y flechas. Al soltar, desaparece.",
        Art.REVEAL,
    ),
    GuideStep(
        "Todo es configurable",
        "Mueve y cambia el tamaño de las cajas invisibles, elige PIN o patrón, ajusta intentos, colores, vibraciones y el modo atenuado. " +
            "En Ajustes → «Instrucciones» tienes cada opción explicada con detalle, y puedes volver a abrir esta guía cuando quieras.",
        Art.DONE,
    ),
)

/**
 * Guía rápida a pantalla completa por pasos. `showAtStart` es el valor actual de «mostrar al abrir»; al cerrar se devuelve
 * el valor que el usuario deja marcado en el último paso.
 */
@Composable
fun GuideDialog(showAtStart: Boolean, onClose: (showAtStart: Boolean) -> Unit) {
    var index by remember { mutableStateOf(0) }
    var showAgain by remember { mutableStateOf(showAtStart) }
    val step = guideSteps[index]
    val last = index == guideSteps.lastIndex

    Dialog(
        onDismissRequest = { onClose(showAgain) },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Card(Modifier.fillMaxWidth(0.94f)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Guía rápida · ${index + 1} de ${guideSteps.size}", fontSize = 12.sp, color = Color(0xFF9AA4B2))
                    TextButton(onClick = { onClose(showAgain) }) { Text("Cerrar") }
                }
                GuideArt(step.art, Modifier.fillMaxWidth().height(210.dp))
                Column(Modifier.heightIn(max = 230.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(step.title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text(step.text, fontSize = 14.sp)
                }
                if (last) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Switch(checked = showAgain, onCheckedChange = { showAgain = it })
                        Text("Mostrar esta guía al abrir la app", fontSize = 13.sp)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { index-- }, enabled = index > 0, modifier = Modifier.weight(1f)) { Text("Anterior") }
                    if (last) {
                        Button(onClick = { onClose(showAgain) }, modifier = Modifier.weight(1f)) { Text("Entendido") }
                    } else {
                        Button(onClick = { index++ }, modifier = Modifier.weight(1f)) { Text("Siguiente") }
                    }
                }
            }
        }
    }
}

private val accent = Color(0xFF6EE7B7)
private val dim = Color(0xFF9AA4B2)
private val body = Color(0xFF151A22)

/** Dibuja un móvil y devuelve su pantalla como (izquierda, arriba, ancho, alto). */
private fun DrawScope.phone(left: Float, top: Float, w: Float, h: Float): FloatArray {
    val radius = CornerRadius(w * 0.13f)
    drawRoundRect(body, Offset(left, top), Size(w, h), radius)
    drawRoundRect(dim, Offset(left, top), Size(w, h), radius, style = Stroke(w * 0.035f))
    return floatArrayOf(left + w * 0.06f, top + w * 0.1f, w * 0.88f, h - w * 0.2f)
}

private fun DrawScope.label(measurer: TextMeasurer, text: String, x: Float, y: Float, color: Color, centered: Boolean = false, rightAligned: Boolean = false) {
    val layout = measurer.measure(text, TextStyle(color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium))
    val dx = if (centered) layout.size.width / 2f else if (rightAligned) layout.size.width.toFloat() else 0f
    drawText(layout, topLeft = Offset(x - dx, y - layout.size.height / 2f))
}

private fun DrawScope.sparkle(c: Offset, r: Float) {
    drawLine(accent, Offset(c.x - r, c.y), Offset(c.x + r, c.y), r * 0.3f, StrokeCap.Round)
    drawLine(accent, Offset(c.x, c.y - r), Offset(c.x, c.y + r), r * 0.3f, StrokeCap.Round)
}

private fun DrawScope.lockIcon(cx: Float, cy: Float, s: Float, color: Color) {
    drawArc(color, 180f, 180f, false, Offset(cx - s * 0.32f, cy - s * 0.75f), Size(s * 0.64f, s * 0.8f), style = Stroke(s * 0.12f))
    drawRoundRect(color, Offset(cx - s * 0.5f, cy - s * 0.25f), Size(s, s * 0.75f), CornerRadius(s * 0.14f))
}

/** Teclado de 3×4 puntos; `okAccent` resalta la tecla de abajo a la izquierda como «OK». */
private fun DrawScope.keypad(scr: FloatArray, topFrac: Float, okAccent: Boolean, measurer: TextMeasurer?) {
    val cw = scr[2] / 3f
    val ch = scr[3] * (1f - topFrac) / 4.4f
    for (row in 0 until 4) for (col in 0 until 3) {
        val c = Offset(scr[0] + cw * (col + 0.5f), scr[1] + scr[3] * topFrac + ch * (row + 0.5f))
        val isOk = okAccent && row == 3 && col == 0
        drawCircle(if (isOk) accent else dim.copy(alpha = 0.35f), minOf(cw, ch) * 0.36f, c)
        if (isOk && measurer != null) {
            val layout = measurer.measure("OK", TextStyle(color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold))
            drawText(layout, topLeft = Offset(c.x - layout.size.width / 2f, c.y - layout.size.height / 2f))
        }
    }
}

private fun DrawScope.appGrid(scr: FloatArray, topFrac: Float) {
    val cols = 4
    val cw = scr[2] / cols
    for (row in 0 until 4) for (col in 0 until cols) {
        val s = cw * 0.55f
        drawRoundRect(
            dim.copy(alpha = 0.3f),
            Offset(scr[0] + cw * col + (cw - s) / 2f, scr[1] + scr[3] * topFrac + row * cw * 1.05f),
            Size(s, s), CornerRadius(s * 0.3f),
        )
    }
}

@Composable
private fun GuideArt(kind: Art, modifier: Modifier) {
    val measurer = rememberTextMeasurer()
    val transition = rememberInfiniteTransition(label = "pulso")
    val pulse by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1300), RepeatMode.Restart), label = "p")

    Canvas(modifier) {
        val ph = size.height * 0.94f
        val pw = ph * 0.47f
        val left = (size.width - pw) / 2f
        val top = (size.height - ph) / 2f
        val cy = size.height / 2f
        when (kind) {
            Art.WELCOME -> {
                val scr = phone(left, top, pw, ph)
                drawRoundRect(dim.copy(alpha = 0.6f), Offset(scr[0] + scr[2] * 0.25f, scr[1] + scr[3] * 0.1f), Size(scr[2] * 0.5f, scr[3] * 0.06f), CornerRadius(8f))
                lockIcon(scr[0] + scr[2] / 2f, scr[1] + scr[3] * 0.38f, scr[2] * 0.22f, accent)
                for (i in 0 until 4) drawCircle(Color.White, scr[2] * 0.04f, Offset(scr[0] + scr[2] * (0.31f + i * 0.127f), scr[1] + scr[3] * 0.55f))
                sparkle(Offset(left - pw * 0.55f, cy - ph * 0.22f), 14f)
                sparkle(Offset(left + pw * 1.55f, cy - ph * 0.05f), 10f)
                sparkle(Offset(left - pw * 0.8f, cy + ph * 0.2f), 8f)
                sparkle(Offset(left + pw * 1.8f, cy + ph * 0.28f), 14f)
            }
            Art.CAPTURES -> {
                val h = size.height * 0.78f
                val w = h * 0.47f
                val gap = w * 0.45f
                val total = w * 3 + gap * 2
                val x0 = (size.width - total) / 2f
                val y0 = size.height * 0.04f
                val names = listOf("Bloqueo", "Teclado", "Escritorio")
                for (i in 0 until 3) {
                    val x = x0 + i * (w + gap)
                    val scr = phone(x, y0, w, h)
                    when (i) {
                        0 -> {
                            drawRoundRect(dim.copy(alpha = 0.6f), Offset(scr[0] + scr[2] * 0.2f, scr[1] + scr[3] * 0.12f), Size(scr[2] * 0.6f, scr[3] * 0.07f), CornerRadius(6f))
                            lockIcon(scr[0] + scr[2] / 2f, scr[1] + scr[3] * 0.8f, scr[2] * 0.16f, dim)
                        }
                        1 -> {
                            for (d in 0 until 4) drawCircle(Color.White, scr[2] * 0.035f, Offset(scr[0] + scr[2] * (0.33f + d * 0.113f), scr[1] + scr[3] * 0.15f))
                            keypad(scr, 0.25f, false, null)
                        }
                        else -> appGrid(scr, 0.28f)
                    }
                    label(measurer, names[i], x + w / 2f, y0 + h + size.height * 0.08f, accent, centered = true)
                }
            }
            Art.SETTINGS -> {
                val scr = phone(left, top, pw, ph)
                appGrid(scr, 0.3f)
                val zw = scr[2] * 0.3f
                val zh = scr[3] * 0.07f
                val right = Offset(scr[0] + scr[2] - zw / 2f, scr[1] + zh / 2f)
                val leftZ = Offset(scr[0] + zw / 2f, scr[1] + zh / 2f)
                drawRoundRect(accent.copy(alpha = 0.4f), Offset(right.x - zw / 2f, scr[1]), Size(zw, zh), CornerRadius(6f))
                drawRoundRect(dim.copy(alpha = 0.3f), Offset(leftZ.x - zw / 2f, scr[1]), Size(zw, zh), CornerRadius(6f))
                drawCircle(accent.copy(alpha = 1f - pulse), zh * (0.8f + 1.8f * pulse), right, style = Stroke(3f))
                drawCircle(accent, zh * 0.45f, right)
                label(measurer, "Ajustes\npulsación larga", left + pw + 14f, scr[1] + zh * 0.9f, accent)
                label(measurer, "Reiniciar\npulsación larga", left - 14f, scr[1] + zh * 0.9f, dim, rightAligned = true)
            }
            Art.EFFECT -> {
                val scr = phone(left, top, pw, ph)
                for (d in 0 until 4) drawCircle(Color.White, scr[2] * 0.045f, Offset(scr[0] + scr[2] * (0.31f + d * 0.127f), scr[1] + scr[3] * 0.17f))
                keypad(scr, 0.27f, true, measurer)
                label(measurer, "Teclea y\npulsa OK", left + pw + 14f, scr[1] + scr[3] * 0.62f, accent)
                label(measurer, "1.º intento:\nse guarda", left - 14f, scr[1] + scr[3] * 0.17f, dim, rightAligned = true)
                label(measurer, "Último intento:\ndesbloquea", left - 14f, scr[1] + scr[3] * 0.62f, dim, rightAligned = true)
            }
            Art.REVEAL -> {
                val scr = phone(left, top, pw, ph)
                appGrid(scr, 0.3f)
                val c = Offset(scr[0] + scr[2] / 2f, scr[1] + scr[3] * 0.5f)
                drawRoundRect(Color.Black.copy(alpha = 0.85f), Offset(scr[0] + scr[2] * 0.1f, scr[1] + scr[3] * 0.3f), Size(scr[2] * 0.8f, scr[3] * 0.16f), CornerRadius(8f))
                label(measurer, "4 2 7 1", c.x, scr[1] + scr[3] * 0.38f, accent, centered = true)
                drawCircle(accent.copy(alpha = 1f - pulse), scr[2] * (0.12f + 0.25f * pulse), c, style = Stroke(3f))
                drawCircle(accent, scr[2] * 0.07f, c)
                label(measurer, "Mantén pulsado:\nse ve el número", left + pw + 14f, c.y, accent)
                label(measurer, "Suelta:\ndesaparece", left - 14f, c.y, dim, rightAligned = true)
            }
            Art.DONE -> {
                val c = Offset(size.width / 2f, cy)
                val r = size.height * 0.34f
                drawCircle(accent, r, c)
                drawLine(Color.Black, Offset(c.x - r * 0.45f, c.y + r * 0.02f), Offset(c.x - r * 0.1f, c.y + r * 0.37f), r * 0.14f, StrokeCap.Round)
                drawLine(Color.Black, Offset(c.x - r * 0.1f, c.y + r * 0.37f), Offset(c.x + r * 0.5f, c.y - r * 0.33f), r * 0.14f, StrokeCap.Round)
                sparkle(Offset(c.x - r * 1.5f, c.y - r * 0.6f), 14f)
                sparkle(Offset(c.x + r * 1.6f, c.y - r * 0.2f), 10f)
                sparkle(Offset(c.x + r * 1.3f, c.y + r * 0.7f), 14f)
            }
        }
    }
}
