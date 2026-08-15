package com.mohamed.miqaat.quran

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.mohamed.miqaat.domain.QuranAudio
import com.mohamed.miqaat.domain.model.Moshaf

/**
 * La file de lecture, en objets Media3.
 *
 * Le `mediaId` porte le **numéro de sourate** : c'est lui que le service relit
 * pour savoir quoi enregistrer comme position, sans avoir à retenir un état en
 * parallèle de celui du lecteur.
 */
object QuranMediaItems {

    fun build(
        moshaf: Moshaf,
        reciterName: String,
        surahIds: List<Int>,
        surahNames: Map<Int, String>,
        /** La pochette Miqaat ; voir [QuranArtwork] pour pourquoi elle est obligatoire en pratique. */
        artwork: ByteArray? = null,
    ): List<MediaItem> = surahIds.mapNotNull { surahId ->
        val url = QuranAudio.audioUrl(moshaf, surahId) ?: return@mapNotNull null
        MediaItem.Builder()
            .setMediaId(surahId.toString())
            .setUri(url)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(surahNames[surahId] ?: surahId.toString())
                    .setArtist(reciterName)
                    .setAlbumTitle(moshaf.name)
                    // Renseigné ici, ce champ prime sur les métadonnées du flux —
                    // sinon c'est la pochette embarquée dans le MP3 de mp3quran
                    // qui s'affiche sur l'écran verrouillé.
                    .apply {
                        artwork?.let { setArtworkData(it, MediaMetadata.PICTURE_TYPE_FRONT_COVER) }
                    }
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build(),
            )
            .build()
    }

    /** Le numéro de sourate porté par un élément de la file, ou null. */
    fun surahIdOf(item: MediaItem?): Int? = item?.mediaId?.toIntOrNull()
}
