package com.banno.android.gordontest

import org.junit.Assert
import org.junit.Test
import kotlin.random.Random

class FlakyTest {
    @Test fun flaky() = Assert.assertEquals(Random.nextInt(2), 1)
}
