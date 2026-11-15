package com.lokit

import com.lokit.ui.AppSettings
import com.lokit.ui.EffectState
import com.lokit.ui.KeyHit
import com.lokit.ui.KeyValueStore
import com.lokit.ui.SettingsStore
import com.lokit.ui.Specs
import com.lokit.ui.Zone
import com.lokit.ui.FeedbackStyle
import com.lokit.ui.BgMode
import com.lokit.ui.MaskKind
import com.lokit.ui.MaskStyle
import com.lokit.ui.maskLines
import com.lokit.ui.hitKey
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsStoreTest {
    private class MemoryStore : KeyValueStore {
        val m = HashMap<String, Any>()
        override fun getString(key: String) = m[key] as String?
        override fun getBoolean(key: String, default: Boolean) = m[key] as Boolean? ?: default
        override fun getLong(key: String, default: Long) = m[key] as Long? ?: default
        override fun putString(key: String, value: String) { m[key] = value }
        override fun putBoolean(key: String, value: Boolean) { m[key] = value }
        override fun putLong(key: String, value: Long) { m[key] = value }
    }

    @Test fun defaultsWhenEmpty() {
        val s = SettingsStore(MemoryStore()).load()
        assertEquals(6, s.pinLength)
        assertEquals(Specs.keys.first { it.key == "key5" }.default, s.box(Specs.keys.first { it.key == "key5" }))
        assertNull(s.box(Specs.padBack))
    }

    @Test fun savesAndLoadsBoxesAndPin() {
        val store = SettingsStore(MemoryStore())
        val z = Zone(0.1f, 0.2f, 0.3f, 0.4f)
        val s = AppSettings(pinLength = 4)
            .withBox(Specs.keys.first { it.key == "key5" }.key, z)
            .withBox(Specs.padDots.key, null)
            .withBox(Specs.padBack.key, Zone(0f, 0f, 0.1f, 0.1f))
        store.save(s)
        val back = store.load()
        assertEquals(4, back.pinLength)
        assertEquals(z, back.box(Specs.keys.first { it.key == "key5" }))
        assertNull(back.box(Specs.padDots))
        assertEquals(Zone(0f, 0f, 0.1f, 0.1f), back.box(Specs.padBack))
        // Restablecer vuelve al valor por defecto
        store.save(back.resetBox(Specs.padDots.key))
        assertEquals(Specs.padDots.default, store.load().box(Specs.padDots))
    }

    @Test fun effectFlow() {
        val e0 = EffectState()
        val (e1, ok1) = e0.submit("123456")
        assertFalse(ok1)
        assertEquals("123456", e1.first)
        val (e2, ok2) = e1.submit("999999")
        assertTrue(ok2)
        assertEquals("123456", e2.first) // el segundo PIN no sobrescribe
        val (e3, ok3) = e2.submit("000000")
        assertTrue(ok3)
        assertEquals("123456", e3.first)
    }

    @Test fun effectPersists() {
        val store = SettingsStore(MemoryStore())
        store.saveEffect(EffectState(1, "4321"))
        assertEquals(EffectState(1, "4321"), store.loadEffect())
    }

    @Test fun keypadDefaultGrid() {
        val st = AppSettings()
        assertEquals(KeyHit.Digit('1'), hitKey(st, 0.22f, 0.51f)?.first)
        assertEquals(KeyHit.Digit('5'), hitKey(st, 0.5f, 0.6375f)?.first)
        assertEquals(KeyHit.Digit('9'), hitKey(st, 0.78f, 0.76f)?.first)
        assertEquals(KeyHit.Digit('0'), hitKey(st, 0.5f, 0.89f)?.first)
        assertEquals(KeyHit.Delete, hitKey(st, 0.78f, 0.89f)?.first)
        assertNull(hitKey(st, 0.22f, 0.89f))
        assertNull(hitKey(st, 0.5f, 0.1f))
    }

    @Test fun customKeyBoxesAndDisabledDelete() {
        val st = AppSettings()
            .withBox("key7", Zone(0f, 0f, 0.1f, 0.1f))
            .withBox("keyDel", null)
        assertEquals(KeyHit.Digit('7'), hitKey(st, 0.05f, 0.05f)?.first)
        assertNull(hitKey(st, 0.78f, 0.89f))
    }

    @Test fun feedbackSettingsPersist() {
        val store = SettingsStore(MemoryStore())
        store.save(AppSettings(fbStyle = FeedbackStyle.PILL, fbLight = false, fbIntensity = 0.5f))
        val b = store.load()
        assertEquals(FeedbackStyle.PILL, b.fbStyle)
        assertFalse(b.fbLight)
        assertEquals(0.5f, b.fbIntensity)
    }

    @Test fun maskLinesFormatSpanish() {
        val t = LocalDateTime(2026, 11, 15, 5, 3, 0, 0) // domingo
        assertEquals(listOf("05:03"), maskLines(MaskKind.TIME, t))
        assertEquals(listOf("05", "03"), maskLines(MaskKind.TIME_STACKED, t))
        assertEquals(listOf("dom, 15 nov"), maskLines(MaskKind.DATE, t))
        assertEquals(listOf("5:03"), maskLines(MaskKind.TIME_NOZERO, t))
        assertEquals(listOf("5:03"), maskLines(MaskKind.TIME_12, t))
        assertEquals(listOf("lun, 2 mar"), maskLines(MaskKind.DATE, LocalDateTime(2026, 3, 2, 23, 59, 0, 0)))
    }

    @Test fun hour12Formats() {
        val tarde = LocalDateTime(2026, 11, 15, 17, 5, 0, 0)
        assertEquals(listOf("17:05"), maskLines(MaskKind.TIME_NOZERO, tarde))
        assertEquals(listOf("5:05"), maskLines(MaskKind.TIME_12, tarde))
        assertEquals(listOf("5:05 PM"), maskLines(MaskKind.TIME_12_AMPM, tarde))
        assertEquals(listOf("12:00 AM"), maskLines(MaskKind.TIME_12_AMPM, LocalDateTime(2026, 11, 15, 0, 0, 0, 0)))
        assertEquals(listOf("12:30 PM"), maskLines(MaskKind.TIME_12_AMPM, LocalDateTime(2026, 11, 15, 12, 30, 0, 0)))
    }

    @Test fun padHasMasksAndOldStylesStillDecode() {
        assertEquals(3, Specs.masksFor(com.lokit.ui.ScreenId.PAD).size)
        assertTrue(Specs.forScreen(com.lokit.ui.ScreenId.PAD).containsAll(Specs.padMasks))
        // formato antiguo (6 campos, sin grosor)
        assertEquals(MaskStyle(MaskKind.DATE, 1L, BgMode.NONE, 2L, 1f, 1, 0), MaskStyle.decode("DATE|1|NONE|2|1.0|1"))
        assertEquals(2, MaskStyle.decode(MaskStyle(weight = 2).encode())!!.weight)
    }

    @Test fun maskStyleCodec() {
        val m = MaskStyle(MaskKind.DATE, 0xFF112233L, BgMode.SOLID, 0xFFAABBCCL, 0.8f, 2)
        assertEquals(m, MaskStyle.decode(m.encode()))
        assertNull(MaskStyle.decode("basura"))
        assertNull(MaskStyle.decode(null))
    }

    @Test fun masksAreOffByDefaultAndPersist() {
        val spec = Specs.lockMasks.first()
        assertNull(AppSettings().box(spec))
        val store = SettingsStore(MemoryStore())
        val style = MaskStyle(MaskKind.TIME, 0xFF000000L, BgMode.AUTO, 0xFF000000L, 1.2f, 0)
        val s = AppSettings().withBox(spec.key, Zone(0.1f, 0.3f, 0.8f, 0.2f)).withMask(spec.key, style)
        store.save(s)
        val back = store.load()
        assertEquals(Zone(0.1f, 0.3f, 0.8f, 0.2f), back.box(spec))
        assertEquals(style, back.mask(spec))
        // Restablecer vuelve al estilo por defecto
        assertEquals(spec.maskDefault, back.resetBox(spec.key).mask(spec))
    }

    @Test fun newSettingsPersist() {
        val store = SettingsStore(MemoryStore())
        val s = AppSettings(
            dotSize = 0.5f, dotGap = 1.5f, dotBorder = 0.6f, revealBgAlpha = 0.2f, revealColor = 0xFF000000L,
            dimEnabled = true, dimSeconds = 9, dimLevel = 0.7f, dimBlur = 20, settingsHoldMs = 1500, resetHoldMs = 300,
        )
        store.save(s)
        assertEquals(s, store.load())
    }

    @Test fun hiddenButtonsDefaults() {
        val st = AppSettings()
        val settingsBox = st.box(Specs.settingsBox)!!
        val resetBox = st.box(Specs.resetBox)!!
        assertTrue(settingsBox.x > 0.5f && settingsBox.y == 0f) // arriba a la derecha
        assertTrue(resetBox.x == 0f && resetBox.y == 0f) // arriba a la izquierda
        assertEquals(800, st.settingsHoldMs)
    }
}
