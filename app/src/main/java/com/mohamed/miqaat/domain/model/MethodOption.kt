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
enum class MethodOption(
    val parameters: CalculationParameters,
    /**
     * Comment cette méthode passe de la seconde à la minute affichée. Par défaut, le
     * comportement d'Adhan : aucune méthode ne change tant que son calendrier officiel
     * n'a pas été mesuré sur un mois entier.
     */
    val calibration: TimeCalibration = TimeCalibration.DEFAULT,
) {
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
     * (18°/17°), plus la calibration relevée sur le calendrier officiel de la wilaya
     * de Skikda pour Rabīʿ al-Awwal 1448 (30 jours, 14 août → 12 septembre 2026).
     *
     * Les cinq décalages tiennent en deux termes : une base d'environ 85 secondes
     * commune à tous les moments (la minute de précaution du ministère, plus l'écart
     * entre notre position et son point de référence pour la ville) et, sur le seul
     * Maghrib, les **3 minutes** supplémentaires déjà repérées en D23 — 261 ≈ 85 + 176.
     * Méthode de mesure et tableau de référence : `docs/prayer-times-accuracy.md`.
     *
     * Chaque valeur est prise au milieu de son intervalle admissible, sous une
     * contrainte ferme : **ne jamais afficher avant l'heure officielle**. C'est la
     * raison d'être de l'iḥtiyāṭ, et ce qui coûte à l'ʿAṣr ses 126 s (125 est le
     * minimum qui évite d'être en avance) plutôt que les 120 qui tomberaient juste
     * deux jours de plus.
     *
     * Le shurūq n'a pas de colonne dans le calendrier officiel : faute de mesure il
     * garde un décalage nul, donc tronqué — il marque la fin du Fajr, l'annoncer un peu
     * tôt est le côté prudent.
     */
    ALGERIA(
        custom(fajr = 18.0, isha = 17.0),
        calibration = TimeCalibration(
            rounding = MinuteRounding.DOWN,
            offsetSeconds = mapOf(
                PrayerName.FAJR to 95,
                PrayerName.DHUHR to 85,
                PrayerName.ASR to 126,
                PrayerName.MAGHRIB to 261,
                PrayerName.ISHA to 82,
            ),
        ),
    ),

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
