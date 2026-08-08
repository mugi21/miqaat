package com.mohamed.miqaat.ui

import androidx.annotation.StringRes
import com.mohamed.miqaat.R
import com.mohamed.miqaat.domain.model.PrayerName

/** Libellé arabe de chaque moment — partagé entre l'écran d'accueil et les notifications. */
val PrayerName.labelRes: Int
    @StringRes get() = when (this) {
        PrayerName.FAJR -> R.string.prayer_fajr
        PrayerName.SUNRISE -> R.string.prayer_sunrise
        PrayerName.DHUHR -> R.string.prayer_dhuhr
        PrayerName.ASR -> R.string.prayer_asr
        PrayerName.MAGHRIB -> R.string.prayer_maghrib
        PrayerName.ISHA -> R.string.prayer_isha
    }
