package com.banno.gordon

import kotlinx.html.* // ktlint-disable no-wildcard-imports
import kotlinx.html.stream.appendHTML

internal sealed class TestResult {
    data class Passed(val duration: Float?) : TestResult()

    object NotRun : TestResult()

    object Ignored : TestResult()

    data class Failed(val failures: List<Failure>) : TestResult() {
        constructor(duration: Float?, shellOutput: String, failedOnSerial: String) : this(
            listOf(
                Failure(
                    duration = duration,
                    shellOutput = shellOutput,
                    deviceSerial = failedOnSerial
                )
            )
        )
    }

    data class Flaky(val passedDuration: Float?, val failures: List<Failure>) : TestResult()
}

data class Failure(val duration: Float?, val shellOutput: String, val deviceSerial: String)

internal inline fun <reified T : TestResult> Map<TestCase, TestResult>.filterResultType(): Map<TestCase, T> {
    return filterValues { it is T }
        .mapValues { it.value as T }
}

internal fun Map<PoolName, Map<TestCase, TestResult>>.getTestCasesByResult(predicate: (TestResult) -> Boolean): Map<PoolName, List<TestCase>> =
    mapValues {
        it.value.mapNotNull { (testCase, testResult) -> testCase.takeIf { predicate(testResult) } }
    }.filterValues { it.isNotEmpty() }

internal fun Map<PoolName, Map<TestCase, TestResult>>.summary(): String {
    val allResults = flatMap { it.value.values }

    return listOfNotNull(
        "Test Results",
        "----------------",
        allResults.count { it is TestResult.Passed }.let { "Passed: $it" },
        allResults.count { it is TestResult.Ignored }.takeIf { it > 0 }?.let { "Ignored: $it" },
        allResults.count { it is TestResult.Flaky }.takeIf { it > 0 }?.let { "Flaky: $it" },
        allResults.count { it is TestResult.NotRun }.takeIf { it > 0 }?.let { "Unable to run: $it" },
        allResults.count { it is TestResult.Failed }.takeIf { it > 0 }?.let { "Failed: $it" },
        *mapValues { poolResults -> poolResults.value.filterValues { it is TestResult.NotRun || it is TestResult.Failed } }
            .flatMap { (poolName, poolResults) ->
                poolResults.map {
                    "$poolName: ${it.key.fullyQualifiedClassName.substringAfterLast('.')}.${it.key.methodName}"
                }
            }
            .toTypedArray()
    ).joinToString("\n")
}

internal fun Map<PoolName, Map<TestCase, TestResult>>.junitReports() =
    flatMap { (poolName, results) ->
        results.map { (testCase, result) ->
            val fileContent =
                xmlDocument("testsuite") {
                    attribute("name", poolName)
                    attribute("tests", "1")
                    attribute("skipped", if (result is TestResult.Ignored) "1" else "0")
                    attribute("errors", if (result is TestResult.NotRun) "1" else "0")
                    attribute("failures", if (result is TestResult.Failed) "1" else "0")
                    element("testcase") {
                        attribute("name", testCase.methodName)
                        attribute("classname", testCase.fullyQualifiedClassName)
                        result.duration()?.let { attribute("time", it.toString()) }

                        when (result) {
                            is TestResult.Ignored -> element("skipped")
                            is TestResult.NotRun -> element("error", "Unable to run test")
                            is TestResult.Failed -> element(
                                "failure",
                                result.failures.concatFailures()
                            )
                            is TestResult.Flaky -> element(
                                "system-err",
                                result.failures.concatFailures()
                            )
                            is TestResult.Passed -> Unit
                        }
                    }
                }

            ReportFile(
                "$poolName-${testCase.fullyQualifiedClassName}.${testCase.methodName}.xml",
                fileContent
            )
        }
    }

private fun List<Failure>.concatFailures(): String = mapIndexed { index, it ->
    "\n\nFailure ${index + 1}\nDevice: ${it.deviceSerial}\n${it.shellOutput}"
}
    .joinToString(separator = "\n")

private fun TestResult.duration(): Float? = when (this) {
    is TestResult.Passed -> duration
    is TestResult.Failed -> failures.mapNotNull { it.duration }.sum()
    is TestResult.Flaky -> failures.mapNotNull { it.duration }.sum() + (passedDuration ?: 0f)
    is TestResult.NotRun,
    is TestResult.Ignored -> null
}

