package com.final_project

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
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
    private lateinit var riceDao: RiceDao

    private var tickCount = 0

    private val tickIntervalMs = 5_000L
    private val handler = Handler(Looper.getMainLooper())

    // ⭐ 修改：不再直接寫死字串列表，改為延遲載入 (因為需要 Context 才能讀取資源)
    // 這裡只宣告變數，稍後在 onCreate 或是使用時讀取 strings.xml
    private lateinit var feedMessages: Array<String>

    private val tickRunnable = object : Runnable {
        override fun run() {
            onTimeTick()
            handler.postDelayed(this, tickIntervalMs)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ⭐ 初始化訊息列表 (從 XML 讀取)
        feedMessages = resources.getStringArray(R.array.feed_messages_array)

        // --- 初始化資料庫與 DAO ---
        db = AppDatabase.getInstance(this)
        chickenDao = db.chickenDao()
        riceDao = db.riceDao()

        // --- 取得從 World 傳來的參數 ---
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

        handler.postDelayed(tickRunnable, tickIntervalMs)
    }

    // ⭐ 餵食邏輯
    private fun onFeedClicked(tvInfo: TextView) {
        val field = riceDao.getField()

        if (field == null || field.stock <= 0) {
            // ⭐ 修改：使用 R.string 資源，解決硬編碼警告
            Toast.makeText(this, R.string.toast_no_rice, Toast.LENGTH_SHORT).show()
            return
        }

        val newField = field.copy(stock = field.stock - 1)
        riceDao.update(newField)

        chicken.hunger = (chicken.hunger - 20).coerceAtLeast(0)
        chicken.mood = (chicken.mood + 10).coerceAtMost(100)
        chicken.exp += 1

        saveChicken()
        updateUi(tvInfo)

        // ⭐ 修改：使用 R.string 資源
        Toast.makeText(this, R.string.toast_feed_success, Toast.LENGTH_SHORT).show()

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

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(tickRunnable)
    }

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

    // ⭐ 修改：UI 更新邏輯，完全消除字串串接警告
    private fun updateUi(tv: TextView) {
        val ivChicken: ImageView = findViewById(R.id.ivChicken)

        // 取得對應等級的字串資源 ID
        val stageResId: Int = when {
            chicken.exp < 10 -> {
                ivChicken.setImageResource(R.drawable.chicken_small)
                R.string.stage_small
            }
            chicken.exp < 20 -> {
                ivChicken.setImageResource(R.drawable.chicken_middle)
                R.string.stage_middle
            }
            chicken.exp < 30 -> {
                ivChicken.setImageResource(R.drawable.chicken_mid_big)
                R.string.stage_mid_big
            }
            else -> {
                ivChicken.setImageResource(R.drawable.chicken_big)
                R.string.stage_big
            }
        }
        // 透過 ID 取得實際字串
        val stageText = getString(stageResId)

        // 取得性別字串
        val genderResId = if (chicken.gender == Gender.MALE) R.string.gender_male else R.string.gender_female
        val genderText = getString(genderResId)

        val stock = riceDao.getField()?.stock ?: 0

        // ⭐ 核心修正：使用 getString 搭配參數，取代 Kotlin 的 $字串串接
        // 對應到 strings.xml 中的 chicken_status_format
        tv.text = getString(
            R.string.chicken_status_format,
            stageText,      // %1$s
            genderText,     // %2$s
            chicken.hunger, // %3$d
            chicken.mood,   // %4$d
            chicken.health, // %5$d
            chicken.exp,    // %6$d
            stock           // %7$d
        )
    }
}