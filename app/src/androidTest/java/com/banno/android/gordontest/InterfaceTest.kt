package com.banno.android.gordontest

import org.junit.Assert
import org.junit.Test

interface InterfaceTest {
    @Test fun interfaceA() = Assert.assertEquals(1, 1)
    @Test fun interfaceB() = Assert.assertEquals(1, 1)
    @Test fun interfaceC() = Assert.assertEquals(1, 1)
}
