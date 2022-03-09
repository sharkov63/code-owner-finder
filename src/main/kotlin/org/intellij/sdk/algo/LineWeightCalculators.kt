package org.intellij.sdk.algo

import org.intellij.sdk.diff.*

/**
 * Any object which can calculate the weight of a [DiffLine].
 */
interface LineWeightCalculator {
    fun calculate(line: String): Int
}

/**
 * Simplest implementation of [LineWeightCalculator],
 * which assigns weight to be line length.
 */
@Suppress("unused")
object LengthLineWeightCalculator : LineWeightCalculator {
    override fun calculate(line: String) = line.length
}

/**
 * More intellectual implementation of [LineWeightCalculator],
 * which counts the number of "words" in a line.
 *
 * A "word" is a sequence of consecutive letters, digits, or other symbols,
 * which are separated either by blank symbols, or capital letters
 * (to distinguish individual words in camel case).
 * Basically a "word" is something like
 * one unit of information in a file.
 */
object WordLineWeightCalculator : LineWeightCalculator {

    override fun calculate(line: String): Int {
        val tokens = line.trim().split("\\s+".toRegex())
        return tokens.sumOf(WordLineWeightCalculator::wordsInToken)
    }

    enum class WordType {
        LETTER,
        DIGIT,
        OTHER,
        NONE,
    }

    private fun wordsInToken(token: String): Int {
        var counter = 0
        var currentWordType = WordType.NONE
        var previousChar: Char? = null
        token.forEach { char ->
            when {
                char.isLetter() -> {
                    if (char.isUpperCase() && previousChar?.isUpperCase() != true || currentWordType != WordType.LETTER) {
                        ++counter
                        currentWordType = WordType.LETTER
                    }
                }
                char.isDigit() -> {
                    if (currentWordType != WordType.DIGIT) {
                        ++counter
                        currentWordType = WordType.DIGIT
                    }
                }
                else -> {
                    if (currentWordType != WordType.OTHER) {
                        currentWordType = WordType.OTHER
                    }
                }
            }
            previousChar = char
        }
        return counter
    }
}