package com.banno.android.gordontest

import org.junit.Assert
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

    @Test fun parameterizedPass() = Assert.assertEquals(1, 1)
    @Test fun parameterizedFail() = Assert.assertEquals(1, parameter)

    @Ignore
    @Test
    fun parameterizedIgnore() = Assert.assertEquals(1, 1)
}
