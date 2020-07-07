package com.banno.android.gordontest

import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SmallTest
import androidx.test.filters.FlakyTest
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test

class AnnotatedTest {
    @LargeTest @Test fun annotatedA() = Assert.assertEquals(1, 1)

    @SmallTest @Test fun annotatedB() = Assert.assertEquals(1, 1)

    @LargeTest @FlakyTest @Test fun annotatedC() = Assert.assertEquals(1, 1)
}