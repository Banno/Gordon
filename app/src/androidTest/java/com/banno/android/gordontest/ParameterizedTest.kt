package com.banno.android.gordontest

import org.junit.Assert
import org.junit.Assume
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ParameterizedTest(private val parameter: Any) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun parameters(): Iterable<Array<Any>> {
            return listOf(
                arrayOf(1 as Any),
                arrayOf("arg" as Any)
            )
        }
    }

    @Test fun parameterizedA() = Assert.assertEquals(1, 1)
    @Test fun parameterizedB() = Assume.assumeTrue(1 == parameter)
    @Test fun parameterizedC() = Assert.assertTrue(1 == parameter)

    @Ignore
    @Test
    fun parameterizedD() = Assert.assertEquals(1, 1)
}
