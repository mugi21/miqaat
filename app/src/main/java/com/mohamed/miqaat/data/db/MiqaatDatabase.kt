package com.mohamed.miqaat.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CachedLocationEntity::class,
        InvocationEntity::class,
        QuranReciterEntity::class,
        QuranMoshafEntity::class,
        QuranSurahEntity::class,
        QuranFavoriteEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class MiqaatDatabase : RoomDatabase() {

    abstract fun locationDao(): LocationDao

    abstract fun invocationDao(): InvocationDao

    abstract fun quranDao(): QuranDao

    abstract fun quranFavoriteDao(): QuranFavoriteDao

    companion object {
        /** v2 : code pays pour la sélection automatique de la méthode de calcul. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cached_location ADD COLUMN countryCode TEXT")
            }
        }

        /**
         * v3 : les invocations et leur rappel.
         *
         * ⚠ Room compare le schéma réel à celui qu'il attend : ce `CREATE TABLE`
         * doit être **mot pour mot** celui qu'il génère pour [InvocationEntity]
         * (types, `NOT NULL`, `AUTOINCREMENT`), sinon le premier lancement après
         * migration échoue sur une erreur de validation.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `invocation` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `builtinKey` TEXT,
                        `title` TEXT,
                        `body` TEXT,
                        `enabled` INTEGER NOT NULL,
                        `scheduleType` TEXT NOT NULL,
                        `hour` INTEGER NOT NULL,
                        `minute` INTEGER NOT NULL,
                        `anchorPrayer` TEXT,
                        `offsetMinutes` INTEGER NOT NULL,
                        `sortOrder` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        /**
         * v4 : le catalogue du Coran (récitateurs, moshafs, sourates) et les
         * favoris. Même avertissement qu'en v3 — ces `CREATE TABLE` sont ceux
         * que Room génère, **mot pour mot** : une clé primaire non
         * auto-générée s'écrit `INTEGER NOT NULL … PRIMARY KEY(id)` et non
         * `INTEGER PRIMARY KEY`, et la moindre différence fait échouer la
         * validation au premier lancement d'après migration.
         *
         * Le catalogue n'est qu'un cache : le perdre ne coûte qu'un
         * rechargement. Les **favoris**, eux, sont de la donnée de
         * l'utilisateur — c'est pour eux qu'on écrit une migration plutôt que
         * de laisser Room repartir de zéro.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `quran_reciter` (" +
                        "`id` INTEGER NOT NULL, `name` TEXT NOT NULL, " +
                        "`letter` TEXT NOT NULL, `language` TEXT NOT NULL, " +
                        "PRIMARY KEY(`id`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `quran_moshaf` (" +
                        "`id` INTEGER NOT NULL, `reciterId` INTEGER NOT NULL, " +
                        "`name` TEXT NOT NULL, `server` TEXT NOT NULL, " +
                        "`surahList` TEXT NOT NULL, PRIMARY KEY(`id`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `quran_surah` (" +
                        "`id` INTEGER NOT NULL, `name` TEXT NOT NULL, " +
                        "`makki` INTEGER NOT NULL, `language` TEXT NOT NULL, " +
                        "PRIMARY KEY(`id`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `quran_favorite` (" +
                        "`type` TEXT NOT NULL, `refId` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`type`, `refId`))",
                )
            }
        }

        fun build(context: Context): MiqaatDatabase =
            Room.databaseBuilder(context, MiqaatDatabase::class.java, "miqaat.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
    }
}
