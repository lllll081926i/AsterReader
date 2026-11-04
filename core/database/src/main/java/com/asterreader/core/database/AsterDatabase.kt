package com.asterreader.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        BookEntity::class,
        ChapterEntity::class,
        ShelfEntity::class,
        AnnotationEntity::class,
        RepositoryEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AsterDatabase : RoomDatabase() {
    abstract fun asterDao(): AsterDao

    companion object {
        @Volatile
        private var INSTANCE: AsterDatabase? = null

        fun getDatabase(context: Context): AsterDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AsterDatabase::class.java,
                    "aster_comics.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
