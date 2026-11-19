package com.lokit.ui

import kotlinx.datetime.LocalDateTime

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
enum class ScreenId(val title: String, val file: String, val cleanFile: String) {
    LOCK("Pantalla de bloqueo", "lock.img", "lock_clean.img"),
    PAD("Teclado de PIN", "pad.img", "pad_clean.img"),
    PATTERN("Pantalla de patrón", "pattern.img", "pattern_clean.img"),
    HOME("Escritorio", "home.img", "home_clean.img"),
}

/** Qué se dibuja en una máscara: una hora o una fecha, con su formato. */
enum class MaskKind(val label: String, val date: Boolean = false) {
    TIME("Hora 24 h (05:53)"),
    TIME_NOZERO("Hora 24 h sin cero (5:53)"),
    TIME_12("Hora 12 h (5:53)"),
    TIME_12_AMPM("Hora 12 h con AM/PM (5:53 PM)"),
    TIME_STACKED("Hora vertical (dos líneas)"),
    DATE("Fecha (dom, 15 nov)", true),
    DATE_SHORT("Fecha corta (15 nov)", true),
    DATE_LONG("Fecha larga (domingo, 15 de noviembre)", true);

    /** Tamaño de letra por defecto, como fracción de la altura de la pantalla. */
    val defaultSize: Float get() = if (this == TIME_STACKED) 0.085f else 0.022f
}

/** Fondo de una máscara: transparente, o el trozo equivalente de la captura «limpia» (sin hora, fecha ni iconos). */
enum class BgMode(val label: String) {
    NONE("Transparente"),
    IMAGE("Imagen de fondo limpia"),
}

/**
 * Aspecto de una máscara. Las de hora/fecha usan todo; la de los puntos del PIN solo usa el fondo.
 * El texto NO depende del tamaño de la caja: `size` es la altura de la letra como fracción de la altura de la
 * pantalla; la caja solo marca dónde se alinea. `align`: 0 izquierda, 1 centro, 2 derecha.
 * `weight`: 0 fina, 1 normal, 2 negrita. `spacing`: separación entre caracteres (em). `font`: 0 sistema, 1 serif, 2 mono, 3 cursiva.
 */
data class MaskStyle(
    val kind: MaskKind = MaskKind.TIME,
    val textColor: Long = 0xFFFFFFFFL,
    val bgMode: BgMode = BgMode.IMAGE,
    val bgColor: Long = 0xFF000000L,
    val size: Float = 0.022f,
    val align: Int = 1,
    val weight: Int = 0,
    val spacing: Float = 0f,
    val font: Int = 0,
) {
    fun encode() = "${kind.name}|$textColor|${bgMode.name}|$bgColor|$size|$align|$weight|$spacing|$font"

    companion object {
        fun decode(s: String?): MaskStyle? {
            val p = s?.split("|") ?: return null
            if (p.size != 6 && p.size != 7 && p.size != 9) return null
            val kind = MaskKind.entries.firstOrNull { it.name == p[0] } ?: return null
            val legacy = p.size != 9
            // Formato antiguo: el cuarto campo era un factor sobre la caja; ahora es un tamaño absoluto.
            val size = if (legacy) kind.defaultSize * (p[4].toFloatOrNull() ?: return null) else p[4].toFloatOrNull() ?: return null
            return MaskStyle(
                kind = kind,
                textColor = p[1].toLongOrNull() ?: return null,
                bgMode = if (p[2] == "NONE") BgMode.NONE else BgMode.IMAGE, // formatos antiguos (auto, degradado, sólido) -> imagen
                bgColor = p[3].toLongOrNull() ?: return null,
                size = size,
                align = p[5].toIntOrNull() ?: return null,
                weight = if (p.size >= 7) p[6].toIntOrNull() ?: return null else 0,
                spacing = if (p.size == 9) p[7].toFloatOrNull() ?: return null else 0f,
                font = if (p.size == 9) p[8].toIntOrNull() ?: return null else 0,
            )
        }
    }
}

/** Una máscara colocada por el usuario en una pantalla (se añaden y se quitan libremente). */
data class MaskItem(val id: Int, val zone: Zone, val style: MaskStyle) {
    fun encode() = "$id#${zone.encode()}#${style.encode()}"

    companion object {
        fun decode(s: String): MaskItem? {
            val p = s.split("#")
            if (p.size != 3) return null
            return MaskItem(p[0].toIntOrNull() ?: return null, Zone.decode(p[1]) ?: return null, MaskStyle.decode(p[2]) ?: return null)
        }
    }
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
    /** No nulo si la caja lleva máscara (fondo y, en hora/fecha, texto). */
    val maskDefault: MaskStyle? = null,
)

