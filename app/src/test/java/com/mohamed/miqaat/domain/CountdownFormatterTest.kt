package com.mohamed.miqaat.domain

import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Test

class CountdownFormatterTest {

    @Test
    fun `duree nulle`() {
        assertEquals("00:00:00", formatCountdown(Duration.ZERO))
    }

    @Test
    fun `heures minutes secondes avec zeros de tete`() {
        assertEquals("02:13:05", formatCountdown(Duration.ofHours(2).plusMinutes(13).plusSeconds(5)))
    }

    @Test
    fun `plus de dix heures`() {
        assertEquals("11:00:09", formatCountdown(Duration.ofHours(11).plusSeconds(9)))
    }

    @Test
    fun `duree negative ramenee a zero`() {
        assertEquals("00:00:00", formatCountdown(Duration.ofSeconds(-3)))
    }
}
