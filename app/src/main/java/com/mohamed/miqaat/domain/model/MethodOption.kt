package com.mohamed.miqaat.domain.model

import com.batoulapps.adhan2.CalculationMethod
import com.batoulapps.adhan2.CalculationParameters
import com.batoulapps.adhan2.PrayerAdjustments

/**
 * Méthode de calcul sélectionnable dans l'app : les méthodes de la librairie
 * Adhan plus les méthodes nationales officielles qu'elle ne fournit pas
 * (paramètres relevés sur l'API AlAdhan `v1/methods`, référence du domaine).
 *
 * Les 11 premières entrées portent exactement le même `name` que l'enum Adhan
 * [CalculationMethod] : la valeur persistée dans DataStore reste lisible.
 */
enum class MethodOption(val parameters: CalculationParameters) {
    MUSLIM_WORLD_LEAGUE(CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters),
    EGYPTIAN(CalculationMethod.EGYPTIAN.parameters),
    KARACHI(CalculationMethod.KARACHI.parameters),
    UMM_AL_QURA(CalculationMethod.UMM_AL_QURA.parameters),
    DUBAI(CalculationMethod.DUBAI.parameters),
    MOON_SIGHTING_COMMITTEE(CalculationMethod.MOON_SIGHTING_COMMITTEE.parameters),
    NORTH_AMERICA(CalculationMethod.NORTH_AMERICA.parameters),
    KUWAIT(CalculationMethod.KUWAIT.parameters),
    QATAR(CalculationMethod.QATAR.parameters),
    SINGAPORE(CalculationMethod.SINGAPORE.parameters),
    TURKEY(CalculationMethod.TURKEY.parameters),

    /**
     * Algérie — ministère des Affaires religieuses et des Wakfs : angles de MWL
     * (18°/17°), mais **Maghrib retardé de 3 minutes**. Cette marge ne figure pas
     * dans la spécification AlAdhan ; elle a été relevée sur le calendrier officiel,
     * identique à Skikda le 6 août et le 15 décembre 2026 (voir D23).
     * L'Isha est calculée par angle et non par intervalle : elle n'en hérite pas.
     */
    ALGERIA(custom(fajr = 18.0, isha = 17.0, maghribMinutes = 3)),

    /** Tunisie — ministère des Affaires religieuses. */
    TUNISIA(custom(fajr = 18.0, isha = 18.0)),

    /** Maroc — ministère des Habous et des Affaires islamiques. */
    MOROCCO(custom(fajr = 19.0, isha = 17.0)),

    /** Jordanie — ministère des Awqaf (Maghrib retardé de 5 min). */
    JORDAN(custom(fajr = 18.0, isha = 18.0, maghribMinutes = 5)),

    /** France — Union des organisations islamiques de France. */
    FRANCE(custom(fajr = 12.0, isha = 12.0)),

    /** Russie — Administration spirituelle des musulmans. */
    RUSSIA(custom(fajr = 16.0, isha = 15.0)),

    /** Indonésie — Kementerian Agama (Kemenag). */
    INDONESIA(custom(fajr = 20.0, isha = 18.0)),

    /** Malaisie — Jabatan Kemajuan Islam Malaysia (JAKIM). */
    MALAYSIA(custom(fajr = 20.0, isha = 18.0)),

    /**
     * Portugal — Comunidade Islâmica de Lisboa : Isha = Maghrib + 77 min, où
     * Maghrib = coucher + 3 min. Adhan applique l'intervalle d'Isha au coucher
     * brut (avant ajustements), d'où l'ajustement de +3 min sur les deux prières.
     */
    PORTUGAL(custom(fajr = 18.0, ishaIntervalMinutes = 77, maghribMinutes = 3, ishaMinutes = 3)),

    /** Région du Golfe (Bahreïn, Oman) — Isha 90 min après le Maghrib. */
    GULF(custom(fajr = 19.5, ishaIntervalMinutes = 90)),
}

private fun custom(
    fajr: Double,
    isha: Double = 0.0,
    ishaIntervalMinutes: Int = 0,
    maghribMinutes: Int = 0,
    ishaMinutes: Int = 0,
): CalculationParameters = CalculationMethod.OTHER.parameters.copy(
    fajrAngle = fajr,
    ishaAngle = isha,
    ishaInterval = ishaIntervalMinutes,
    methodAdjustments = PrayerAdjustments(maghrib = maghribMinutes, isha = ishaMinutes),
)
