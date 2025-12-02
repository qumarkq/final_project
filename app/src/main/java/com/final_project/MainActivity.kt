package com.final_project

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.media.MediaPlayer
import android.view.View
import android.util.TypedValue

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ANIMAL_ID = "extra_animal_id"
        const val EXTRA_INITIAL_EXP = "extra_initial_exp"
    }

    private var currentAnimalId: Int = 0

    private val chicken = Chicken()

    private lateinit var db: AppDatabase
    private lateinit var chickenDao: ChickenDao
    private lateinit var riceDao: RiceDao   // ⭐ 稻米 DAO

    private var bgmPlayer: MediaPlayer? = null

    private var tickCount = 0

    // 每 5 秒更新一次雞的狀態
    private val tickIntervalMs = 5_000L
    private val handler = Handler(Looper.getMainLooper())

    private val feedMessages = listOf(
        "謝謝主人，我肚子好餓～",
        "好好吃喔，再多一點可以嗎？",
        "感覺有力氣了！一起玩吧！",
        "嗚哇，是我最喜歡的飼料！",
        "今天也是被好好照顧的一天～",
        "謝謝你沒有忘記我 ❤",
        "再不來我就要生氣啾了！",
        "吃飽了，好幸福～",
        "啾啾～我會努力長大給你看！"
    )

    private val tickRunnable = object : Runnable {
        override fun run() {
            onTimeTick()
            handler.postDelayed(this, tickIntervalMs)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // --- 初始化資料庫與 DAO ---
        db = AppDatabase.getInstance(this)
        chickenDao = db.chickenDao()
        riceDao = db.riceDao()

        // --- 背景音樂 ---
        bgmPlayer = MediaPlayer.create(this, R.raw.bgm_farm)
        bgmPlayer?.isLooping = true
        bgmPlayer?.start()

        // --- 取得從 World 傳來的參數（修正 getIntent 問題）---
        currentAnimalId = intent.getIntExtra(EXTRA_ANIMAL_ID, 0)
        val initialExpFromWorld = intent.getIntExtra(EXTRA_INITIAL_EXP, 0)

        // --- 從資料庫載回雞的狀態，或初始化 ---
        val saved = chickenDao.getChicken(currentAnimalId)
        if (saved != null) {
            chicken.gender = Gender.valueOf(saved.gender)
            chicken.hunger = saved.hunger
            chicken.mood = saved.mood
            chicken.health = saved.health
            chicken.exp = saved.exp
        } else {
            chicken.gender = Gender.FEMALE
            chicken.exp = initialExpFromWorld
        }

        val tvInfo: TextView = findViewById(R.id.tvInfo)
        val btnFeed: Button = findViewById(R.id.btnFeed)

        updateUi(tvInfo)

        btnFeed.setOnClickListener {
            onFeedClicked(tvInfo)
        }

        // 啟動定時更新雞的狀態
        handler.postDelayed(tickRunnable, tickIntervalMs)
    }

    // ⭐ 餵食邏輯：先檢查稻米庫存，再餵雞
    private fun onFeedClicked(tvInfo: TextView) {
        // 先從資料庫拿當前稻米欄位
        val field = riceDao.getField()

        if (field == null || field.stock <= 0) {
            // 沒有田或庫存 = 0 → 無法餵食
            Toast.makeText(
                this,
                "沒有稻米飼料了！請先去稻田收割～",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // 有庫存 → 扣 1 份
        val newField = field.copy(stock = field.stock - 1)
        riceDao.update(newField)

        // 照原本規則餵食
        chicken.hunger = (chicken.hunger - 20).coerceAtLeast(0)
        chicken.mood = (chicken.mood + 10).coerceAtMost(100)
        chicken.exp += 1

        saveChicken()
        updateUi(tvInfo)

        Toast.makeText(this, "餵食成功！(消耗 1 份稻米)", Toast.LENGTH_SHORT).show()

        val message = feedMessages.random()
        showFloatingMessage(message)
    }

    private fun showFloatingMessage(text: String) {
        val tvFloating: TextView = findViewById(R.id.tvFloatingMessage)

        val verticalText = text.toCharArray().joinToString("\n")
        tvFloating.text = verticalText

        val len = text.length
        val sizeSp = when {
            len <= 8  -> 18f
            len <= 12 -> 16f
            else      -> 14f
        }
        tvFloating.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)

        tvFloating.visibility = View.VISIBLE
        tvFloating.alpha = 1f
        tvFloating.translationY = 0f

        tvFloating.animate()
            .translationY(-60f)
            .alpha(0f)
            .setDuration(3000)
            .withEndAction {
                tvFloating.visibility = View.GONE
            }
            .start()
    }

    override fun onPause() {
        super.onPause()
        bgmPlayer?.pause()
    }

    override fun onResume() {
        super.onResume()
        if (bgmPlayer != null && !bgmPlayer!!.isPlaying) {
            bgmPlayer?.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(tickRunnable)
        bgmPlayer?.release()
        bgmPlayer = null
    }

    // 每 5 秒自動變餓 / 掉心情 / 掉健康 / 加經驗
    private fun onTimeTick() {
        chicken.hunger = (chicken.hunger + 5).coerceAtMost(100)

        if (chicken.hunger >= 80) {
            chicken.mood = (chicken.mood - 2).coerceAtLeast(0)
        }

        if (chicken.hunger == 100) {
            chicken.health = (chicken.health - 1).coerceAtLeast(0)
        }

        tickCount++
        if (tickCount % 3 == 0) {
            chicken.exp += 1
        }

        saveChicken()

        val tvInfo: TextView? = findViewById(R.id.tvInfo)
        tvInfo?.let { updateUi(it) }
    }

    private fun saveChicken() {
        val entity = ChickenEntity(
            id = currentAnimalId,
            gender = chicken.gender.name,
            hunger = chicken.hunger,
            mood = chicken.mood,
            health = chicken.health,
            exp = chicken.exp
        )
        chickenDao.upsert(entity)
    }

    // 更新畫面＋依 EXP 切換四種雞圖，並顯示飼料庫存
    private fun updateUi(tv: TextView) {
        val ivChicken: ImageView = findViewById(R.id.ivChicken)

        val stage: String = when {
            chicken.exp < 10 -> {
                ivChicken.setImageResource(R.drawable.chicken_small)
                "小雞"
            }
            chicken.exp < 20 -> {
                ivChicken.setImageResource(R.drawable.chicken_middle)
                "中雞"
            }
            chicken.exp < 30 -> {
                ivChicken.setImageResource(R.drawable.chicken_mid_big)
                "大中雞"
            }
            else -> {
                ivChicken.setImageResource(R.drawable.chicken_big)
                "大雞"
            }
        }

        // 性別（顯示中文）
        val genderText = if (chicken.gender == Gender.MALE) "公雞" else "母雞"

        // 顯示飼料庫存（用 riceDao，避免重複呼叫 db）
        val stock = riceDao.getField()?.stock ?: 0

        tv.text = """
        等級：$stage
        性別：$genderText
        飢餓：${chicken.hunger}
        心情：${chicken.mood}
        健康：${chicken.health}
        EXP：${chicken.exp}
        飼料庫存：$stock
        """.trimIndent()
    }
}