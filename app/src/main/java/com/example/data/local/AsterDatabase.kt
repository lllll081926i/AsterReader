package com.example.data.local

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
    exportSchema = false
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
                    "aster_reader_local.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
