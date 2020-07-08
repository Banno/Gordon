package com.banno.android.gordontest

import androidx.test.filters.FlakyTest
import androidx.test.filters.LargeTest
import androidx.test.filters.SmallTest
import org.junit.Assert
import org.junit.Test

class AnnotatedTest {
    @LargeTest @Test fun annotatedA() = Assert.assertEquals(1, 1)

    @SmallTest @Test fun annotatedB() = Assert.assertEquals(1, 1)

    @LargeTest @FlakyTest @Test fun annotatedC() = Assert.assertEquals(1, 1)
}

@FlakyTest
class AnnotatedTestClass {
    @Test fun annotatedA() = Assert.assertEquals(1, 1)

    @SmallTest @Test fun annotatedB() = Assert.assertEquals(1, 1)
}
