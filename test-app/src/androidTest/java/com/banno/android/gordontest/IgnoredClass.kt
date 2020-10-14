package com.banno.android.gordontest

import org.junit.Assert
import org.junit.Ignore
import org.junit.Test

@Ignore
class IgnoredClass {
    @Test fun a() = Assert.assertEquals(1, 1)
    @Test fun b() = Assert.assertEquals(1, 1)
    @Test fun c() = Assert.assertEquals(1, 1)
}
