package com.mohamed.miqaat.ui.quran

import androidx.annotation.StringRes
import com.mohamed.miqaat.R
import com.mohamed.miqaat.domain.QuranSuggestion
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

/** Pourquoi cette sourate est proposée maintenant — même patron que `PrayerName.labelRes`. */
val QuranSuggestion.Reason.labelRes: Int
    @StringRes get() = when (this) {
        QuranSuggestion.Reason.FRIDAY -> R.string.quran_reason_friday
        QuranSuggestion.Reason.BEFORE_SLEEP -> R.string.quran_reason_before_sleep
        QuranSuggestion.Reason.MORNING -> R.string.quran_reason_morning
        QuranSuggestion.Reason.EVENING -> R.string.quran_reason_evening
        QuranSuggestion.Reason.ANYTIME -> R.string.quran_reason_anytime
    }

/**
 * `mm:ss`, ou `h:mm:ss` au-delà de l'heure — al-Baqara dépasse deux heures chez
 * plus d'un récitateur. `Locale.ROOT` : des chiffres occidentaux, comme partout
 * ailleurs dans l'app.
 */
fun formatPlaybackTime(millis: Long): String {
    val total = millis.coerceAtLeast(0).milliseconds
    val hours = total.inWholeHours
    val minutes = total.inWholeMinutes % 60
    val seconds = total.inWholeSeconds % 60
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
    }
}
