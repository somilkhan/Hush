package app.hushstream.core.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [LibraryRow::class, HistoryRow::class], version = 1, exportSchema = false)
abstract class HushDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var instance: HushDatabase? = null

        fun get(context: Context): HushDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, HushDatabase::class.java, "hush.db")
                .build()
                .also { instance = it }
        }
    }
}
