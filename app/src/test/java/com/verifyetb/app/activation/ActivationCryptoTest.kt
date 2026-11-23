package com.verifyetb.app.activation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivationCryptoTest {

    @Test
    fun roundTrip_valid() {
        val request = "K7MP-2QRN"
        val activation = ActivationCrypto.activationFor(request)
        assertTrue(ActivationCrypto.isValid(request, activation))
        assertTrue(ActivationCrypto.isValid(request.replace("-", ""), activation.lowercase()))
    }

    @Test
    fun wrongCode_rejected() {
        val request = "ABCD-EFGH"
        val activation = ActivationCrypto.activationFor(request)
        assertFalse(ActivationCrypto.isValid(request, "ZZZZ-ZZZZ"))
        assertFalse(ActivationCrypto.isValid("AAAA-BBBB", activation))
    }

    @Test
    fun stable_forSameRequest() {
        val a = ActivationCrypto.activationFor("K7MP-2QRN")
        val b = ActivationCrypto.activationFor("k7mp2qrn")
        assertEquals(a, b)
    }
}
