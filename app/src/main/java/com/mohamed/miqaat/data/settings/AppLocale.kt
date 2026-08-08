package com.mohamed.miqaat.data.settings

import android.content.Context
import android.content.res.Configuration
import androidx.core.content.edit
import java.util.Locale

/** Les langues proposées dans les réglages ; [tag] null = suivre le téléphone. */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    ARABIC("ar"),
    FRENCH("fr"),
    ENGLISH("en"),
    ;

    companion object {
        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { it.tag != null && it.tag == tag } ?: SYSTEM
    }
}

/**
 * Langue choisie dans l'app, appliquée en habillant les contextes.
 *
 * Stockée en `SharedPreferences` et non dans DataStore comme les autres réglages :
 * `Activity.attachBaseContext` s'exécute avant tout le reste et doit être
 * **synchrone**. DataStore ne se lit qu'en suspend — un `runBlocking` à chaque
 * création d'activité serait un risque d'ANR pour une valeur lue à chaque écran.
 *
 * SYSTEM (défaut) n'habille rien : la langue du téléphone s'applique
 * naturellement, y compris le choix « langue par application » d'Android 13+.
 */
object AppLocale {

    private const val PREFS = "app_locale"
    private const val KEY_TAG = "language_tag"

    fun current(context: Context): AppLanguage =
        AppLanguage.fromTag(prefs(context).getString(KEY_TAG, null))

    fun set(context: Context, language: AppLanguage) {
        prefs(context).edit { putString(KEY_TAG, language.tag) }
    }

    /**
     * Renvoie [context] tel quel si l'on suit le téléphone, sinon un contexte dont
     * les ressources — textes **et** sens d'écriture — sont dans la langue choisie.
     * À appliquer partout où l'on lit une ressource hors activité : widget,
     * notifications.
     */
    fun wrap(context: Context): Context {
        val tag = current(context).tag ?: return context
        val locale = Locale.forLanguageTag(tag)
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return context.createConfigurationContext(configuration)
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
