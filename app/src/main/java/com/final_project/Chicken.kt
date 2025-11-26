package com.final_project
// 新增在檔案最上方
enum class Gender {
    MALE,  // 公
    FEMALE // 母
}

// 你的 Chicken class 改成包含 gender
data class Chicken(
    var gender: Gender = Gender.FEMALE,   // 預設母雞
    var hunger: Int = 50,
    var mood: Int = 80,
    var health: Int = 100,
    var exp: Int = 0
)
