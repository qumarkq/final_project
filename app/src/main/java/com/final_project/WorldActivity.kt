package com.final_project

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class WorldActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var chickenDao: ChickenDao
    private lateinit var breedDao: BreedDao

    // 11 個動物
    private lateinit var animals: List<ImageView>

    private val animators = mutableListOf<AnimatorSet>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_world)

        db = AppDatabase.getInstance(this)
        chickenDao = db.chickenDao()
        breedDao = db.breedDao()

        // 綁定 11 隻 ImageView (使用 List 管理更乾淨)
        animals = listOf(
            findViewById(R.id.ivAnimal0),
            findViewById(R.id.ivAnimal1),
            findViewById(R.id.ivAnimal2),
            findViewById(R.id.ivAnimal3),
            findViewById(R.id.ivAnimal4),
            findViewById(R.id.ivAnimal5),
            findViewById(R.id.ivAnimal6),
            findViewById(R.id.ivAnimal7),
            findViewById(R.id.ivAnimal8),
            findViewById(R.id.ivAnimal9),
            findViewById(R.id.ivAnimal10)
        )

        // 設定點擊事件：進入各自的主畫面
        animals.forEachIndexed { index, imageView ->
            imageView.setOnClickListener { openAnimalDetail(index) }
        }

        // 前往稻田按鈕
        val btnToRice: Button = findViewById(R.id.btnToRice)
        btnToRice.setOnClickListener {
            openRiceField()
        }

        // 一進來，依照資料庫更新外觀
        updateAnimalsUi()

        // 等 layout 完成後，啟動「下半部隨機走路」
        val root = findViewById<View>(R.id.rootWorld)
        root.post {
            val width = root.width
            val height = root.height
            animals.forEach { img ->
                startRandomWalk(img, width, height)
            }
        }
    }

    private fun openRiceField() {
        val intent = Intent(this, RiceActivity::class.java)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        updateAnimalsUi()
        animators.forEach { if (!it.isStarted) it.start() }
        checkBreeding()   // ⭐ 回來時檢查是否可以生小雞
    }

    override fun onPause() {
        super.onPause()
        animators.forEach { it.pause() }
    }

    override fun onDestroy() {
        super.onDestroy()
        animators.forEach { it.cancel() }
        animators.clear()
    }

    // 參數簡化：移除沒用到的 initialExp
    private fun openAnimalDetail(id: Int) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_ANIMAL_ID, id)
        }
        startActivity(intent)
    }

    // ========= 依等級更新世界畫面的雞圖 =========

    private fun updateAnimalsUi() {
        // 使用迴圈更新，程式碼更簡潔
        animals.forEachIndexed { index, imageView ->
            updateSingleAnimalUi(index, imageView)
        }
    }

    // 修正警告：移除了永遠是 0 的 defaultExp 參數
    private fun updateSingleAnimalUi(id: Int, imageView: ImageView) {
        val saved = chickenDao.getChicken(id)

        val exp: Int

        if (saved != null) {
            // 🐔 這格已經有雞了 → 顯示出來
            exp = saved.exp
            imageView.visibility = View.VISIBLE

        } else {
            // 🐣 資料庫沒有這隻雞
            if (id == 0 || id == 1) {
                // ⭐ 只有 0 / 1 會「自動建立」：一隻小公雞 + 一隻小母雞
                exp = 0 // 初始經驗值固定為 0
                val genderStr = if (id == 0) Gender.MALE.name else Gender.FEMALE.name

                val newEntity = ChickenEntity(
                    id = id,
                    gender = genderStr,
                    hunger = 50,
                    mood = 80,
                    health = 100,
                    exp = exp
                )
                chickenDao.upsert(newEntity)

                imageView.visibility = View.VISIBLE
            } else {
                // ⭐ 2..10 一開始沒有雞 → 這一格先不顯示
                imageView.visibility = View.INVISIBLE
                return
            }
        }

        imageView.setImageResource(getDrawableForExp(exp))
    }

    private fun getDrawableForExp(exp: Int): Int {
        return when {
            exp < 10 -> R.drawable.chicken_small
            exp < 20 -> R.drawable.chicken_middle
            exp < 30 -> R.drawable.chicken_mid_big
            else     -> R.drawable.chicken_big
        }
    }

    // ========= 生小雞邏輯 =========

    private fun checkBreeding() {
        val all = chickenDao.getAll()

        // 先找一隻大公雞
        val male = all.firstOrNull { it.gender == Gender.MALE.name && it.exp >= 30 }
        // 再找一隻大母雞
        val female = all.firstOrNull { it.gender == Gender.FEMALE.name && it.exp >= 30 }

        if (male == null || female == null) return

        // ⭐ 檢查這對是否已經生過小雞
        val existed = breedDao.getRecord(male.id, female.id)
        if (existed != null) {
            // 已經生過，不再生
            return
        }

        // 找一個還沒被使用的 id
        val usedIds = all.map { it.id }.toSet()
        val freeId = (2..10).firstOrNull { it !in usedIds } ?: return

        val isMale = Random.nextBoolean()
        val gender = if (isMale) Gender.MALE.name else Gender.FEMALE.name

        // 建立一隻小雞
        val baby = ChickenEntity(
            id = freeId,
            gender = gender,
            hunger = 50,
            mood = 80,
            health = 100,
            exp = 0
        )
        chickenDao.upsert(baby)

        // ⭐ 記錄這對公母已經生育過
        val record = BreedRecord(
            maleId = male.id,
            femaleId = female.id
        )
        breedDao.insert(record)

        Toast.makeText(
            this,
            "一隻小${if (isMale) "公" else "母"}雞誕生了！",
            Toast.LENGTH_SHORT
        ).show()

        updateAnimalsUi()
    }

    // ========= 下半部隨機走路邏輯 =========

    private fun startRandomWalk(view: ImageView, screenWidth: Int, screenHeight: Int) {
        val imageWidth = view.width
        val imageHeight = view.height

        val minX = 0f
        val maxX = (screenWidth - imageWidth).toFloat()

        val minY = screenHeight * 0.5f
        val maxY = (screenHeight - imageHeight).toFloat()

        if (view.x == 0f && view.y == 0f) {
            val startX = Random.nextFloat() * (maxX - minX) + minX
            val startY = Random.nextFloat() * (maxY - minY) + minY
            view.x = startX
            view.y = startY
        }

        playNextRandomStep(view, screenWidth, screenHeight)
    }

    private fun playNextRandomStep(view: ImageView, screenWidth: Int, screenHeight: Int) {
        val imageWidth = view.width
        val imageHeight = view.height

        val minX = 0f
        val maxX = (screenWidth - imageWidth).toFloat()

        val minY = screenHeight * 0.5f
        val maxY = (screenHeight - imageHeight).toFloat()

        val targetX = Random.nextFloat() * (maxX - minX) + minX
        val targetY = Random.nextFloat() * (maxY - minY) + minY

        val animX = ObjectAnimator.ofFloat(view, "x", view.x, targetX)
        val animY = ObjectAnimator.ofFloat(view, "y", view.y, targetY)

        val set = AnimatorSet().apply {
            duration = Random.nextLong(2500L, 4500L)
            playTogether(animX, animY)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (!this@WorldActivity.isFinishing && !this@WorldActivity.isDestroyed) {
                        playNextRandomStep(view, screenWidth, screenHeight)
                    }
                }
            })
        }

        animators.add(set)
        set.start()
    }
}