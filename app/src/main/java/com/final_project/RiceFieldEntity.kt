package com.final_project

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rice_field")
data class RiceFieldEntity(
    @PrimaryKey val id: Int = 0,         // 就 0 這一筆
    val stage: String,                   // RiceStage.name
    val growth: Int,                     // 0..100 成長度
    val stock: Int                       // 倉庫裡有多少單位稻米（給雞吃）
)
