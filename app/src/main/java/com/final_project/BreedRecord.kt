package com.final_project

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "breed_record")
data class BreedRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val maleId: Int,
    val femaleId: Int
)