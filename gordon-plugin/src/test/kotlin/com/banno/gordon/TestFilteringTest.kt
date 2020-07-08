package com.banno.gordon

import org.junit.Test

class TestFilteringTest {

    @Test
    fun shouldMatchOnClassName() {
        TestCase("com.banno.android.gordontest.TestClassName", "methodName", false)
            .matchesFilter(listOf("TestClassName")) shouldEqual true
    }

    @Test
    fun shouldMatchOnMethodName() {
        TestCase("com.banno.android.gordontest.TestClassName", "methodName", false)
            .matchesFilter(listOf("methodName")) shouldEqual true
    }

    @Test
    fun shouldMatchOnClassMethod() {
        TestCase("com.banno.android.gordontest.TestClassName", "methodName", false)
            .matchesFilter(listOf("TestClassName.methodName")) shouldEqual true
    }

    @Test
    fun shouldMatchOnPackageName() {
        TestCase("com.banno.android.gordontest.TestClassName", "methodName", false)
            .matchesFilter(listOf("com.banno.android.gordontest")) shouldEqual true
    }

    @Test
    fun shouldMatchOnAnnotationName() {
        TestCase("com.banno.android.gordontest.TestClassName", "methodName", false, setOf("com.banno.CustomAnnotation"))
            .matchesFilter(listOf("com.banno.CustomAnnotation")) shouldEqual true
    }

    @Test
    fun shouldMatchIfAnyOfTheFiltersMatch() {
        TestCase("com.banno.android.gordontest.TestClassName", "methodName", false)
            .matchesFilter(listOf("com.banno.android.gordontest", "OtherTest")) shouldEqual true
    }

    @Test
    fun shouldNotMatchIfClassNameDoesNotMatchFully() {
        TestCase("com.banno.android.gordontest.TestClassName", "methodName", false)
            .matchesFilter(listOf("TestClass")) shouldEqual false
    }

    @Test
    fun shouldNotMatchPartialPackage() {
        TestCase("com.banno.android.gordontest.TestClassName", "methodName", false)
            .matchesFilter(listOf("banno.android")) shouldEqual false
    }

    @Test
    fun shouldNotMatchWithoutPeriodForClassMethod() {
        TestCase("com.banno.android.gordontest.TestClassName", "methodName", false)
            .matchesFilter(listOf("TestClassNamemethodName")) shouldEqual false
    }

    @Test
    fun shouldNotMatchEndingsOfClassNames() {
        TestCase("com.banno.android.gordontest.TestClassName", "methodName", false)
            .matchesFilter(listOf("ClassName")) shouldEqual false
    }

    @Test
    fun shouldNotMatchPartialAnnotationNames() {
        TestCase("com.banno.android.gordontest.TestClassName", "methodName", false, setOf("com.banno.CustomAnnotation"))
            .matchesFilter(listOf("CustomAnnotation")) shouldEqual false
    }
}