/** Resultado de pulsar una tecla. */
sealed interface KeyHit {
    data class Digit(val d: Char) : KeyHit
    data object Delete : KeyHit
    /** Tecla «OK»: ejecuta el intento con lo tecleado. */
    data object Ok : KeyHit
}

object Specs {
    val lockOpen = BoxSpec(
        "lockOpen", "Abrir teclado",
        "Al tocar aquí en la pantalla de bloqueo (ya despierta) aparece el teclado de PIN.",
        Zone(0f, 0f, 1f, 1f), false, "abrir",
    )

    // Rejilla por defecto de 3 columnas × 4 filas: 1-9, [vacío] 0 [borrar].
    private fun gridZone(row: Int, col: Int) = Zone(0.08f + col * 0.28f, 0.45f + row * 0.125f, 0.28f, 0.125f)

    /** Teclas del 0 al 9 y borrar, cada una con su propia caja. */
    val keys: List<BoxSpec> = ('1'..'9').map { d ->
        val i = d - '1'
        BoxSpec("key$d", "Tecla $d", "Caja de la tecla $d.", gridZone(i / 3, i % 3), false, "$d")
    } + BoxSpec("key0", "Tecla 0", "Caja de la tecla 0.", gridZone(3, 1), false, "0") +
        BoxSpec("keyDel", "Borrar", "Caja de la tecla de borrar. Desactívala si no existe en tu teclado.", gridZone(3, 2), true, "⌫") +
        BoxSpec("keyOk", "OK", "Caja de la tecla OK: al pulsarla se ejecuta el intento (si ya hay el mínimo de dígitos).", gridZone(3, 0), false, "OK")

    val padDots = BoxSpec(
        "padDots", "Puntos del PIN",
        "Zona donde se dibujan los puntos al teclear. Si activas la máscara, tapa los puntos o el recuadro que trae la captura.",
        Zone(0.25f, 0.22f, 0.5f, 0.04f), true, "•••",
        maskDefault = MaskStyle(bgMode = BgMode.NONE),
    )
    /** Modo patrón: aquí se dibuja la rejilla de puntos. La máscara tapa la rejilla que trae la captura. */
    val padPattern = BoxSpec(
        "padPattern", "Rejilla del patrón",
        "Zona donde el espectador dibuja el patrón. La caja solo marca el centro y la zona táctil: el tamaño y la forma de la rejilla se ajustan abajo, no con la caja. La máscara tapa la rejilla de la captura (necesita la imagen de fondo limpia de esta pantalla).",
        Zone(0.1f, 0.4f, 0.8f, 0.45f), false, "patrón",
        maskDefault = MaskStyle(bgMode = BgMode.IMAGE),
    )
    val padBack = BoxSpec(
        "padBack", "Atrás / Cancelar",
        "Vuelve a la pantalla de bloqueo (sin reiniciar el efecto).",
        null, true, "atrás",
    )
    val homeReveal = BoxSpec(
        "homeReveal", "Revelar PIN (mantener pulsado)",
        "Zona secreta del escritorio: el PIN se ve mientras la mantienes pulsada y se oculta al soltar.",
        Zone(0.4f, 0.45f, 0.2f, 0.1f), false, "PIN",
    )
    val homeRevealView = BoxSpec(
        "homeRevealView", "Posición del número",
        "Dónde se dibuja el PIN revelado. El texto se ajusta al tamaño de la caja.",
        Zone(0.1f, 0.42f, 0.8f, 0.14f), false, "123456",
    )
    /** Modo patrón: caja donde se enseña, pequeño, el patrón guardado (con líneas, flechas y orden). */
    val homePattern = BoxSpec(
        "homePattern", "Patrón revelado",
        "Dónde se dibuja el patrón guardado mientras mantienes pulsada la zona secreta: puntos numerados por orden, con líneas y flechas de dirección. Se ajusta como un cuadrado dentro de la caja.",
        Zone(0.3f, 0.4f, 0.4f, 0.2f), false, "patrón",
    )
    val homeLock = BoxSpec(
        "homeLock", "Bloquear y reiniciar",
        "Vuelve a la pantalla de bloqueo y reinicia el efecto (borra el PIN guardado).",
        null, true, "bloq.",
    )