internal fun Map<PoolName, Map<TestCase, TestResult>>.htmlReport(): ReportFile {
    val builder = StringBuilder().appendHTML().html {
        head {
            addCSS()
            addTabsJavascript()
        }
        body {
            tabs(keys)

            forEach { (poolName, results) ->
                tabContent(poolName) {
                    val failed = results.filterResultType<TestResult.Failed>()
                    if (failed.isNotEmpty()) {
                        div(classes = "outputsection") {
                            resultSectionTitle("Failed")
                            article {
                                failed.forEach { (testCase, result) ->
                                    button(classes = "failure collapsible") { +testCase.title }
                                    div(classes = "content") { addFormattedShellOutput(result.failures) }
                                }
                            }
                        }
                    }

                    val flaky = results.filterResultType<TestResult.Flaky>()
                    if (flaky.isNotEmpty()) {
                        div(classes = "outputsection") {
                            resultSectionTitle("Flaky (Failed and then Passed)")
                            article {
                                flaky.forEach { (testCase, result) ->
                                    button(classes = "flaky collapsible") { +testCase.title }
                                    div(classes = "content") { addFormattedShellOutput(result.failures) }
                                }
                            }
                        }
                    }

                    printResultSection(
                        "Unable to run",
                        results.filterResultType<TestResult.NotRun>().keys
                    )

                    printResultSection(
                        "Ignored",
                        results.filterResultType<TestResult.Ignored>().keys
                    )

                    printResultSection("Passed", results.filterResultType<TestResult.Passed>().keys)
                }
            }

            addCollapsibleJavascript()

            openDefaultTab()
        }
    }

    return ReportFile("test-report.html", builder.toString())
}

private fun FlowContent.tabs(poolNames: Set<String>) {
    header {
        div(classes = "tab") {
            poolNames.forEachIndexed { index, poolName ->
                button(classes = "tablinks") {
                    id = if (index == 0) "defaultOpen" else "$poolName-tab"
                    onClick = "switchTab(event, '$poolName')"
                    +poolName
                }
            }
        }
    }
}

private fun FlowContent.tabContent(poolName: String, content: DIV.() -> Unit) {
    div(classes = "tabcontent") {
        id = poolName
        poolTitle("$poolName Test Results")
        br
        content()
    }
}

private fun FlowContent.openDefaultTab() {
    script(type = ScriptType.textJavaScript) {
        unsafe { raw("""document.getElementById("defaultOpen").click();""") }
    }
}

private fun FlowContent.addCollapsibleJavascript() {
    script(type = ScriptType.textJavaScript) {
        unsafe {
            raw(
                """
                var coll = document.getElementsByClassName("collapsible");
                var i;
                    
                for (i = 0; i < coll.length; i++) {
                    coll[i].addEventListener("click", function() {
                        this.classList.toggle("active");
                        var content = this.nextElementSibling;
                        if (content.style.maxHeight){
                            content.style.maxHeight = null;
                        } else {
                            content.style.maxHeight = content.scrollHeight + "px";
                        }
                    });
                }
                """.trimIndent()
            )
        }
    }
}

private fun HEAD.addTabsJavascript() {
    script(type = ScriptType.textJavaScript) {
        unsafe {
            raw(
                """
                function switchTab(evt, poolName) {
                    var i, tabcontent, tablinks;
                    
                    tabcontent = document.getElementsByClassName("tabcontent");
                    for (i = 0; i < tabcontent.length; i++) {
                        tabcontent[i].style.display = "none";
                    }
                    
                    tablinks = document.getElementsByClassName("tablinks");
                    for (i = 0; i < tablinks.length; i++) {
                        tablinks[i].className = tablinks[i].className.replace(" active", "");
                    }
                    
                    document.getElementById(poolName).style.display = "block";
                    evt.currentTarget.className += " active";
                }
                """.trimIndent()
            )
        }
    }
}

