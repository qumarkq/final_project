package com.final_project

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.min

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

        // 讀資料
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
        when (val stage = RiceStage.valueOf(f.stage)) {
            RiceStage.SOWING, RiceStage.SPROUT, RiceStage.GROWING -> {
                val newGrowth = min(100, f.growth + 25)

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

                // 修改點：使用資源字串
                Toast.makeText(this, getString(R.string.toast_grow), Toast.LENGTH_SHORT).show()
            }

            RiceStage.RIPE -> {
                val addFeed = 9
                val newStock = f.stock + addFeed

                field = f.copy(
                    stage = RiceStage.SOWING.name,
                    growth = 0,
                    stock = newStock
                )
                riceDao.update(field!!)
                updateUi()

                // 修改點：使用帶參數的資源字串 (%d 會被 addFeed 取代)
                val msg = getString(R.string.toast_harvest, addFeed)
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUi() {
        val f = field ?: return
        val stage = RiceStage.valueOf(f.stage)

        val imgRes = when (stage) {
            RiceStage.SOWING  -> R.drawable.rice_sowing
            RiceStage.SPROUT  -> R.drawable.rice_sprout
            RiceStage.GROWING -> R.drawable.rice_growing
            RiceStage.RIPE    -> R.drawable.rice_ripe
        }

        plots.forEach { iv ->
            iv.setImageResource(imgRes)
        }

        // 修改點：不再直接寫死中文，而是根據狀態取得對應的 R.string
        val stageText = getString(when (stage) {
            RiceStage.SOWING  -> R.string.stage_sowing
            RiceStage.SPROUT  -> R.string.stage_sprout
            RiceStage.GROWING -> R.string.stage_growing
            RiceStage.RIPE    -> R.string.stage_ripe
        })

        // 修改點：使用 getString 的格式化功能填入變數
        // 對應 strings.xml 裡的 %1$s, %2$d, %3$d
        tvInfo.text = getString(R.string.rice_info_format, stageText, f.growth, f.stock)

        // 修改點：按鈕文字也改用資源
        btnAction.text = getString(when (stage) {
            RiceStage.RIPE -> R.string.btn_action_harvest
            else           -> R.string.btn_action_care
        })
    }
}