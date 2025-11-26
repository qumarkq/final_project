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
    private lateinit var ivAnimal0: ImageView
    private lateinit var ivAnimal1: ImageView
    private lateinit var ivAnimal2: ImageView
    private lateinit var ivAnimal3: ImageView
    private lateinit var ivAnimal4: ImageView
    private lateinit var ivAnimal5: ImageView
    private lateinit var ivAnimal6: ImageView
    private lateinit var ivAnimal7: ImageView
    private lateinit var ivAnimal8: ImageView
    private lateinit var ivAnimal9: ImageView
    private lateinit var ivAnimal10: ImageView

    private val animators = mutableListOf<AnimatorSet>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_world)

        db = AppDatabase.getInstance(this)
        chickenDao = db.chickenDao()
        breedDao = db.breedDao()

        // 綁定 11 隻 ImageView
        ivAnimal0 = findViewById(R.id.ivAnimal0)
        ivAnimal1 = findViewById(R.id.ivAnimal1)
        ivAnimal2 = findViewById(R.id.ivAnimal2)
        ivAnimal3 = findViewById(R.id.ivAnimal3)
        ivAnimal4 = findViewById(R.id.ivAnimal4)
        ivAnimal5 = findViewById(R.id.ivAnimal5)
        ivAnimal6 = findViewById(R.id.ivAnimal6)
        ivAnimal7 = findViewById(R.id.ivAnimal7)
        ivAnimal8 = findViewById(R.id.ivAnimal8)
        ivAnimal9 = findViewById(R.id.ivAnimal9)
        ivAnimal10 = findViewById(R.id.ivAnimal10)

        // 點擊：進入各自的主畫面（id = 0..10）
        ivAnimal0.setOnClickListener { openAnimalDetail(0, 0) }
        ivAnimal1.setOnClickListener { openAnimalDetail(1, 0) }
        ivAnimal2.setOnClickListener { openAnimalDetail(2, 0) }
        ivAnimal3.setOnClickListener { openAnimalDetail(3, 0) }
        ivAnimal4.setOnClickListener { openAnimalDetail(4, 0) }
        ivAnimal5.setOnClickListener { openAnimalDetail(5, 0) }
        ivAnimal6.setOnClickListener { openAnimalDetail(6, 0) }
        ivAnimal7.setOnClickListener { openAnimalDetail(7, 0) }
        ivAnimal8.setOnClickListener { openAnimalDetail(8, 0) }
        ivAnimal9.setOnClickListener { openAnimalDetail(9, 0) }
        ivAnimal10.setOnClickListener { openAnimalDetail(10, 0) }

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
            listOf(
                ivAnimal0, ivAnimal1, ivAnimal2, ivAnimal3, ivAnimal4, ivAnimal5,
                ivAnimal6, ivAnimal7, ivAnimal8, ivAnimal9, ivAnimal10
            ).forEach { img ->
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

    private fun openAnimalDetail(id: Int, initialExp: Int) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_ANIMAL_ID, id)
            putExtra(MainActivity.EXTRA_INITIAL_EXP, initialExp)
        }
        startActivity(intent)
    }

    // ========= 依等級更新世界畫面的雞圖 =========

    private fun updateAnimalsUi() {
        updateSingleAnimalUi(0, defaultExp = 0, imageView = ivAnimal0)
        updateSingleAnimalUi(1, defaultExp = 0, imageView = ivAnimal1)
        updateSingleAnimalUi(2, defaultExp = 0, imageView = ivAnimal2)
        updateSingleAnimalUi(3, defaultExp = 0, imageView = ivAnimal3)
        updateSingleAnimalUi(4, defaultExp = 0, imageView = ivAnimal4)
        updateSingleAnimalUi(5, defaultExp = 0, imageView = ivAnimal5)
        updateSingleAnimalUi(6, defaultExp = 0, imageView = ivAnimal6)
        updateSingleAnimalUi(7, defaultExp = 0, imageView = ivAnimal7)
        updateSingleAnimalUi(8, defaultExp = 0, imageView = ivAnimal8)
        updateSingleAnimalUi(9, defaultExp = 0, imageView = ivAnimal9)
        updateSingleAnimalUi(10, defaultExp = 0, imageView = ivAnimal10)
    }

    private fun updateSingleAnimalUi(id: Int, defaultExp: Int, imageView: ImageView) {
        val saved = chickenDao.getChicken(id)

        val exp: Int
        val genderStr: String

        if (saved != null) {
            // 🐔 這格已經有雞了 → 顯示出來
            exp = saved.exp
            genderStr = saved.gender
            imageView.visibility = View.VISIBLE

        } else {
            // 🐣 資料庫沒有這隻雞

            if (id == 0 || id == 1) {
                // ⭐ 只有 0 / 1 會「自動建立」：一隻小公雞 + 一隻小母雞
                exp = defaultExp
                genderStr = if (id == 0) Gender.MALE.name else Gender.FEMALE.name

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

    /** 在畫面寬度 width、高度 height 的前提下，讓 view 在「螢幕下半部」隨機走來走去。 */
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

    /** 讓動物往下一個「下半部隨機位置」移動，結束後自己再呼叫下一段。 */
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
                    // 用這個 Activity 的實例來判斷，而不是 android.app.Activity 類別
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