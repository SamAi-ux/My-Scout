package com.example

import com.example.model.JenjangPramuka
import com.example.model.sanitizeGudepId
import com.example.security.PasswordHasher
import com.example.ui.components.MorseCodec
import com.example.ui.components.SandiCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun passwordHasher_hashesAndVerifiesSafely() {
        val raw = "PramukaHebat123!"
        val hashed = PasswordHasher.hashPassword(raw)
        assertNotEquals(raw, hashed)
        assertFalse(hashed.contains(raw))
        assertTrue(PasswordHasher.verifyPassword(raw, hashed))
        assertFalse(PasswordHasher.verifyPassword("SalahPassword", hashed))
    }

    @Test
    fun morseCodec_roundTripsIndonesianWords() {
        val encoded = MorseCodec.encode("PRAMUKA JAYA")
        assertEquals("PRAMUKA JAYA", MorseCodec.decode(encoded))
    }

    @Test
    fun sandiCodec_encodesCorrectly() {
        assertEquals("1 2 3", SandiCodec.toSandiAngka("ABC"))
        assertEquals("NOP", SandiCodec.toSandiAn("ABC"))
        assertEquals("ZYX", SandiCodec.toSandiAz("ABC"))
    }

    @Test
    fun sanitizeGudepId_andJenjangRoles_workCorrectly() {
        assertEquals("01_001", sanitizeGudepId("01.001"))
        assertTrue(JenjangPramuka.PEMBINA.isPembinaRole)
        assertTrue(JenjangPramuka.PEMBINA_DAMPING.isPembinaRole)
        assertFalse(JenjangPramuka.PENGGALANG.isPembinaRole)
    }
}
