package com.banno.gordon

import arrow.effects.extensions.io.fx.fx
import kotlinx.coroutines.Dispatchers
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.property
import se.vidstige.jadb.JadbConnection

@CacheableTask
internal abstract class GordonTestTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    internal val instrumentationApk: RegularFileProperty = project.objects.fileProperty()

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    internal val applicationApk: RegularFileProperty = project.objects.fileProperty()

    @get:Input
    internal val instrumentationRunnerOptions: Property<InstrumentationRunnerOptions> = project.objects.property()

    @get:Input
    internal val testFilters: List<String>
        get() = (commandlineTestFilter.get().takeIf { it.isNotBlank() } ?: extensionTestFilter.get())
            .split(',')
            .filter { it.isNotBlank() }
            .map { it.replace('#', '.') }

    @get:Input
    internal val poolingStrategy = project.extensions.getByType<GordonExtension>().poolingStrategy

    private val retryQuota = project.extensions.getByType<GordonExtension>().retryQuota
    private val testTimeoutMillis = project.extensions.getByType<GordonExtension>().testTimeoutMillis

    @Option(option = "tests", description = "Comma-separated packages, classes, or methods.")
    val commandlineTestFilter: Property<String> = project.objects.property()

    private val extensionTestFilter = project.extensions.getByType<GordonExtension>().testFilter

    @OutputDirectory
    val testResultsDirectory: Provider<Directory> = project.layout.buildDirectory.dir("test-results/$name")

    @OutputDirectory
    val reportDirectory: Provider<Directory> = project.layout.buildDirectory.dir("reports/$name")

    init {
        commandlineTestFilter.convention("")
    }

    @TaskAction
    private fun runTests() {
        val runTests = fx {
            testResultsDirectory.get().asFile.clear().bind()
            reportDirectory.get().asFile.clear().bind()

            val applicationPackage = getManifestPackage(applicationApk.get().asFile).bind()
            val instrumentationPackage = getManifestPackage(instrumentationApk.get().asFile).bind()
            val pools = calculatePools(JadbConnection(), poolingStrategy.get()).bind()
            val testCases = loadTestSuite(instrumentationApk.get().asFile).bind()
                .filter { it.matchesFilter(testFilters) }

            when {
                testCases.isEmpty() -> raiseError<Unit>(IllegalStateException("No test cases found")).bind()
                pools.isEmpty() -> raiseError<Unit>(IllegalStateException("No devices found")).bind()
                pools.any { it.devices.isEmpty() } -> {
                    val emptyPools = pools.filter { it.devices.isEmpty() }.map { it.poolName }
                    raiseError<Unit>(IllegalStateException("No devices found in pools $emptyPools")).bind()
                }
            }

            pools.flatMap { it.devices }.reinstall(
                dispatcher = Dispatchers.Default,
                logger = logger,
                applicationPackage = applicationPackage,
                instrumentationPackage = instrumentationPackage,
                applicationApk = applicationApk.get().asFile,
                instrumentationApk = instrumentationApk.get().asFile
            ).bind()

            val testResults = runAllTests(
                dispatcher = Dispatchers.Default,
                logger = logger,
                instrumentationPackage = instrumentationPackage,
                instrumentationRunnerOptions = instrumentationRunnerOptions.get(),
                allTestCases = testCases,
                allPools = pools,
                retryQuota = retryQuota.get(),
                testTimeoutMillis = testTimeoutMillis.get()
            ).bind()

            testResults.junitReports().write(testResultsDirectory.get().asFile).bind()

            val htmlReportPath = testResults.htmlReport().write(reportDirectory.get().asFile).bind()

            pools.flatMap { it.devices }.uninstall(
                dispatcher = Dispatchers.Default,
                applicationPackage = applicationPackage,
                instrumentationPackage = instrumentationPackage
            ).bind()

            val testRunFailed =
                testResults.getTestCasesByResult { it is TestResult.Failed || it is TestResult.NotRun }.isNotEmpty()

            val summaryLogLevel = when {
                testRunFailed -> LogLevel.ERROR
                testResults.getTestCasesByResult { it is TestResult.Flaky }.isNotEmpty() -> LogLevel.WARN
                else -> LogLevel.LIFECYCLE
            }

            logger.log(summaryLogLevel, testResults.summary())

            if (testRunFailed) {
                raiseError<Unit>(RuntimeException("Test run failed. See the report at: file://$htmlReportPath")).bind()
            }
        }

        runTests.unsafeRunSync()
    }
}

internal fun TestCase.matchesFilter(filters: List<String>): Boolean {
    val fullyQualifiedTestMethod = "$fullyQualifiedClassName.$methodName"

    return filters.isEmpty() || filters.any {
        val filter = it.split('.')

        fullyQualifiedTestMethod.startsWith(it) ||
                fullyQualifiedTestMethod.split('.').takeLast(filter.size) == filter ||
                fullyQualifiedClassName.split('.').takeLast(filter.size) == filter
    }
}
