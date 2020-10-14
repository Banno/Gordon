package com.banno.android.gordontest.library

import org.junit.Assert
import org.junit.Ignore
import org.junit.Test

class LibraryTest {
    @Test fun libraryA() = Assert.assertEquals(1, 1)
    @Test fun libraryB() = Assert.assertEquals(1, 1)

    @Ignore
    @Test
    fun libraryIgnore() = Assert.assertEquals(1, 1)
}
