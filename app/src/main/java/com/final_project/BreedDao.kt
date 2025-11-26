package com.final_project

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface BreedDao {

    @Insert
    fun insert(record: BreedRecord)

    @Query("SELECT * FROM breed_record WHERE maleId = :male AND femaleId = :female LIMIT 1")
    fun getRecord(male: Int, female: Int): BreedRecord?
}
