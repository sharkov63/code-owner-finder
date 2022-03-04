package org.intellij.sdk.action

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class WordLineWeightCalculatorTests {

    private val calculator: LineWeightCalculator = WordLineWeightCalculator

    private fun singleTest(line: String, expectedWeight: Int) {
        val calculatedWeight = calculator.calculate(line)
        assertEquals(expectedWeight, calculatedWeight)
    }

    @Test
    fun `Correct word count on simple sentences`() {
        singleTest("hello my dear friend", 4)
        singleTest("   Abacaba hey      MANY SpAcE \t a tAb      ", 9)
        singleTest(" кириллица   Латиница ъюычыфоажыоадылвфаз     привет ", 4)
    }

    @Test
    fun `Correct word count on camel case`() {
        singleTest("vcsFileRevision", 3)
        singleTest("CodeOwnerFinder", 3)
        singleTest("WordLineWeightCalculator", 4)
        singleTest("НоменклатураХарактеристкаТорговаяМарка", 4)
        singleTest("CAPSLOCK", 1)
        singleTest("FixADiffFIle", 3)
    }

    @Test
    fun `Correct word count on snake case`() {
        singleTest("make_next_state", 3)
        singleTest("гомоморфный_образ_группы_до_победы_коммунизма_изоморфен_факторгруппе_по_ядру_гомоморфизма", 11)
    }

    @Test
    fun `Correct word count on source code`() {
        singleTest("WordLineWeightCalculator.calculate(line)", 6)
        singleTest("override fun calculateKnowledgeLevelOf(developer: String, history: DiffHistory): Double {", 12)
    }
}