package com.blackjack.trainer

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class SumsActivity : AppCompatActivity() {

    private var correct  = 0
    private var attempts = 0
    private var currentAnswer = 0
    private lateinit var deck: ArrayDeque<Card>
    private val handler = Handler(Looper.getMainLooper())
    private val answerButtons get() = listOf<Button>(
        findViewById(R.id.btnOpt1),
        findViewById(R.id.btnOpt2),
        findViewById(R.id.btnOpt3),
        findViewById(R.id.btnOpt4)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sums)
        deck = Deck.shuffled()
        nextQuestion()
    }

    private fun nextQuestion() {
        if (deck.size < 6) deck = Deck.shuffled()

        val numCards = (2..4).random()
        val cards = (0 until numCards).map { deck.removeFirst() }
        val info = BlackjackStrategy.handInfo(cards)
        currentAnswer = info.total

        // Build colored card display
        val ssb = SpannableStringBuilder()
        cards.forEachIndexed { i, card ->
            if (i > 0) ssb.append("   ")
            val start = ssb.length
            ssb.append(card.toString())
            val color = if (card.isRed) Color.RED else Color.WHITE
            ssb.setSpan(ForegroundColorSpan(color), start, ssb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            ssb.setSpan(RelativeSizeSpan(1.4f), start, ssb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        findViewById<TextView>(R.id.tvCards).text = ssb

        val softLabel = if (info.isSoft) "  (soft hand)" else ""
        findViewById<TextView>(R.id.tvQuestion).text = "What is the blackjack total?$softLabel"

        val options = generateOptions(currentAnswer)
        answerButtons.zip(options.shuffled()).forEach { (btn, opt) ->
            btn.text = opt.toString()
            btn.backgroundTintList = ContextCompat.getColorStateList(this, R.color.button_dark)
            btn.isEnabled = true
            btn.setOnClickListener { checkAnswer(opt) }
        }

        updateScore()
        findViewById<TextView>(R.id.tvFeedback).text = ""
    }

    private fun generateOptions(correct: Int): List<Int> {
        val opts = mutableSetOf(correct)
        val offsets = listOf(-4, -3, -2, -1, 1, 2, 3, 4, 5, -5).shuffled()
        for (off in offsets) {
            if (opts.size >= 4) break
            val candidate = correct + off
            if (candidate in 2..30 && candidate != correct) opts.add(candidate)
        }
        while (opts.size < 4) opts.add(correct + opts.size * 3 + 1)
        return opts.toList()
    }

    private fun checkAnswer(selected: Int) {
        attempts++
        val feedback = findViewById<TextView>(R.id.tvFeedback)
        answerButtons.forEach { it.isEnabled = false }

        if (selected == currentAnswer) {
            correct++
            feedback.text = "Correct! Total = $currentAnswer"
            feedback.setTextColor(ContextCompat.getColor(this, R.color.correct_green))
        } else {
            feedback.text = "Wrong — total is $currentAnswer"
            feedback.setTextColor(ContextCompat.getColor(this, R.color.wrong_red))
        }

        saveScores()
        updateScore()
        handler.postDelayed({ nextQuestion() }, 1300)
    }

    private fun updateScore() {
        val pct = if (attempts == 0) 0 else 100 * correct / attempts
        findViewById<TextView>(R.id.tvScore).text = "Score: $correct/$attempts ($pct%)"
    }

    private fun saveScores() {
        getSharedPreferences("scores", MODE_PRIVATE).edit()
            .putInt("sums_correct", correct)
            .putInt("sums_attempts", attempts)
            .apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
