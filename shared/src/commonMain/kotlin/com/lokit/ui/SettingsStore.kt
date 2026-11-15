package com.lokit.ui

/** Almacenamiento clave-valor local. Android: SharedPreferences. iOS: NSUserDefaults (ver las `actual`). */
interface KeyValueStore {
    fun getString(key: String): String?
    fun getBoolean(key: String, default: Boolean): Boolean
    fun getLong(key: String, default: Long): Long
    fun putString(key: String, value: String)
    fun putBoolean(key: String, value: Boolean)
    fun putLong(key: String, value: Long)
}

/** Ficheros internos (las capturas). Android: filesDir. iOS: Documents. */
interface BlobStore {
    fun read(name: String): ByteArray?
    fun write(name: String, bytes: ByteArray)
    fun delete(name: String)
}

class SettingsStore(private val prefs: KeyValueStore) {
    private fun float(key: String, default: Float, lo: Float, hi: Float) =
        (prefs.getString(key)?.toFloatOrNull() ?: default).coerceIn(lo, hi)

    private fun int(key: String, default: Int, lo: Int, hi: Int) = prefs.getLong(key, default.toLong()).toInt().coerceIn(lo, hi)

    fun load(): AppSettings {
        val overrides = HashMap<String, Zone?>()
        val masks = HashMap<String, MaskStyle>()
        for (spec in Specs.all) {
            val raw = prefs.getString("box_${spec.key}")
            if (raw != null && raw != DEFAULT_MARK) {
                if (raw.isEmpty()) overrides[spec.key] = null else Zone.decode(raw)?.let { overrides[spec.key] = it }
            }
            MaskStyle.decode(prefs.getString("mask_${spec.key}"))?.let { masks[spec.key] = it }
        }
        val screenMasks = HashMap<ScreenId, List<MaskItem>>()
        for (id in ScreenId.entries) {
            val raw = prefs.getString("masks_${id.name}")
            val list = if (raw != null) {
                raw.split(";").filter { it.isNotBlank() }.mapNotNull { MaskItem.decode(it) }
            } else legacyMasks(id)
            if (list.isNotEmpty()) screenMasks[id] = list
        }
        val d = AppSettings()
        val style = FeedbackStyle.entries.firstOrNull { it.name == prefs.getString("fbStyle") } ?: d.fbStyle
        return AppSettings(
            pinLength = int("pinLength", d.pinLength, 3, 10),
            overrides = overrides,
            maskOverrides = masks,
            masks = screenMasks,
            fbStyle = style,
            fbLight = prefs.getBoolean("fbLight", d.fbLight),
            fbTint = prefs.getLong("fbTint", d.fbTint),
            fbIntensity = float("fbIntensity", d.fbIntensity, 0.05f, 0.9f),
            dotSize = float("dotSize", d.dotSize, 0.2f, 1.5f),
            dotGap = float("dotGap", d.dotGap, 0f, 3f),
            dotBorder = float("dotBorder", d.dotBorder, 0.05f, 1f),
            revealBgAlpha = float("revealBgAlpha", d.revealBgAlpha, 0f, 1f),
            revealColor = prefs.getLong("revealColor", d.revealColor),
            dimEnabled = prefs.getBoolean("dimEnabled", d.dimEnabled),
            dimSeconds = int("dimSeconds", d.dimSeconds, 1, 120),
            dimLevel = float("dimLevel", d.dimLevel, 0f, 0.95f),
            dimBlur = int("dimBlur", d.dimBlur, 0, 40),
            settingsHoldMs = int("settingsHoldMs", d.settingsHoldMs, 200, 5000),
            resetHoldMs = int("resetHoldMs", d.resetHoldMs, 200, 5000),
        )
    }

    fun save(s: AppSettings) {
        prefs.putLong("pinLength", s.pinLength.toLong())
        prefs.putString("fbStyle", s.fbStyle.name)
        prefs.putBoolean("fbLight", s.fbLight)
        prefs.putLong("fbTint", s.fbTint)
        prefs.putString("fbIntensity", s.fbIntensity.toString())
        prefs.putString("dotSize", s.dotSize.toString())
        prefs.putString("dotGap", s.dotGap.toString())
        prefs.putString("dotBorder", s.dotBorder.toString())
        prefs.putString("revealBgAlpha", s.revealBgAlpha.toString())
        prefs.putLong("revealColor", s.revealColor)
        prefs.putBoolean("dimEnabled", s.dimEnabled)
        prefs.putLong("dimSeconds", s.dimSeconds.toLong())
        prefs.putString("dimLevel", s.dimLevel.toString())
        prefs.putLong("dimBlur", s.dimBlur.toLong())
        prefs.putLong("settingsHoldMs", s.settingsHoldMs.toLong())
        prefs.putLong("resetHoldMs", s.resetHoldMs.toLong())
        for (id in ScreenId.entries) prefs.putString("masks_${id.name}", s.masksOf(id).joinToString(";") { it.encode() })
        for (spec in Specs.all) {
            val raw = if (s.overrides.containsKey(spec.key)) (s.overrides[spec.key]?.encode() ?: "") else DEFAULT_MARK
            prefs.putString("box_${spec.key}", raw)
            prefs.putString("mask_${spec.key}", s.maskOverrides[spec.key]?.encode() ?: DEFAULT_MARK)
        }
    }

    fun loadEffect() = EffectState(prefs.getLong("stage", 0L).toInt(), prefs.getString("firstPin") ?: "")

    fun saveEffect(e: EffectState) {
        prefs.putLong("stage", e.stage.toLong())
        prefs.putString("firstPin", e.first)
    }

    /** Máscaras fijas de versiones anteriores («lockMask1»…): se convierten en máscaras libres. */
    private fun legacyMasks(id: ScreenId): List<MaskItem> {
        val prefix = id.name.lowercase()
        val kinds = when (id) {
            ScreenId.LOCK -> listOf(MaskKind.TIME_STACKED, MaskKind.DATE, MaskKind.TIME_NOZERO)
            ScreenId.PAD -> listOf(MaskKind.TIME_NOZERO, MaskKind.TIME, MaskKind.DATE)
            ScreenId.HOME -> listOf(MaskKind.DATE, MaskKind.TIME_NOZERO, MaskKind.TIME)
        }
        return kinds.mapIndexedNotNull { i, kind ->
            val zone = Zone.decode(prefs.getString("box_${prefix}Mask${i + 1}")) ?: return@mapIndexedNotNull null
            val style = MaskStyle.decode(prefs.getString("mask_${prefix}Mask${i + 1}"))
                ?: MaskStyle(kind = kind, size = kind.defaultSize)
            MaskItem(i + 1, zone, style)
        }
    }

    private companion object {
        const val DEFAULT_MARK = "default"
    }
}
