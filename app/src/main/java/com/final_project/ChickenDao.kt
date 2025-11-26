package com.final_project

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ChickenDao {

    @Query("SELECT * FROM chicken WHERE id = :id LIMIT 1")
    fun getChicken(id: Int): ChickenEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(chicken: ChickenEntity)


    @Query("SELECT * FROM chicken")
    fun getAll(): List<ChickenEntity>
}
