package com.lokit

import com.lokit.ui.AppSettings
import com.lokit.ui.EffectState
import com.lokit.ui.KeyHit
import com.lokit.ui.KeyValueStore
import com.lokit.ui.SettingsStore
import com.lokit.ui.Specs
import com.lokit.ui.Zone
import com.lokit.ui.FeedbackStyle
import com.lokit.ui.hitKey
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
}
