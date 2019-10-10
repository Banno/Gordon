package com.banno.android.gordontest

import org.junit.Test
import kotlin.random.Random

class FlakyTest {

    @Test
    fun flaky() {
        val randomInt = Random.Default.nextInt(0, 2)

        check(randomInt != 1) { "Flaky Test!" }
    }
}
