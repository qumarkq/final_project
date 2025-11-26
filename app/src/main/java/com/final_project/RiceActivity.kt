package com.final_project

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.collections.forEach
import kotlin.math.min
import kotlin.text.trimIndent

class RiceActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var riceDao: RiceDao

    // 9 塊田格
    private lateinit var plots: List<ImageView>

    private lateinit var tvInfo: TextView
    private lateinit var btnAction: Button

    private var field: RiceFieldEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rice)

        db = AppDatabase.getInstance(this)
        riceDao = db.riceDao()

        // 綁定 9 個格子
        plots = listOf(
            findViewById(R.id.ivPlot0),
            findViewById(R.id.ivPlot1),
            findViewById(R.id.ivPlot2),
            findViewById(R.id.ivPlot3),
            findViewById(R.id.ivPlot4),
            findViewById(R.id.ivPlot5),
            findViewById(R.id.ivPlot6),
            findViewById(R.id.ivPlot7),
            findViewById(R.id.ivPlot8)
        )

        tvInfo = findViewById(R.id.tvRiceInfo)
        btnAction = findViewById(R.id.btnRiceAction)

        // 讀資料，沒有就初始化一塊田：剛播種、成長 0、沒有庫存
        val saved = riceDao.getField()
        field = if (saved == null) {
            val initial = RiceFieldEntity(
                id = 0,
                stage = RiceStage.SOWING.name,
                growth = 0,
                stock = 0
            )
            riceDao.insert(initial)
            initial
        } else {
            saved
        }

        updateUi()

        btnAction.setOnClickListener {
            onActionClicked()
        }
    }

    private fun onActionClicked() {
        val f = field ?: return
        val stage = RiceStage.valueOf(f.stage)

        when (stage) {
            RiceStage.SOWING, RiceStage.SPROUT, RiceStage.GROWING -> {
                // 照顧稻米：成長度 +25，超過 100 當成 100
                val newGrowth = min(100, f.growth + 25)

                // 判斷是否進入下一階段
                val newStage = when {
                    stage == RiceStage.SOWING  && newGrowth >= 25 -> RiceStage.SPROUT
                    stage == RiceStage.SPROUT  && newGrowth >= 50 -> RiceStage.GROWING
                    stage == RiceStage.GROWING && newGrowth >= 100 -> RiceStage.RIPE
                    else -> stage
                }

                field = f.copy(
                    stage = newStage.name,
                    growth = newGrowth
                )
                riceDao.update(field!!)
                updateUi()

                Toast.makeText(this, "稻米成長了一點～", Toast.LENGTH_SHORT).show()
            }

            RiceStage.RIPE -> {
                // 結穗 → 收割，增加庫存，並重新播種
                // 規則：每次收成 10 份，其中 1 份留下當下一輪的種子 → 飼料庫存實際 +9
                val addFeed = 9
                val newStock = f.stock + addFeed

                field = f.copy(
                    stage = RiceStage.SOWING.name,
                    growth = 0,
                    stock = newStock
                )
                riceDao.update(field!!)
                updateUi()

                Toast.makeText(
                    this,
                    "收割完成！飼料庫存 +$addFeed（保留 1 份當種子）",
                    Toast.LENGTH_SHORT
                ).show()
            }

        }
    }

    private fun updateUi() {
        val f = field ?: return
        val stage = RiceStage.valueOf(f.stage)

        // 根據階段顯示不同圖片，套用到 9 塊田
        val imgRes = when (stage) {
            RiceStage.SOWING  -> R.drawable.rice_sowing
            RiceStage.SPROUT  -> R.drawable.rice_sprout
            RiceStage.GROWING -> R.drawable.rice_growing
            RiceStage.RIPE    -> R.drawable.rice_ripe
        }

        plots.forEach { iv ->
            iv.setImageResource(imgRes)
        }

        val stageText = when (stage) {
            RiceStage.SOWING  -> "階段：播種"
            RiceStage.SPROUT  -> "階段：發芽"
            RiceStage.GROWING -> "階段：成長"
            RiceStage.RIPE    -> "階段：結穗（可收割）"
        }

        tvInfo.text = """
            $stageText
            成長度：${f.growth} / 100
            稻米庫存：${f.stock}
        """.trimIndent()

        btnAction.text = when (stage) {
            RiceStage.RIPE -> "收割並重新播種"
            else           -> "照顧稻米（澆水/施肥）"
        }
    }
}
