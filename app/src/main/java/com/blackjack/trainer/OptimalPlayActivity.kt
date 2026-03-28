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

class OptimalPlayActivity : AppCompatActivity() {

    private var correct  = 0
    private var attempts = 0
    private var currentAction = BlackjackStrategy.Action.HIT
    private lateinit var deck: ArrayDeque<Card>
    private val handler = Handler(Looper.getMainLooper())

    private val actionButtons get() = mapOf(
        BlackjackStrategy.Action.HIT    to findViewById<Button>(R.id.btnHit),
        BlackjackStrategy.Action.STAND  to findViewById<Button>(R.id.btnStand),
        BlackjackStrategy.Action.DOUBLE to findViewById<Button>(R.id.btnDouble),
        BlackjackStrategy.Action.SPLIT  to findViewById<Button>(R.id.btnSplit)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_optimal_play)
        deck = Deck.shuffled()
        nextQuestion()
    }

    private fun nextQuestion() {
        if (deck.size < 6) deck = Deck.shuffled()

        // Deal player 2 cards and dealer 1 upcard
        // Occasionally generate a pair for split practice (~25% of hands)
        val playerCards: List<Card>
        val dealerCard: Card

        val forcePair = (0..3).random() == 0
        if (forcePair) {
            val firstCard = deck.removeFirst()
            // Find or fake a matching card value
            val matchIdx = deck.indexOfFirst { it.value == firstCard.value }
            val secondCard = if (matchIdx >= 0) deck.removeAt(matchIdx) else deck.removeFirst()
            playerCards = listOf(firstCard, secondCard)
        } else {
            playerCards = listOf(deck.removeFirst(), deck.removeFirst())
        }
        dealerCard = deck.removeFirst()

        currentAction = BlackjackStrategy.optimalAction(playerCards, dealerCard)

        // Display player hand
        findViewById<TextView>(R.id.tvPlayerHand).text = buildCardSpan(playerCards)

        // Display dealer upcard
        val dealerSsb = SpannableStringBuilder()
        dealerSsb.append(dealerCard.toString())
        val dealerColor = if (dealerCard.isRed) Color.RED else Color.WHITE
        dealerSsb.setSpan(ForegroundColorSpan(dealerColor), 0, dealerSsb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        dealerSsb.setSpan(RelativeSizeSpan(1.4f), 0, dealerSsb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        findViewById<TextView>(R.id.tvDealerCard).text = dealerSsb

        val info = BlackjackStrategy.handInfo(playerCards)
        val handLabel = when {
            info.isPair -> "Pair of ${playerCards[0].value}s"
            info.isSoft -> "Soft ${info.total}"
            else        -> "Hard ${info.total}"
        }
        findViewById<TextView>(R.id.tvHandLabel).text = handLabel

        // Enable all buttons
        actionButtons.forEach { (_, btn) ->
            btn.isEnabled = true
            btn.backgroundTintList = ContextCompat.getColorStateList(this, R.color.button_dark)
            btn.alpha = 1f
        }

        // Disable Split if not a pair
        if (!info.isPair) {
            actionButtons[BlackjackStrategy.Action.SPLIT]?.apply {
                isEnabled = false
                alpha = 0.35f
            }
        }

        actionButtons.forEach { (action, btn) ->
            btn.setOnClickListener { checkAnswer(action, playerCards, dealerCard) }
        }

        updateScore()
        findViewById<TextView>(R.id.tvFeedback).text = ""
    }

    private fun buildCardSpan(cards: List<Card>): SpannableStringBuilder {
        val ssb = SpannableStringBuilder()
        cards.forEachIndexed { i, card ->
            if (i > 0) ssb.append("   ")
            val start = ssb.length
            ssb.append(card.toString())
            val color = if (card.isRed) Color.RED else Color.WHITE
            ssb.setSpan(ForegroundColorSpan(color), start, ssb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            ssb.setSpan(RelativeSizeSpan(1.4f), start, ssb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return ssb
    }

    private fun checkAnswer(selected: BlackjackStrategy.Action, playerCards: List<Card>, dealerCard: Card) {
        attempts++
        actionButtons.forEach { (_, btn) -> btn.isEnabled = false }

        val feedback = findViewById<TextView>(R.id.tvFeedback)
        val explanation = BlackjackStrategy.explanation(playerCards, dealerCard, currentAction)

        if (selected == currentAction) {
            correct++
            feedback.text = "Correct!\n$explanation"
            feedback.setTextColor(ContextCompat.getColor(this, R.color.correct_green))
        } else {
            feedback.text = "Wrong — optimal: ${currentAction.label}\n$explanation"
            feedback.setTextColor(ContextCompat.getColor(this, R.color.wrong_red))
        }

        saveScores()
        updateScore()
        handler.postDelayed({ nextQuestion() }, 2000)
    }

    private fun updateScore() {
        val pct = if (attempts == 0) 0 else 100 * correct / attempts
        findViewById<TextView>(R.id.tvScore).text = "Score: $correct/$attempts ($pct%)"
    }

    private fun saveScores() {
        getSharedPreferences("scores", MODE_PRIVATE).edit()
            .putInt("optimal_correct", correct)
            .putInt("optimal_attempts", attempts)
            .apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
