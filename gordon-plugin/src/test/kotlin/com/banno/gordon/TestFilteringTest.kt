package com.banno.gordon

import org.junit.Test

class TestFilteringTest {

    @Test
    fun shouldMatchOnClassName() {
        TestCase("com.banno.grip.accounts.AccountChainFragmentTest", "methodName", false)
            .matchesFilter(listOf("AccountChainFragmentTest")) shouldEqual true
    }

    @Test
    fun shouldMatchOnMethodName() {
        TestCase("com.banno.grip.accounts.AccountChainFragmentTest", "methodName", false)
            .matchesFilter(listOf("methodName")) shouldEqual true
    }

    @Test
    fun shouldMatchOnClassMethod() {
        TestCase("com.banno.grip.accounts.AccountChainFragmentTest", "methodName", false)
            .matchesFilter(listOf("AccountChainFragmentTest.methodName")) shouldEqual true
    }

    @Test
    fun shouldMatchOnPackageName() {
        TestCase("com.banno.grip.accounts.AccountChainFragmentTest", "methodName", false)
            .matchesFilter(listOf("com.banno.grip.accounts")) shouldEqual true
    }

    @Test
    fun shouldMatchOnAnnotationName() {
        TestCase("com.banno.grip.accounts.AccountChainFragmentTest", "methodName", false, setOf("com.banno.CustomAnnotation"))
            .matchesFilter(listOf("com.banno.CustomAnnotation")) shouldEqual true
    }

    @Test
    fun shouldMatchIfAnyOfTheFiltersMatch() {
        TestCase("com.banno.grip.accounts.AccountChainFragmentTest", "methodName", false)
            .matchesFilter(listOf("com.banno.grip.accounts", "OtherTest")) shouldEqual true
    }

    @Test
    fun shouldNotMatchIfClassNameDoesNotMatchFully() {
        TestCase("com.banno.grip.accounts.AccountChainFragmentTest", "methodName", false)
            .matchesFilter(listOf("AccountChainFragment")) shouldEqual false
    }

    @Test
    fun shouldNotMatchPartialPackage() {
        TestCase("com.banno.grip.accounts.AccountChainFragmentTest", "methodName", false)
            .matchesFilter(listOf("banno.grip")) shouldEqual false
    }

    @Test
    fun shouldNotMatchWithoutPeriodForClassMethod() {
        TestCase("com.banno.grip.accounts.AccountChainFragmentTest", "methodName", false)
            .matchesFilter(listOf("AccountChainFragmentTestmethodName")) shouldEqual false
    }

    @Test
    fun shouldNotMatchEndingsOfClassNames() {
        TestCase("com.banno.grip.accounts.AccountChainFragmentTest", "methodName", false)
            .matchesFilter(listOf("FragmentTest")) shouldEqual false
    }

    @Test
    fun shouldNotMatchPartialAnnotationNames() {
        TestCase("com.banno.grip.accounts.AccountChainFragmentTest", "methodName", false, setOf("com.banno.CustomAnnotation"))
            .matchesFilter(listOf("CustomAnnotation")) shouldEqual false
    }
}