package com.lokit

import com.lokit.ui.AppSettings
import com.lokit.ui.EffectState
import com.lokit.ui.KeyHit
import com.lokit.ui.Pattern
import com.lokit.ui.patternGeometry
import com.lokit.ui.UnlockMode
import com.lokit.ui.KeyValueStore
import com.lokit.ui.SettingsStore
import com.lokit.ui.Specs
import com.lokit.ui.Zone
import com.lokit.ui.FeedbackStyle
import com.lokit.ui.BgMode
import com.lokit.ui.MaskKind
import com.lokit.ui.ScreenId
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
        assertEquals(4, s.minDigits)
        assertEquals(12, s.maxDigits)
        assertEquals(Specs.keys.first { it.key == "key5" }.default, s.box(Specs.keys.first { it.key == "key5" }))
        assertNull(s.box(Specs.padBack))
    }

    @Test fun savesAndLoadsBoxesAndPin() {
        val store = SettingsStore(MemoryStore())
        val z = Zone(0.1f, 0.2f, 0.3f, 0.4f)
        val s = AppSettings(minDigits = 5, maxDigits = 9)
            .withBox(Specs.keys.first { it.key == "key5" }.key, z)
            .withBox(Specs.padDots.key, null)
            .withBox(Specs.padBack.key, Zone(0f, 0f, 0.1f, 0.1f))
        store.save(s)
        val back = store.load()
        assertEquals(5, back.minDigits)
        assertEquals(9, back.maxDigits)
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
        store.saveEffect(EffectState(1, "4321", 2))
        assertEquals(EffectState(1, "4321", 2), store.loadEffect())
    }

    @Test fun effectWithThreeAttempts() {
        val (e1, ok1) = EffectState().submit("1111", 3)
        assertFalse(ok1)
        val (e2, ok2) = e1.submit("2222", 3)
        assertFalse(ok2)
        val (e3, ok3) = e2.submit("3333", 3)
        assertTrue(ok3)
        assertEquals("1111", e3.first)
    }

    @Test fun effectWithOneAttemptUnlocksAndSaves() {
        val (e, ok) = EffectState().submit("5555", 1)
        assertTrue(ok)
        assertEquals("5555", e.first)
    }

    @Test fun patternEncodeDecode() {
        assertEquals(3 to listOf(0, 1, 4, 8), Pattern.decode(Pattern.encode(3, listOf(0, 1, 4, 8))))
        assertNull(Pattern.decode("1234"))
        assertNull(Pattern.decode("3:0,9")) // fuera de la rejilla
    }

    @Test fun patternIncludesIntermediateNodes() {
        // De 0 a 2 pasa por el 1; de 0 a 8 (diagonal) pasa por el 4.
        assertEquals(listOf(0, 1, 2), Pattern.extend(listOf(0), 2, 3))
        assertEquals(listOf(0, 4, 8), Pattern.extend(listOf(0), 8, 3))
        // Si el intermedio ya estaba visitado, no se repite; un punto repetido se ignora.
        assertEquals(listOf(1, 0, 2), Pattern.extend(listOf(1, 0), 2, 3))
        assertEquals(listOf(0, 1), Pattern.extend(listOf(0, 1), 0, 3))
        // Movimiento de caballo: sin intermedios.
        assertEquals(listOf(0, 5), Pattern.extend(listOf(0), 5, 3))
    }

    @Test fun patternGeometryIgnoresBoxSize() {
        val s = AppSettings(patternDot = 0.03f, patternGapX = 0.05f, patternGapY = 0.1f)
        // Pantalla 1000x2000: punto = 60 px, paso horizontal 160 px, vertical 260 px.
        val big = patternGeometry(s, Zone(0f, 0f, 1f, 1f), 1000f, 2000f)
        val small = patternGeometry(s, Zone(0.4f, 0.4f, 0.2f, 0.2f), 1000f, 2000f) // misma posición central, otra forma
        assertEquals(160f, big.stepX)
        assertEquals(260f, big.stepY)
        assertEquals(big.cx(0), small.cx(0))
        assertEquals(big.cy(8), small.cy(8))
        // Centro de la rejilla = centro de la caja; el punto 4 es el del medio.
        assertEquals(500f, big.cx(4))
        assertEquals(1000f, big.cy(4))
        assertEquals(4, big.nodeAt(505f, 995f))
        assertEquals(0, big.nodeAt(big.cx(0), big.cy(0)))
        assertEquals(-1, big.nodeAt(big.cx(0) + 80f, big.cy(0) + 130f)) // entre puntos
        assertEquals(-1, big.nodeAt(5f, 5f))
    }

    @Test fun patternSettingsPersist() {
        val store = SettingsStore(MemoryStore())
        store.save(
            AppSettings(
                unlockMode = UnlockMode.PATTERN, patternGrid = 4, patternMin = 6, patternDot = 0.04f,
                patternGapX = 0.05f, patternGapY = 0.07f, patternLine = 0.01f,
                patternDotColor = 0xFF112233L, patternLineColor = 0xFF445566L, askAtStart = false,
            ),
        )
        val back = store.load()
        assertEquals(UnlockMode.PATTERN, back.unlockMode)
        assertEquals(4, back.patternGrid)
        assertEquals(6, back.patternMin)
        assertEquals(0.04f, back.patternDot)
        assertEquals(0.05f, back.patternGapX)
        assertEquals(0.07f, back.patternGapY)
        assertEquals(0.01f, back.patternLine)
        assertEquals(0xFF112233L, back.patternDotColor)
        assertEquals(0xFF445566L, back.patternLineColor) // color de línea independiente del de los puntos
        assertFalse(back.askAtStart)
        assertTrue(AppSettings().askAtStart)
        assertEquals(Specs.padPattern.default, back.box(Specs.padPattern))
    }

    @Test fun patternGoesThroughEffect() {
        val p = Pattern.encode(3, listOf(0, 1, 2, 5))
        val (e1, ok1) = EffectState().submit(p, 2)
        assertFalse(ok1)
        val (e2, ok2) = e1.submit(Pattern.encode(3, listOf(6, 7, 8, 5)), 2)
        assertTrue(ok2)
        assertEquals(p, e2.first)
    }

    @Test fun guideAtStartPersists() {
        assertTrue(AppSettings().guideAtStart) // sale la primera vez
        val store = SettingsStore(MemoryStore())
        store.save(AppSettings(guideAtStart = false))
        assertFalse(store.load().guideAtStart)
    }

    @Test fun attemptsPersist() {
        val store = SettingsStore(MemoryStore())
        store.save(AppSettings(attempts = 4))
        assertEquals(4, store.load().attempts)
    }

    @Test fun keypadDefaultGrid() {
        val st = AppSettings()
        assertEquals(KeyHit.Digit('1'), hitKey(st, 0.22f, 0.51f)?.first)
        assertEquals(KeyHit.Digit('5'), hitKey(st, 0.5f, 0.6375f)?.first)
        assertEquals(KeyHit.Digit('9'), hitKey(st, 0.78f, 0.76f)?.first)
        assertEquals(KeyHit.Digit('0'), hitKey(st, 0.5f, 0.89f)?.first)
        assertEquals(KeyHit.Delete, hitKey(st, 0.78f, 0.89f)?.first)
        assertEquals(KeyHit.Ok, hitKey(st, 0.22f, 0.89f)?.first)
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

    @Test fun dateFormats() {
        val t = LocalDateTime(2026, 11, 15, 5, 3, 0, 0)
        assertEquals(listOf("15 nov"), maskLines(MaskKind.DATE_SHORT, t))
        assertEquals(listOf("domingo, 15 de noviembre"), maskLines(MaskKind.DATE_LONG, t))
    }

    @Test fun oldStylesStillDecode() {
        // formato antiguo (6 campos): el factor pasa a tamaño absoluto
        val old = MaskStyle.decode("DATE|1|NONE|2|1.0|1")!!
        assertEquals(MaskKind.DATE.defaultSize, old.size)
        assertEquals(0, old.weight)
        assertEquals(BgMode.IMAGE, MaskStyle.decode("DATE|1|AUTO|2|1.0|1")!!.bgMode) // AUTO antiguo -> imagen
        assertEquals(BgMode.NONE, MaskStyle.decode("DATE|1|NONE|2|1.0|1")!!.bgMode)
        assertEquals(2, MaskStyle.decode(MaskStyle(weight = 2).encode())!!.weight)
    }

    @Test fun maskStyleCodec() {
        val m = MaskStyle(MaskKind.DATE, 0xFF112233L, BgMode.IMAGE, 0xFFAABBCCL, 0.04f, 2, 1, 0.2f, 3)
        assertEquals(m, MaskStyle.decode(m.encode()))
        assertNull(MaskStyle.decode("basura"))
        assertNull(MaskStyle.decode(null))
    }

    @Test fun masksAddUpdateRemoveAndPersist() {
        val store = SettingsStore(MemoryStore())
        assertTrue(AppSettings().masksOf(ScreenId.LOCK).isEmpty())
        val st1 = MaskStyle(MaskKind.TIME_12, 0xFF000000L, BgMode.NONE, 0xFF000000L, 0.03f, 0)
        val (s1, a) = AppSettings().addMask(ScreenId.LOCK, Zone(0.1f, 0.3f, 0.8f, 0.2f), st1)
        val (s2, b) = s1.addMask(ScreenId.LOCK, Zone(0.2f, 0.1f, 0.3f, 0.05f), MaskStyle(MaskKind.DATE_LONG))
        assertEquals(listOf(1, 2), listOf(a.id, b.id))
        val s3 = s2.updateMask(ScreenId.LOCK, b.copy(zone = Zone(0f, 0f, 0.5f, 0.1f)))
        store.save(s3)
        val back = store.load()
        assertEquals(s3.masksOf(ScreenId.LOCK), back.masksOf(ScreenId.LOCK))
        assertEquals(st1, back.masksOf(ScreenId.LOCK).first().style)
        assertTrue(back.masksOf(ScreenId.HOME).isEmpty())
        // quitar la primera no reutiliza su id
        val s4 = back.removeMask(ScreenId.LOCK, a.id)
        assertEquals(listOf(2), s4.masksOf(ScreenId.LOCK).map { it.id })
        assertEquals(3, s4.addMask(ScreenId.LOCK, Zone(0f, 0f, 0.1f, 0.1f), MaskStyle()).second.id)
        store.save(s4)
        assertEquals(1, store.load().masksOf(ScreenId.LOCK).size)
    }

    @Test fun legacyFixedMasksMigrate() {
        val mem = MemoryStore()
        mem.putString("box_lockMask1", Zone(0.1f, 0.2f, 0.3f, 0.4f).encode())
        mem.putString("mask_lockMask1", "TIME|1|AUTO|2|1.0|1|0")
        mem.putString("box_padMask2", "")
        val back = SettingsStore(mem).load()
        val lock = back.masksOf(ScreenId.LOCK)
        assertEquals(1, lock.size)
        assertEquals(Zone(0.1f, 0.2f, 0.3f, 0.4f), lock[0].zone)
        assertEquals(MaskKind.TIME, lock[0].style.kind)
        assertTrue(back.masksOf(ScreenId.PAD).isEmpty())
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
