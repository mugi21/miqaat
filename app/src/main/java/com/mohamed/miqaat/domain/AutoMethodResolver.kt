package com.mohamed.miqaat.domain

import com.mohamed.miqaat.domain.model.CalculationSettings
import com.mohamed.miqaat.domain.model.MethodOption

/**
 * Méthode de calcul recommandée pour un pays (code ISO 3166-1 alpha-2),
 * suivie par le mode « automatique » des réglages.
 *
 * Pays sans méthode officielle documentée : rattachés au standard régional
 * le plus proche (Libye → Égyptienne, Oman/Bahreïn → Golfe). L'Iran n'est pas
 * couvert : la méthode de Téhéran exige un angle Maghrib, non supporté par
 * la librairie Adhan.
 */
object AutoMethodResolver {

    fun resolve(countryCode: String?): MethodOption = when (countryCode?.uppercase()) {
        "DZ" -> MethodOption.ALGERIA
        "TN" -> MethodOption.TUNISIA
        "MA", "EH" -> MethodOption.MOROCCO
        "LY", "EG", "SD" -> MethodOption.EGYPTIAN
        "JO", "PS" -> MethodOption.JORDAN
        "SA" -> MethodOption.UMM_AL_QURA
        "AE" -> MethodOption.DUBAI
        "KW" -> MethodOption.KUWAIT
        "QA" -> MethodOption.QATAR
        "BH", "OM" -> MethodOption.GULF
        "TR" -> MethodOption.TURKEY
        "RU" -> MethodOption.RUSSIA
        "FR" -> MethodOption.FRANCE
        "PT" -> MethodOption.PORTUGAL
        "SG" -> MethodOption.SINGAPORE
        "MY", "BN" -> MethodOption.MALAYSIA
        "ID" -> MethodOption.INDONESIA
        "PK", "IN", "BD", "AF" -> MethodOption.KARACHI
        "US", "CA" -> MethodOption.NORTH_AMERICA
        "GB" -> MethodOption.MOON_SIGHTING_COMMITTEE
        else -> MethodOption.MUSLIM_WORLD_LEAGUE
    }
}

/** Méthode réellement utilisée pour le calcul : auto (selon le pays) ou choix manuel. */
fun CalculationSettings.effectiveMethod(countryCode: String?): MethodOption =
    if (methodAuto) AutoMethodResolver.resolve(countryCode) else method
