package app.hushstream.core.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [LibraryRow::class, HistoryRow::class, DownloadRow::class],
    version = 2,
    exportSchema = false,
)
abstract class HushDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
    abstract fun historyDao(): HistoryDao
    abstract fun downloadDao(): DownloadDao

    companion object {
        /** Adds only the `downloads` table. `library` and `watch_history` are not touched. */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `downloads` (" +
                        "`id` TEXT NOT NULL, " +
                        "`contentType` TEXT NOT NULL, " +
                        "`parentMetaId` TEXT NOT NULL, " +
                        "`videoId` TEXT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`seasonNumber` INTEGER, " +
                        "`episodeNumber` INTEGER, " +
                        "`episodeTitle` TEXT, " +
                        "`sourceUrl` TEXT NOT NULL, " +
                        "`localFileUri` TEXT, " +
                        "`fileName` TEXT NOT NULL, " +
                        "`status` TEXT NOT NULL, " +
                        "`downloadedBytes` INTEGER NOT NULL, " +
                        "`totalBytes` INTEGER, " +
                        "`errorMessage` TEXT, " +
                        "`createdAtEpochMs` INTEGER NOT NULL, " +
                        "`updatedAtEpochMs` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))",
                )
            }
        }

        @Volatile
        private var instance: HushDatabase? = null

        fun get(context: Context): HushDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, HushDatabase::class.java, "hush.db")
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { instance = it }
        }
    }
}
