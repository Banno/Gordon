package com.banno.android.gordontest

import org.junit.Assert
import org.junit.Test

class ClassOne {
    @Test fun a1() = Assert.assertEquals(1, 1)
    @Test fun b1() = Assert.assertEquals(1, 1)
    @Test fun c1() = Assert.assertEquals(1, 1)
}

class ClassTwo {
    @Test fun a2() = Assert.assertEquals(1, 1)
    @Test fun b2() = Assert.assertEquals(1, 1)
    @Test fun c2() = Assert.assertEquals(1, 1)
}
