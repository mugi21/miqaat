package com.mohamed.miqaat.domain

import java.time.Duration
import java.util.Locale

/**
 * Formate une durée en « HH:MM:SS », ex. « 02:13:05 ».
 * Les durées négatives (bord de tick) sont ramenées à zéro.
 */
fun formatCountdown(duration: Duration): String {
    val total = duration.seconds.coerceAtLeast(0)
    return String.format(
        Locale.ROOT,
        "%02d:%02d:%02d",
        total / 3600,
        (total % 3600) / 60,
        total % 60,
    )
}