    /** Botones ocultos globales (se activan con pulsación larga). */
    val settingsBox = BoxSpec(
        "settingsBox", "Acceso a Ajustes",
        "Pulsación larga aquí en cualquier pantalla abre Ajustes. Por defecto, esquina superior derecha.",
        Zone(0.8f, 0f, 0.2f, 0.07f), false, "ajustes",
    )
    val resetBox = BoxSpec(
        "resetBox", "Reiniciar efecto",
        "Pulsación larga aquí en cualquier pantalla borra el PIN guardado y vuelve a la pantalla de bloqueo. Por defecto, esquina superior izquierda.",
        Zone(0f, 0f, 0.2f, 0.07f), true, "reset",
    )
    val hidden = listOf(settingsBox, resetBox)

    /** Cajas editables de una pantalla. */
    fun forScreen(id: ScreenId): List<BoxSpec> = when (id) {
        ScreenId.LOCK -> listOf(lockOpen)
        ScreenId.PAD -> keys + listOf(padDots, padBack)
        ScreenId.PATTERN -> listOf(padPattern, padBack)
        ScreenId.HOME -> listOf(homeReveal, homeRevealView, homePattern, homeLock)
    }

    val all: List<BoxSpec> = ScreenId.entries.flatMap { forScreen(it) }.distinctBy { it.key } + hidden

