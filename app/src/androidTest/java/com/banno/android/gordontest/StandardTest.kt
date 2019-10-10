package com.banno.android.gordontest

import org.junit.Assert
import org.junit.Ignore
import org.junit.Test

class StandardTest {
    @Test fun standardA() = Assert.assertEquals(1, 1)
    @Test fun standardB() = Assert.assertEquals(1, 1)

    @Ignore
    @Test
    fun standardC() = Assert.assertEquals(1, 1)
}