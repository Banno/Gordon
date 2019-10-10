package com.banno.android.gordontest

import org.junit.Assert
import org.junit.Test

abstract class AbstractTest {
    @Test fun abstractA() = Assert.assertEquals(1, 1)
    @Test fun abstractB() = Assert.assertEquals(1, 1)
    @Test fun abstractC() = Assert.assertEquals(1, 1)
}