    fun hitOf(spec: BoxSpec): KeyHit? = when {
        spec.key == "keyDel" -> KeyHit.Delete
        spec.key == "keyOk" -> KeyHit.Ok
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
 * Ajustes del mago. `overrides` solo guarda las cajas que ha cambiado (null = desactivada);
 * `maskOverrides`, los estilos de máscara cambiados.
 * `fbLight`: resalte claro (para teclados sobre fondo oscuro); si es falso, resalte oscuro (teclado claro).
 */
data class AppSettings(
    val unlockMode: UnlockMode = UnlockMode.PIN,
    // Modo patrón: rejilla de `patternGrid × patternGrid` puntos; `patternMin` puntos mínimos para validar al soltar el dedo.
    val patternGrid: Int = 3,
    val patternMin: Int = 4,
    // Dibujo del patrón (tamaños absolutos, como fracción de la altura de la pantalla; la caja NO los escala):
    // diámetro del punto, separación horizontal y vertical entre puntos y grosor de la línea; colores independientes.
    val patternDot: Float = 0.026f,
    val patternGapX: Float = 0.094f,
    val patternGapY: Float = 0.094f,
    val patternLine: Float = 0.008f,
    val patternDotColor: Long = 0xFFFFFFFFL,
    val patternLineColor: Long = 0xFFFFFFFFL,
    /** Al abrir la app, preguntar primero si se usa código o patrón. */
    val askAtStart: Boolean = true,
    /** Mostrar la guía rápida al abrir la app (activa por defecto: sale la primera vez hasta que se desactiva). */
    val guideAtStart: Boolean = true,
    /** Mínimo de dígitos que hay que teclear para que la tecla OK ejecute el intento. */
    val minDigits: Int = 4,
    /** Máximo de dígitos que se admiten; los que sobren se ignoran. Siempre >= `minDigits`. */
    val maxDigits: Int = 12,
    /** Intentos que tiene el espectador: los anteriores al último fallan y el último desbloquea. */
    val attempts: Int = 2,
    val overrides: Map<String, Zone?> = emptyMap(),
    val maskOverrides: Map<String, MaskStyle> = emptyMap(),
    /** Máscaras de hora/fecha que el usuario añade a cada pantalla. */
    val masks: Map<ScreenId, List<MaskItem>> = emptyMap(),
    val fbStyle: FeedbackStyle = FeedbackStyle.CIRCLE,
    val fbLight: Boolean = true,
    val fbIntensity: Float = 0.35f,
    /** Tinte ARGB del resalte; 0 = automático (blanco o negro según `fbLight`). */
    val fbTint: Long = 0L,
    // Puntos del PIN: diámetro (fracción de la altura de la caja), separación (fracción del diámetro) y borde (fracción del radio).
    val dotSize: Float = 0.72f,
    val dotGap: Float = 0.3f,
    val dotBorder: Float = 0.3f,
    // PIN revelado: opacidad del fondo y color del número.
    val revealBgAlpha: Float = 0.93f,
    val revealColor: Long = 0xFFFFFFFFL,
    // Modo atenuado de la pantalla de bloqueo.
    val dimEnabled: Boolean = false,
    val dimSeconds: Int = 5,
    val dimLevel: Float = 0.55f,
    val dimBlur: Int = 14,
    // Duración de la pulsación larga de los botones ocultos (milisegundos).
    val settingsHoldMs: Int = 800,
    val resetHoldMs: Int = 800,
) {
    fun box(spec: BoxSpec): Zone? = if (overrides.containsKey(spec.key)) overrides[spec.key] else spec.default
    fun withBox(key: String, b: Zone?) = copy(overrides = overrides + (key to b))
    fun resetBox(key: String) = copy(overrides = overrides - key, maskOverrides = maskOverrides - key)
    fun resetBoxes(keys: Collection<String>) = copy(overrides = overrides - keys.toSet(), maskOverrides = maskOverrides - keys.toSet())
    fun mask(spec: BoxSpec): MaskStyle = maskOverrides[spec.key] ?: spec.maskDefault ?: MaskStyle()
    fun withMask(key: String, m: MaskStyle) = copy(maskOverrides = maskOverrides + (key to m))

    fun masksOf(id: ScreenId): List<MaskItem> = masks[id] ?: emptyList()
    fun withMasks(id: ScreenId, list: List<MaskItem>) = copy(masks = masks + (id to list))
    fun addMask(id: ScreenId, zone: Zone, style: MaskStyle): Pair<AppSettings, MaskItem> {
        val item = MaskItem((masksOf(id).maxOfOrNull { it.id } ?: 0) + 1, zone, style)
        return withMasks(id, masksOf(id) + item) to item
    }
    fun updateMask(id: ScreenId, item: MaskItem) = withMasks(id, masksOf(id).map { if (it.id == item.id) item else it })
    fun removeMask(id: ScreenId, maskId: Int) = withMasks(id, masksOf(id).filter { it.id != maskId })
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
 * Estado del efecto. stage 0: nada tecleado aún (el primer PIN se guarda).
 * stage 1: en curso, `count` intentos ya fallados. stage 2: ya desbloqueado (siempre desbloquea).
 * Con `attempts` intentos, los `attempts - 1` primeros fallan y el último desbloquea (con 1, el primero ya desbloquea).
 */
data class EffectState(val stage: Int = 0, val first: String = "", val count: Int = 0) {
    /** Devuelve el nuevo estado y si el PIN desbloquea. */
    fun submit(pin: String, attempts: Int = 2): Pair<EffectState, Boolean> = when {
        stage >= 2 -> EffectState(2, first, count) to true
        stage == 0 -> if (attempts <= 1) EffectState(2, pin, 1) to true else EffectState(1, pin, 1) to false
        count + 1 >= attempts -> EffectState(2, first, count + 1) to true
        else -> EffectState(1, first, count + 1) to false
    }
}

private val dayNames = listOf("lun", "mar", "mié", "jue", "vie", "sáb", "dom")
private val monthNames = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
private val dayLong = listOf("lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo")
private val monthLong = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
)

private fun two(n: Int) = if (n < 10) "0$n" else "$n"

private fun hour12(h: Int) = if (h % 12 == 0) 12 else h % 12

/** Líneas de texto de una máscara para un instante dado (formato 24 h, en español). */
fun maskLines(kind: MaskKind, t: LocalDateTime): List<String> = when (kind) {
    MaskKind.TIME -> listOf("${two(t.hour)}:${two(t.minute)}")
    MaskKind.TIME_NOZERO -> listOf("${t.hour}:${two(t.minute)}")
    MaskKind.TIME_12 -> listOf("${hour12(t.hour)}:${two(t.minute)}")
    MaskKind.TIME_12_AMPM -> listOf("${hour12(t.hour)}:${two(t.minute)} ${if (t.hour < 12) "AM" else "PM"}")
    MaskKind.TIME_STACKED -> listOf(two(t.hour), two(t.minute))
    MaskKind.DATE -> listOf("${dayNames[t.dayOfWeek.ordinal]}, ${t.dayOfMonth} ${monthNames[t.monthNumber - 1]}")
    MaskKind.DATE_SHORT -> listOf("${t.dayOfMonth} ${monthNames[t.monthNumber - 1]}")
    MaskKind.DATE_LONG -> listOf("${dayLong[t.dayOfWeek.ordinal]}, ${t.dayOfMonth} de ${monthLong[t.monthNumber - 1]}")
}

/** Paleta de colores de texto y de fondo para máscaras y PIN revelado. */
object Palette {
    val text = listOf(
        "Blanco" to 0xFFFFFFFFL,
        "Negro" to 0xFF000000L,
        "Verde Pixel" to 0xFFD2EB9BL,
        "Azul claro" to 0xFFA8C7FAL,
        "Gris claro" to 0xFFD0D0D0L,
        "Gris oscuro" to 0xFF404040L,
    )
    val bg = listOf(
        "Negro" to 0xFF000000L,
        "Gris oscuro" to 0xFF2B2B2BL,
        "Gris claro" to 0xFFE8E8E0L,
        "Crema" to 0xFFFAF9EEL,
        "Blanco" to 0xFFFFFFFFL,
        "Azul oscuro" to 0xFF10243EL,
    )
}
