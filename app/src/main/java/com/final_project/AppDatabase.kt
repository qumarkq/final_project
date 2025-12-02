package com.final_project

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
//import kotlin.jvm.java

@Database(
    entities = [
        ChickenEntity::class,
        BreedRecord::class,
        RiceFieldEntity::class   // ⭐ 新增稻米
    ],
    version = 5,                 // ⭐ 比之前的大（例如原本 4 → 改成 5）
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun chickenDao(): ChickenDao
    abstract fun breedDao(): BreedDao
    abstract fun riceDao(): RiceDao      // ⭐ 加這行

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chicken_db"
                )
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
