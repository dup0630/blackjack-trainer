package com.blackjack.trainer

import android.graphics.Color
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class HiLoActivity : AppCompatActivity() {

    // How many cards are dealt before asking the user to guess the count
    private val CHECK_INTERVAL = 5

    private lateinit var deck: ArrayDeque<Card>
    private var runningCount   = 0
    private var cardsDealt     = 0
    private var correct        = 0
    private var attempts       = 0
    private var awaitingGuess  = false

    // Cards shown since last count-check
    private val recentCards = mutableListOf<Card>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hi_lo)

        deck = Deck.shuffled()

        findViewById<Button>(R.id.btnNextCard).setOnClickListener { dealNextCard() }
        findViewById<Button>(R.id.btnSubmitCount).setOnClickListener { submitCount() }

        updateScore()
        showCardPhase()
    }

    private fun dealNextCard() {
        if (deck.size < 2) {
            deck = Deck.shuffled()
            runningCount = 0
            cardsDealt = 0
            recentCards.clear()
            showMessage("New deck shuffled. Count reset to 0.", isInfo = true)
        }

        val card = deck.removeFirst()
        runningCount += card.hiLoCount
        cardsDealt++
        recentCards.add(card)

        displayCard(card)
        updateCardsDealt()

        if (cardsDealt % CHECK_INTERVAL == 0) {
            showCountPhase()
        }
    }

    private fun displayCard(card: Card) {
        val ssb = SpannableStringBuilder(card.toString())
        val color = if (card.isRed) Color.RED else Color.WHITE
        ssb.setSpan(ForegroundColorSpan(color), 0, ssb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        ssb.setSpan(RelativeSizeSpan(2.5f), 0, ssb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        findViewById<TextView>(R.id.tvCurrentCard).text = ssb

        // Show hi-lo hint value so user can see what they're tracking
        val countVal = card.hiLoCount
        val hint = when {
            countVal > 0 -> "+1  (low card)"
            countVal < 0 -> "-1  (high card)"
            else         -> " 0  (neutral)"
        }
        val hintColor = when {
            countVal > 0 -> ContextCompat.getColor(this, R.color.correct_green)
            countVal < 0 -> ContextCompat.getColor(this, R.color.wrong_red)
            else         -> Color.YELLOW
        }
        val hintView = findViewById<TextView>(R.id.tvCountHint)
        hintView.text = hint
        hintView.setTextColor(hintColor)

        // Show recent card history
        val history = recentCards.takeLast(10).joinToString(" ") { it.toString() }
        findViewById<TextView>(R.id.tvRecentCards).text = "Recent: $history"
    }

    private fun showCardPhase() {
        awaitingGuess = false
        findViewById<View>(R.id.layoutGuess).visibility = View.GONE
        findViewById<Button>(R.id.btnNextCard).visibility = View.VISIBLE
        findViewById<TextView>(R.id.tvPrompt).text = "Deal cards and track the running count.\nEvery $CHECK_INTERVAL cards you'll be asked for it."
    }

    private fun showCountPhase() {
        awaitingGuess = true
        attempts++
        findViewById<View>(R.id.layoutGuess).visibility = View.VISIBLE
        findViewById<Button>(R.id.btnNextCard).visibility = View.GONE
        findViewById<EditText>(R.id.etCountGuess).setText("")
        findViewById<EditText>(R.id.etCountGuess).requestFocus()
        val recentHand = recentCards.takeLast(CHECK_INTERVAL).joinToString(" ") { it.toString() }
        findViewById<TextView>(R.id.tvPrompt).text = "Last $CHECK_INTERVAL cards: $recentHand\nWhat is the running count?"
    }

    private fun submitCount() {
        val input = findViewById<EditText>(R.id.etCountGuess).text.toString().trim()
        val guessed = input.toIntOrNull()
        if (guessed == null) {
            showMessage("Enter a number (e.g. +3 or -2 or 0).", isInfo = true)
            return
        }

        val feedback = findViewById<TextView>(R.id.tvFeedback)
        if (guessed == runningCount) {
            correct++
            feedback.text = "Correct! Running count = $runningCount"
            feedback.setTextColor(ContextCompat.getColor(this, R.color.correct_green))
        } else {
            feedback.text = "Wrong — count is $runningCount (you said $guessed)"
            feedback.setTextColor(ContextCompat.getColor(this, R.color.wrong_red))
        }

        recentCards.clear()
        saveScores()
        updateScore()
        showCardPhase()
    }

    private fun showMessage(msg: String, isInfo: Boolean) {
        val feedback = findViewById<TextView>(R.id.tvFeedback)
        feedback.text = msg
        feedback.setTextColor(if (isInfo) Color.YELLOW else ContextCompat.getColor(this, R.color.wrong_red))
    }

    private fun updateCardsDealt() {
        val remaining = deck.size
        findViewById<TextView>(R.id.tvDeckInfo).text = "Cards dealt: $cardsDealt  |  Remaining: $remaining"
    }

    private fun updateScore() {
        val pct = if (attempts == 0) 0 else 100 * correct / attempts
        findViewById<TextView>(R.id.tvScore).text = "Score: $correct/$attempts ($pct%)"
    }

    private fun saveScores() {
        getSharedPreferences("scores", MODE_PRIVATE).edit()
            .putInt("hilo_correct", correct)
            .putInt("hilo_attempts", attempts)
            .apply()
    }
}
