package com.blackjack.trainer

object BlackjackStrategy {

    enum class Action(val label: String, val description: String) {
        HIT   ("Hit",    "Take another card."),
        STAND  ("Stand",  "Keep your current hand."),
        DOUBLE ("Double", "Double your bet and take exactly one more card."),
        SPLIT  ("Split",  "Split the pair into two separate hands.")
    }

    data class HandInfo(val total: Int, val isSoft: Boolean, val isPair: Boolean)

    /** Returns (total, isSoft) for a list of cards. */
    fun handInfo(cards: List<Card>): HandInfo {
        var sum = 0
        var aces = 0
        for (c in cards) {
            if (c.value == "A") { aces++; sum += 11 } else sum += c.numericValue
        }
        var soft = aces > 0
        while (sum > 21 && aces > 0) { sum -= 10; aces-- }
        if (aces == 0) soft = false
        val isPair = cards.size == 2 && cards[0].value == cards[1].value
        return HandInfo(sum, soft, isPair)
    }

    fun optimalAction(playerCards: List<Card>, dealerUpcard: Card): Action {
        val info = handInfo(playerCards)
        val d = dealerUpcard.value

        if (info.isPair) {
            pairAction(playerCards[0].value, d)?.let { return it }
        }

        return if (info.isSoft) softAction(info.total, d) else hardAction(info.total, d)
    }

    private fun pairAction(pairVal: String, d: String): Action? = when (pairVal) {
        "A"       -> Action.SPLIT
        "8"       -> Action.SPLIT
        "10","J","Q","K" -> Action.STAND
        "9"       -> if (d in listOf("7","10","J","Q","K","A")) Action.STAND else Action.SPLIT
        "7"       -> if (d in listOf("2","3","4","5","6","7")) Action.SPLIT else Action.HIT
        "6"       -> if (d in listOf("2","3","4","5","6")) Action.SPLIT else Action.HIT
        "5"       -> null  // treat as hard 10
        "4"       -> if (d in listOf("5","6")) Action.SPLIT else Action.HIT
        "3","2"   -> if (d in listOf("2","3","4","5","6","7")) Action.SPLIT else Action.HIT
        else      -> null
    }

    private fun softAction(total: Int, d: String): Action = when (total) {
        13, 14 -> if (d in listOf("5","6")) Action.DOUBLE else Action.HIT
        15, 16 -> if (d in listOf("4","5","6")) Action.DOUBLE else Action.HIT
        17     -> if (d in listOf("3","4","5","6")) Action.DOUBLE else Action.HIT
        18     -> when (d) {
            in listOf("2","3","4","5","6") -> Action.DOUBLE
            in listOf("7","8")             -> Action.STAND
            else                           -> Action.HIT
        }
        else   -> Action.STAND  // soft 19, 20, 21
    }

    private fun hardAction(total: Int, d: String): Action {
        if (total >= 17) return Action.STAND
        if (total <= 8)  return Action.HIT
        return when (total) {
            9  -> if (d in listOf("3","4","5","6")) Action.DOUBLE else Action.HIT
            10 -> if (d in listOf("10","J","Q","K","A")) Action.HIT else Action.DOUBLE
            11 -> if (d == "A") Action.HIT else Action.DOUBLE
            12 -> if (d in listOf("4","5","6")) Action.STAND else Action.HIT
            in 13..16 -> if (d in listOf("2","3","4","5","6")) Action.STAND else Action.HIT
            else -> Action.HIT
        }
    }

    fun explanation(playerCards: List<Card>, dealerUpcard: Card, action: Action): String {
        val info = handInfo(playerCards)
        val handDesc = when {
            info.isPair -> "Pair of ${playerCards[0].value}s"
            info.isSoft -> "Soft ${info.total}"
            else        -> "Hard ${info.total}"
        }
        return buildString {
            append("$handDesc vs dealer ${dealerUpcard.value}: ")
            append("${action.label}. ")
            append(action.description)
        }
    }
}
