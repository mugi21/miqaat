package com.mohamed.miqaat.domain

import java.time.LocalDate
import java.time.chrono.HijrahChronology
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.util.Locale

/** Rang du mois de Ramadan dans l'année hégirienne. */
const val RAMADAN_MONTH = 9

/**
 * Formate une date grégorienne en date Hijri arabe, ex. « 20 صفر 1448 ».
 *
 * HijrahDate suit le calendrier Umm al-Qura : il peut différer de ±1 jour de
 * l'observation locale de la lune, d'où le décalage manuel [offsetDays].
 */
class HijriFormatter {

    // ar-DZ : noms de mois arabes avec chiffres occidentaux (usage algérien).
    private val dayFormatter = hijriFormatter("d MMMM yyyy")
    private val monthFormatter = hijriFormatter("MMMM yyyy")

    /** [offsetDays] : correction manuelle (rúya locale) appliquée avant conversion. */
    fun format(date: LocalDate, offsetDays: Int = 0): String =
        dayFormatter.format(toHijri(date, offsetDays))

    /** Mois et année seuls, ex. « رمضان 1448 » — l'en-tête du calendrier. */
    fun formatMonthYear(date: LocalDate, offsetDays: Int = 0): String =
        monthFormatter.format(toHijri(date, offsetDays))

    /** Conversion brute, pour qui a besoin du numéro de jour ou de mois. */
    fun toHijri(date: LocalDate, offsetDays: Int = 0): HijrahDate =
        HijrahDate.from(date.plusDays(offsetDays.toLong()))

    private fun hijriFormatter(pattern: String): DateTimeFormatter = DateTimeFormatter
        .ofPattern(pattern, Locale.forLanguageTag("ar-DZ"))
        .withChronology(HijrahChronology.INSTANCE)
}

/** Le quantième du mois hégirien (1 à 29 ou 30). */
val HijrahDate.hijriDayOfMonth: Int get() = get(ChronoField.DAY_OF_MONTH)

/** Le rang du mois hégirien (1 = محرّم … 9 = رمضان … 12 = ذو الحجّة). */
val HijrahDate.hijriMonth: Int get() = get(ChronoField.MONTH_OF_YEAR)

val HijrahDate.isRamadan: Boolean get() = hijriMonth == RAMADAN_MONTH
