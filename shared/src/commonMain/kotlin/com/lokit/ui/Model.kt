package com.lokit.ui

/** Rectángulo normalizado (0..1) sobre la pantalla: x, y = esquina superior izquierda; w, h = tamaño. */
data class Zone(val x: Float, val y: Float, val w: Float, val h: Float) {
    fun contains(px: Float, py: Float) = px >= x && px <= x + w && py >= y && py <= y + h
    fun encode() = "$x,$y,$w,$h"

    companion object {
        fun decode(s: String?): Zone? {
            val p = s?.split(",") ?: return null
            if (p.size != 4) return null
            val f = p.map { it.toFloatOrNull() ?: return null }
            return Zone(f[0], f[1], f[2], f[3])
        }
    }
}

/** Las tres pantallas que imita la app. Cada una lleva una captura (fichero `file`). */
enum class ScreenId(val title: String, val file: String) {
    LOCK("Pantalla de bloqueo", "lock.img"),
    PAD("Teclado de PIN", "pad.img"),
    HOME("Escritorio", "home.img"),
}

/** Una caja invisible configurable. `default == null` significa que está desactivada por defecto. */
class BoxSpec(
    val key: String,
    val label: String,
    val help: String,
    val default: Zone?,
    val optional: Boolean,
    /** Texto corto que se dibuja dentro de la caja en el editor. */
    val tag: String = "",
)

/** Resultado de pulsar una tecla. */
sealed interface KeyHit {
    data class Digit(val d: Char) : KeyHit
    data object Delete : KeyHit
}

object Specs {
    val lockOpen = BoxSpec(
        "lockOpen", "Abrir teclado",
        "Al tocar aquí en la pantalla de bloqueo aparece el teclado de PIN.",
        Zone(0f, 0f, 1f, 1f), false, "abrir",
    )

    // Rejilla por defecto de 3 columnas × 4 filas: 1-9, [vacío] 0 [borrar].
    private fun gridZone(row: Int, col: Int) = Zone(0.08f + col * 0.28f, 0.45f + row * 0.125f, 0.28f, 0.125f)

    /** Teclas del 0 al 9 y borrar, cada una con su propia caja. */
    val keys: List<BoxSpec> = ('1'..'9').map { d ->
        val i = d - '1'
        BoxSpec("key$d", "Tecla $d", "Caja de la tecla $d.", gridZone(i / 3, i % 3), false, "$d")
    } + BoxSpec("key0", "Tecla 0", "Caja de la tecla 0.", gridZone(3, 1), false, "0") +
        BoxSpec("keyDel", "Borrar", "Caja de la tecla de borrar. Desactívala si no existe en tu teclado.", gridZone(3, 2), true, "⌫")

    val padDots = BoxSpec(
        "padDots", "Puntos del PIN",
        "Zona donde se dibujan los puntos al teclear. Desactívala si tu captura ya los muestra.",
        Zone(0.25f, 0.22f, 0.5f, 0.04f), true, "•••",
    )
    val padBack = BoxSpec(
        "padBack", "Atrás / Cancelar",
        "Vuelve a la pantalla de bloqueo.",
        null, true, "atrás",
    )
    val homeReveal = BoxSpec(
        "homeReveal", "Revelar PIN",
        "Zona secreta del escritorio: al tocarla se muestra el número que tecleó el espectador.",
        Zone(0.4f, 0.45f, 0.2f, 0.1f), false, "PIN",
    )
    val homeLock = BoxSpec(
        "homeLock", "Bloquear",
        "Vuelve a la pantalla de bloqueo (el PIN guardado se conserva).",
        null, true, "bloq.",
    )

    fun forScreen(id: ScreenId): List<BoxSpec> = when (id) {
        ScreenId.LOCK -> listOf(lockOpen)
        ScreenId.PAD -> keys + listOf(padDots, padBack)
        ScreenId.HOME -> listOf(homeReveal, homeLock)
    }

    val all: List<BoxSpec> = ScreenId.entries.flatMap { forScreen(it) }

    fun hitOf(spec: BoxSpec): KeyHit? = when {
        spec.key == "keyDel" -> KeyHit.Delete
        spec.key.startsWith("key") -> KeyHit.Digit(spec.key.last())
        else -> null
    }
}

/** Aspecto del resalte al pulsar una tecla. */
enum class FeedbackStyle(val label: String) {
    NONE("Ninguno"),
    CIRCLE("Círculo (estilo iPhone)"),
    PILL("Píldora"),
    SQUARE("Cuadrado redondeado (estilo Pixel)"),
    RIPPLE("Onda (estilo Android clásico / Samsung)"),
}

/**
 * Ajustes del mago. `overrides` solo guarda las cajas que ha cambiado (null = desactivada).
 * `fbLight`: resalte claro (para teclados sobre fondo oscuro); si es falso, resalte oscuro (teclado claro).
 */
data class AppSettings(
    val pinLength: Int = 6,
    val overrides: Map<String, Zone?> = emptyMap(),
    val fbStyle: FeedbackStyle = FeedbackStyle.CIRCLE,
    val fbLight: Boolean = true,
    val fbIntensity: Float = 0.35f,
    /** Tinte ARGB del resalte; 0 = automático (blanco o negro según `fbLight`). */
    val fbTint: Long = 0L,
) {
    fun box(spec: BoxSpec): Zone? = if (overrides.containsKey(spec.key)) overrides[spec.key] else spec.default
    fun withBox(key: String, b: Zone?) = copy(overrides = overrides + (key to b))
    fun resetBox(key: String) = copy(overrides = overrides - key)
    fun resetBoxes(keys: Collection<String>) = copy(overrides = overrides - keys.toSet())
}

/** Qué tecla (y su caja) hay bajo un toque normalizado, o null. */
fun hitKey(settings: AppSettings, px: Float, py: Float): Pair<KeyHit, Zone>? {
    for (spec in Specs.keys) {
        val z = settings.box(spec) ?: continue
        if (z.contains(px, py)) return Specs.hitOf(spec)!! to z
    }
    return null
}

/**
 * Estado del efecto. stage 0: nada tecleado aún (el primer PIN falla y se guarda).
 * stage 1: el siguiente PIN, sea cual sea, desbloquea. stage 2: ya desbloqueado (siempre desbloquea).
 */
data class EffectState(val stage: Int = 0, val first: String = "") {
    /** Devuelve el nuevo estado y si el PIN desbloquea. */
    fun submit(pin: String): Pair<EffectState, Boolean> =
        if (stage == 0) EffectState(1, pin) to false else EffectState(2, first) to true
}
