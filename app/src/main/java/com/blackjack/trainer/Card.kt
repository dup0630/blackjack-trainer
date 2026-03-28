package com.blackjack.trainer

data class Card(val value: String, val suit: String) {

    val suitSymbol: String get() = when (suit) {
        "H" -> "\u2665"
        "D" -> "\u2666"
        "C" -> "\u2663"
        "S" -> "\u2660"
        else -> suit
    }

    val isRed: Boolean get() = suit == "H" || suit == "D"

    // Numeric value for total calculation (aces start at 11; handled in strategy)
    val numericValue: Int get() = when (value) {
        "A"          -> 11
        "K", "Q", "J" -> 10
        else          -> value.toInt()
    }

    // Hi-Lo count contribution
    val hiLoCount: Int get() = when (value) {
        "2", "3", "4", "5", "6" -> +1
        "7", "8", "9"            ->  0
        else                     -> -1  // 10, J, Q, K, A
    }

    override fun toString() = "$value$suitSymbol"
}

object Deck {
    private val VALUES = listOf("2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A")
    private val SUITS  = listOf("H", "D", "C", "S")

    fun shuffled(): ArrayDeque<Card> =
        VALUES.flatMap { v -> SUITS.map { s -> Card(v, s) } }
            .shuffled()
            .let { ArrayDeque(it) }
}
