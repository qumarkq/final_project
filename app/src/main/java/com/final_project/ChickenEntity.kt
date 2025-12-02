package com.final_project

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chicken")
data class ChickenEntity(
    @PrimaryKey val id: Int,
    val gender: String,
    val hunger: Int,
    val mood: Int,
    val health: Int,
    val exp: Int
)