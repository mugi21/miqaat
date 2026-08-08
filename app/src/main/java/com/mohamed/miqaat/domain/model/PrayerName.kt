package com.mohamed.miqaat.domain.model

/**
 * Les six moments affichés par l'application, dans l'ordre chronologique du jour.
 * Le shurūq (lever du soleil) n'est pas une prière : il marque la fin du temps du Fajr.
 */
enum class PrayerName(val isPrayer: Boolean) {
    FAJR(isPrayer = true),
    SUNRISE(isPrayer = false),
    DHUHR(isPrayer = true),
    ASR(isPrayer = true),
    MAGHRIB(isPrayer = true),
    ISHA(isPrayer = true),
}
