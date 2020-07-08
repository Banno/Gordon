package com.banno.android.gordontest

import org.junit.Assert
import org.junit.Test

open class BaseClassTest {
    @Test fun baseA() = Assert.assertEquals(1, 1)
    @Test fun baseB() = Assert.assertEquals(1, 1)
    @Test fun baseC() = Assert.assertEquals(1, 1)
}
