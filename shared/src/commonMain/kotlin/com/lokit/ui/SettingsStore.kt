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
    fun load(): AppSettings {
        val overrides = HashMap<String, Zone?>()
        for (spec in Specs.all) {
            val raw = prefs.getString("box_${spec.key}") ?: continue
            if (raw == DEFAULT_MARK) continue
            overrides[spec.key] = if (raw.isEmpty()) null else Zone.decode(raw) ?: continue
        }
        val style = FeedbackStyle.entries.firstOrNull { it.name == prefs.getString("fbStyle") } ?: FeedbackStyle.CIRCLE
        return AppSettings(
            pinLength = prefs.getLong("pinLength", 6L).toInt().coerceIn(3, 10),
            overrides = overrides,
            fbStyle = style,
            fbLight = prefs.getBoolean("fbLight", true),
            fbTint = prefs.getLong("fbTint", 0L),
            fbIntensity = (prefs.getString("fbIntensity")?.toFloatOrNull() ?: 0.35f).coerceIn(0.05f, 0.9f),
        )
    }

    fun save(s: AppSettings) {
        prefs.putLong("pinLength", s.pinLength.toLong())
        prefs.putString("fbStyle", s.fbStyle.name)
        prefs.putBoolean("fbLight", s.fbLight)
        prefs.putLong("fbTint", s.fbTint)
        prefs.putString("fbIntensity", s.fbIntensity.toString())
        for (spec in Specs.all) {
            val raw = if (s.overrides.containsKey(spec.key)) (s.overrides[spec.key]?.encode() ?: "") else DEFAULT_MARK
            prefs.putString("box_${spec.key}", raw)
        }
    }

    fun loadEffect() = EffectState(prefs.getLong("stage", 0L).toInt(), prefs.getString("firstPin") ?: "")

    fun saveEffect(e: EffectState) {
        prefs.putLong("stage", e.stage.toLong())
        prefs.putString("firstPin", e.first)
    }

    private companion object {
        const val DEFAULT_MARK = "default"
    }
}
