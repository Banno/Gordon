package com.banno.android.gordontest.feature

import org.junit.Assert
import org.junit.Ignore
import org.junit.Test

class FeatureTest {
    @Test fun featureA() = Assert.assertEquals(1, 1)
    @Test fun featureB() = Assert.assertEquals(1, 1)

    @Ignore
    @Test
    fun featureIgnore() = Assert.assertEquals(1, 1)
}
