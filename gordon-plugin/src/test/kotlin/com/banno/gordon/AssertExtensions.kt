package com.banno.gordon

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.hasItem
import org.hamcrest.CoreMatchers.not
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.mockito.kotlin.KStubbing
import org.mockito.stubbing.OngoingStubbing

infix fun <T> T.shouldEqual(expected: T) = assertThat(this, equalTo(expected))

infix fun <T> T.shouldNotEqual(expected: T) = assertThat(this, not(equalTo(expected)))

infix fun <T> List<T>.shouldContain(expected: T) = assertThat(this, hasItem(expected))

infix fun <T> List<T>.shouldNotContain(expected: T) = assertThat(this, not(hasItem(expected)))

fun <T> List<T>.assertNotEmpty() = this.size shouldNotEqual 0

fun Any?.assertNull() = assertThat(this, nullValue())

fun Any?.assertNotNull() = assertThat(this, notNullValue())

fun Boolean.assertTrue() = assertThat(this, equalTo(true))

fun Boolean.assertFalse() = assertThat(this, equalTo(false))

inline fun <reified T : Any> forMock(
    mock: T,
    stubbing: KStubbing<T>.(T) -> Unit
): T = mock.apply {
    KStubbing(this).stubbing(this)
}

fun <T> OngoingStubbing<T>.doReturnNull(): OngoingStubbing<T> = thenReturn(null)
