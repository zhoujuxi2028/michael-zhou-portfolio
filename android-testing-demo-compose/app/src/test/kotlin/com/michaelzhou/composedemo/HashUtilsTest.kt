package com.michaelzhou.composedemo

import com.michaelzhou.composedemo.utils.sha256
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class HashUtilsTest {

    @Test
    fun sha256_knownValue() {
        assertEquals(
            "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
            sha256("hello")
        )
    }

    @Test
    fun sha256_emptyString() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            sha256("")
        )
    }

    @Test
    fun sha256_isConsistent() {
        assertEquals(sha256("password123"), sha256("password123"))
    }

    @Test
    fun sha256_caseSensitive() {
        assertNotEquals(sha256("password"), sha256("Password"))
    }
}
