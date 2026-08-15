package com.mohamed.miqaat.quran

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import com.mohamed.miqaat.R

/**
 * La pochette affichée par la notification média et l'écran verrouillé : le logo
 * Miqaat sur le vert de la marque.
 *
 * ⚠ Sans elle, ce n'est **pas** une pochette vide qui s'affiche mais celle de
 * mp3quran : leurs fichiers MP3 portent une image ID3 embarquée, et ExoPlayer
 * complète `MediaMetadata` avec les métadonnées du flux pour tout champ que
 * l'élément de la file ne renseigne pas. Renseigner `artworkData` reprend donc
 * la main — c'est le seul moyen, on ne peut pas « désactiver » la lecture des
 * tags sans perdre aussi la durée.
 *
 * Dessinée à la volée plutôt que livrée en PNG : le logo existe déjà en vectoriel
 * (`ic_launcher_foreground`, réutilisé par l'icône et l'écran de démarrage), et
 * une seule source de vérité pour le dessin vaut mieux qu'un bitmap à
 * resynchroniser à la main.
 */
object QuranArtwork {

    /** Taille confortable pour un écran verrouillé, sans peser dans la session. */
    private const val SIZE_PX = 512

    /** Le logo occupe les deux tiers du carré : il lui faut de l'air autour. */
    private const val LOGO_RATIO = 0.66f

    @Volatile
    private var cached: ByteArray? = null

    /** @return un PNG, ou null si le rendu échoue — la pochette n'est pas vitale. */
    fun pngBytes(context: Context): ByteArray? {
        cached?.let { return it }
        return runCatching { render(context) }.getOrNull()?.also { cached = it }
    }

    private fun render(context: Context): ByteArray {
        val bitmap = Bitmap.createBitmap(SIZE_PX, SIZE_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Le même dégradé que l'icône adaptative et l'écran de démarrage.
        val background = Paint().apply {
            shader = LinearGradient(
                0f, 0f, SIZE_PX.toFloat(), SIZE_PX.toFloat(),
                GRADIENT_TOP, GRADIENT_BOTTOM,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, SIZE_PX.toFloat(), SIZE_PX.toFloat(), background)

        val logo = ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground)
        if (logo != null) {
            // L'avant-plan adaptatif est dessiné sur un canevas de 108dp dont
            // seuls les 72 centraux sont sûrs : le cadrer sur toute la surface
            // le laisserait minuscule.
            val side = (SIZE_PX * LOGO_RATIO * ADAPTIVE_SCALE).toInt()
            val offset = (SIZE_PX - side) / 2
            logo.setBounds(offset, offset, offset + side, offset + side)
            logo.draw(canvas)
        }

        return ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            bitmap.recycle()
            stream.toByteArray()
        }
    }

    /** 108/72 : ce qu'il faut agrandir pour que la zone sûre remplisse le cadre visé. */
    private const val ADAPTIVE_SCALE = 108f / 72f

    // Recopiés de SplashGradientTop / SplashGradientBottom (`ui/theme/Color.kt`) :
    // ce fichier ne peut pas dépendre du thème Compose, il tourne hors composition.
    private const val GRADIENT_TOP = 0xFF0B7E48.toInt()
    private const val GRADIENT_BOTTOM = 0xFF02472A.toInt()
}
