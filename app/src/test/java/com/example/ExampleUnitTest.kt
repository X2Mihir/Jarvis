package com.example

import com.example.domain.MemoryCategory
import com.example.domain.PhoneActionType
import com.example.security.CryptoVault
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun cryptoVault_encrypts_and_decrypts_with_unique_iv() {
        val secret = "Mom's phone is +1-555-0199 and standup is 9:30 AM"
        val cipher1 = CryptoVault.encrypt(secret)
        val cipher2 = CryptoVault.encrypt(secret)

        assertTrue(cipher1.startsWith("ENC1:"))
        assertNotEquals(secret, cipher1)
        assertNotEquals(cipher1, cipher2)
        assertEquals(secret, CryptoVault.decrypt(cipher1))
        assertEquals(secret, CryptoVault.decrypt(cipher2))
    }

    @Test
    fun domainEnums_parse_gracefully() {
        assertEquals(MemoryCategory.PEOPLE, MemoryCategory.fromString("people"))
        assertEquals(MemoryCategory.FACTS, MemoryCategory.fromString("unknown"))
        assertEquals(PhoneActionType.SMS, PhoneActionType.fromString("sms"))
        assertEquals(PhoneActionType.NONE, PhoneActionType.fromString("invalid"))
    }
}
