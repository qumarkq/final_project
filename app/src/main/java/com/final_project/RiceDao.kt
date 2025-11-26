package com.final_project

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface RiceDao {

    @Query("SELECT * FROM rice_field WHERE id = 0 LIMIT 1")
    fun getField(): RiceFieldEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(field: RiceFieldEntity)

    @Update
    fun update(field: RiceFieldEntity)
}
