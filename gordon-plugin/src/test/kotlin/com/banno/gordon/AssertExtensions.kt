package com.banno.gordon

import kotlin.test.assertEquals

infix fun <T> T.shouldEqual(expected: T) = assertEquals(expected, this)