private fun HEAD.addCSS() {
    style {
        unsafe {
            raw(
                """
                     body {
                         --primaryAppColor: #3aaeda;
                         --dangerAppColor: #f44336;
                         --warningAppColor: #ffaa71;
                         font-family: -apple-system,system-ui,"Segoe UI",Roboto,"Helvetica Neue",sans-serif;
                         color: #6b757b;
                         background-color: #eef1f4;
                         margin: 0;
                     }

                     @keyframes fadeEffect {
                         from {opacity: 0;}
                         to {opacity: 1;}
                     }

                     body > header {
                         background-color: var(--primaryAppColor);
                         margin-bottom: 16px;
                     }

                     .tab button {
                         display: inline-block;
                         font-size: 15px;
                         line-height: 62px;
                         color: rgba(255,255,255,0.7);
                         background-color: transparent;
                         cursor: pointer;
                         z-index: 1;
                         padding: 3px 32px;
                     }

                     .tab button:hover,
                     .tab button.active {
                         color: #ffffff;
                     }

                     .tab button.active {
                         font-weight: 600;
                     }

                     .tabcontent {
                         display: none;
                         padding: 0 24px 24px;
                     }

                     h1 {
                         color: #455564;
                         font-size: 24px;
                         line-height: 36px;
                         font-weight: 500;
                     }

                     .tabcontent > div {
                         background-color: #ffffff;
                         box-shadow: 0 1px 1px 0 rgba(0,0,0,.24),0 1px 4px rgba(0,0,0,.12);
                         border-radius: 2px;
                     }

                     .outputsection {
                         margin-bottom: 32px;
                     }

                     .listedoutputsection article p {
                         margin-top: 0;
                     }

                     div > header {
                         font-size: 20px;
                         color: #455564;
                         padding: 24px 24px 16px 0;
                         margin-left: 24px;
                         line-height: 21px;
                         border-bottom: 1px solid #e4e7ea;
                     }

                     div > article {
                         padding: 16px 24px;
                     }

                     button {
                         -webkit-appearance: none;
                         -moz-appearance: none;
                         appearance: none;
                         border: 1px solid transparent;
                     }

                     .collapsible {
                         display: flex;
                         align-items: center;
                         width: 100%;
                         font-size: 15px;
                         color: #ffffff;
                         cursor: pointer;
                         padding: 16px;
                         border-bottom-color: #ffffff;
                     }

                     .failure {
                         background-color: var(--dangerAppColor);
                     }

                     .flaky {
                         background-color: var(--warningAppColor);
                     }

                     .collapsible::after {
                         content: '+';
                         font-size: 13px;
                         font-weight: 600;
                         color: #ffffff;
                         margin: 0 8px 0 auto;
                     }

                     .collapsible.active::after {
                         content: '-';
                     }

                     .collapsible:hover {
                         background-color: #ffffff;
                     }

                     .failure:hover {
                         color: var(--dangerAppColor);
                         border: 1px solid var(--dangerAppColor);
                     }

                     .flaky:hover {
                         color: var(--warningAppColor);
                         border: 1px solid var(--warningAppColor);
                     }

                     .failure:hover::after {
                         color: var(--dangerAppColor);
                     }

                     .flaky:hover::after {
                         color: var(--warningAppColor);
                     }

                     .content {
                         padding: 0 16px;
                         max-height: 0;
                         overflow: hidden;
                         transition: max-height 0.2s ease-out;
                     }

                     p {
                         font-size: 14px;
                         line-height: 22px;
                     }
                """.trimIndent()
            )
        }
    }
}

private val TestCase.title: String
    get() = "$fullyQualifiedClassName.$methodName"

private fun FlowContent.poolTitle(poolName: String) {
    h1 { +poolName }
}

private fun FlowContent.resultSectionTitle(sectionName: String) {
    header { +sectionName }
}

private fun FlowContent.addFormattedShellOutput(failures: List<Failure>) {
    failures.forEachIndexed { index, it ->
        br
        br

        p { +"Failure ${index + 1}" }

        p { +"Device: ${it.deviceSerial}" }

        it.shellOutput.split("\n\n")
            .map {
                p {
                    it.split("\n")
                        .forEach {
                            +it
                            br
                        }
                }
            }
    }
}

private fun FlowContent.printResultSection(header: String, tests: Set<TestCase>) {
    if (tests.isNotEmpty()) {
        div {
            classes = setOf("outputsection", "listedoutputsection")
            resultSectionTitle(header)
            article {
                p {
                    tests.forEach {
                        +it.title
                        br
                    }
                }
            }
        }
    }
}
