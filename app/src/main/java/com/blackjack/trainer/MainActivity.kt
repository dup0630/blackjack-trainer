package com.blackjack.trainer

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences("scores", MODE_PRIVATE)
        updateScoreDisplay(prefs)

        findViewById<Button>(R.id.btnSums).setOnClickListener {
            startActivity(Intent(this, SumsActivity::class.java))
        }
        findViewById<Button>(R.id.btnOptimalPlay).setOnClickListener {
            startActivity(Intent(this, OptimalPlayActivity::class.java))
        }
        findViewById<Button>(R.id.btnHiLo).setOnClickListener {
            startActivity(Intent(this, HiLoActivity::class.java))
        }
        findViewById<Button>(R.id.btnResetScores).setOnClickListener {
            prefs.edit().clear().apply()
            updateScoreDisplay(prefs)
        }
    }

    override fun onResume() {
        super.onResume()
        updateScoreDisplay(getSharedPreferences("scores", MODE_PRIVATE))
    }

    private fun updateScoreDisplay(prefs: android.content.SharedPreferences) {
        fun pct(mode: String): String {
            val correct = prefs.getInt("${mode}_correct", 0)
            val attempts = prefs.getInt("${mode}_attempts", 0)
            if (attempts == 0) return "—"
            return "$correct/$attempts (${100 * correct / attempts}%)"
        }
        findViewById<TextView>(R.id.tvScoreSums).text       = "Sums: ${pct("sums")}"
        findViewById<TextView>(R.id.tvScoreOptimal).text    = "Optimal Play: ${pct("optimal")}"
        findViewById<TextView>(R.id.tvScoreHiLo).text      = "Hi-Lo Count: ${pct("hilo")}"
    }
}
