package com.mohamed.miqaat.ui

import android.content.Context
import androidx.annotation.StringRes
import com.mohamed.miqaat.R
import com.mohamed.miqaat.domain.model.BuiltinInvocation
import com.mohamed.miqaat.domain.model.Invocation

/** Titre d'une invocation livrée — traduit dans les trois langues. */
val BuiltinInvocation.titleRes: Int
    @StringRes get() = when (this) {
        BuiltinInvocation.MORNING -> R.string.invocation_morning_title
        BuiltinInvocation.EVENING -> R.string.invocation_evening_title
    }

/**
 * Texte d'une invocation livrée. Déclaré une seule fois, en arabe, avec
 * `translatable="false"` : un dhikr est une formule rituelle, il ne se traduit
 * pas plus que l'appel à la prière (voir D26 et la règle i18n n°5).
 */
val BuiltinInvocation.bodyRes: Int
    @StringRes get() = when (this) {
        BuiltinInvocation.MORNING -> R.string.invocation_morning_body
        BuiltinInvocation.EVENING -> R.string.invocation_evening_body
    }

/** Le titre à afficher : ressource pour une invocation livrée, texte saisi sinon. */
fun Invocation.displayTitle(context: Context): String =
    builtin?.let { context.getString(it.titleRes) } ?: title.orEmpty()

/** Le texte à lire : ressource pour une invocation livrée, texte saisi sinon. */
fun Invocation.displayBody(context: Context): String =
    builtin?.let { context.getString(it.bodyRes) } ?: body.orEmpty()
