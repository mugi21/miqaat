package com.mohamed.miqaat.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CachedLocationEntity::class, InvocationEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class MiqaatDatabase : RoomDatabase() {

    abstract fun locationDao(): LocationDao

    abstract fun invocationDao(): InvocationDao

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

        fun build(context: Context): MiqaatDatabase =
            Room.databaseBuilder(context, MiqaatDatabase::class.java, "miqaat.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
    }
}
