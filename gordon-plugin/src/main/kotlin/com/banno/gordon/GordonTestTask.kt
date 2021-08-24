package com.banno.gordon

import arrow.core.Either
import arrow.core.computations.either
import arrow.core.left
import arrow.core.right
import com.android.tools.build.bundletool.device.Device
import kotlinx.coroutines.Dispatchers
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.LogLevel
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.kotlin.dsl.property
import java.io.File
import javax.inject.Inject

@CacheableTask
internal abstract class GordonTestTask @Inject constructor(
    objects: ObjectFactory,
    projectLayout: ProjectLayout
) : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    internal val instrumentationApkDir: DirectoryProperty = objects.directoryProperty()

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    internal val applicationAab: RegularFileProperty = objects.fileProperty()

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    internal val signingKeystoreFile: RegularFileProperty = objects.fileProperty()

    @get:Input
    internal val signingConfigCredentials: Property<SigningConfigCredentials> = objects.property()

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    internal val dynamicFeatureModuleManifest: RegularFileProperty = objects.fileProperty()

    @get:Input
    internal val dynamicFeatureModuleName: Property<String> = objects.property()

    @get:Input
    internal val applicationPackage: Property<String> = objects.property()

    @get:Input
    internal val instrumentationPackage: Property<String> = objects.property()

    @get:Input
    internal val poolingStrategy: Property<PoolingStrategy> = objects.property()

    @get:Input
    internal val tabletShortestWidthDp: Property<Int> = objects.property()

    @get:Internal
    internal val ignoreProblematicDevices: Property<Boolean> = objects.property()

    @get:Internal
    internal val retryQuota: Property<Int> = objects.property()

    @get:Internal
    internal val installTimeoutMillis: Property<Long> = objects.property()

    @get:Internal
    internal val testTimeoutMillis: Property<Long> = objects.property()

    @get:Internal
    internal val extensionTestFilter: Property<String> = objects.property()

    @get:Internal
    internal val extensionTestInstrumentationRunner: Property<String> = objects.property()

    @get:Internal
    @Option(option = "tests", description = "Comma-separated packages, classes, methods, or annotations.")
    val commandlineTestFilter: Property<String> = objects.property()

    @get:Internal
    internal val androidInstrumentationRunnerOptions: Property<InstrumentationRunnerOptions> = objects.property()

    @get:Input
    internal val testFilters: Provider<List<String>> =
        commandlineTestFilter.zip(extensionTestFilter) { commandlineTestFilter, extensionTestFilter ->
            (commandlineTestFilter.takeIf { it.isNotBlank() } ?: extensionTestFilter)
                .split(',')
                .filter { it.isNotBlank() }
                .map { it.replace('#', '.') }
        }

    @get:Input
    internal val instrumentationRunnerOptions: Provider<InstrumentationRunnerOptions> =
        androidInstrumentationRunnerOptions.zip(extensionTestInstrumentationRunner) { options, extensionRunner ->
            if (extensionRunner.isNotBlank()) options.copy(testInstrumentationRunner = extensionRunner)
            else options
        }

    @OutputDirectory
    val testResultsDirectory: Provider<Directory> = projectLayout.buildDirectory.dir("test-results/$name")

    @OutputDirectory
    val reportDirectory: Provider<Directory> = projectLayout.buildDirectory.dir("reports/$name")

    @get:Internal
    internal val rootProjectBuildDirectory: DirectoryProperty = objects.directoryProperty()

    @OutputDirectory
    @Suppress("unused") // Declare the same output dir on all Gordon tasks so that Gradle won't try to run Gordon for multiple modules in parallel
    val rootBuildGordonDirectory: Provider<Directory> = rootProjectBuildDirectory.dir("gordon")

    init {
        applicationAab.convention { PLACEHOLDER_APPLICATION_AAB }
        signingKeystoreFile.convention { PLACEHOLDER_SIGNING_KEYSTORE }
        dynamicFeatureModuleManifest.convention { PLACEHOLDER_DYNAMIC_MODULE_MANIFEST }
        dynamicFeatureModuleName.convention(PLACEHOLDER_DYNAMIC_MODULE_NAME)
        applicationPackage.convention(PLACEHOLDER_APPLICATION_PACKAGE)
        commandlineTestFilter.convention("")
    }

    @TaskAction
    private fun runTests() {
        runTestsCatching().fold({ throw it }, {})
    }

    private fun runTestsCatching(): Either<Throwable, Unit> {
        val instrumentationApk = instrumentationApkDir.asFileTree.single { it.extension == "apk" }

        return either.eager {
            testResultsDirectory.get().asFile.clear().bind()
            reportDirectory.get().asFile.clear().bind()

            val adb = initializeDefaultAdbServer().bind()
            val problematicDevices = mutableListOf<Device>()
            val originalPools = calculatePools(
                adb,
                poolingStrategy.get(),
                tabletShortestWidthDp.get().takeIf { it > -1 }
            ).bind()
            val testCases = loadTestSuite(instrumentationApk).bind()
                .filter { it.matchesFilter(testFilters.get()) }

            testCases.validateTestCases().bind()
            originalPools.validateDevicePools().bind()

            val applicationAab = applicationAab.get().asFile.takeUnless { it == PLACEHOLDER_APPLICATION_AAB }
            val applicationPackage = applicationPackage.get().takeUnless { it == PLACEHOLDER_APPLICATION_PACKAGE }
            val onDemandDynamicModuleName = dynamicFeatureModuleName.get().takeUnless { it == PLACEHOLDER_DYNAMIC_MODULE_NAME }
                .takeIf { dynamicFeatureModuleManifest.get().asFile.readText().contains("dist:on-demand") }

            val signingConfig = SigningConfig(
                storeFile = signingKeystoreFile.get().asFile.takeUnless { it == PLACEHOLDER_SIGNING_KEYSTORE },
                storePassword = signingConfigCredentials.get().storePassword,
                keyAlias = signingConfigCredentials.get().keyAlias,
                keyPassword = signingConfigCredentials.get().keyPassword
            )

            originalPools.flatMap { it.devices }.reinstall(
                dispatcher = Dispatchers.Default,
                logger = logger,
                applicationPackage = applicationPackage,
                instrumentationPackage = instrumentationPackage.get(),
                onDemandDynamicModuleName = onDemandDynamicModuleName,
                applicationAab = applicationAab,
                signingConfig = signingConfig,
                instrumentationApk = instrumentationApk,
                installTimeoutMillis = installTimeoutMillis.get(),
                adb = adb,
                ignoreProblematicDevices = ignoreProblematicDevices.get(),
                problematicDevices = problematicDevices,
            ).bind()

            val pools = originalPools.filterProblematicDevices(problematicDevices)
            pools.validateDevicePools().bind()

            val testResults = runAllTests(
                dispatcher = Dispatchers.Default,
                logger = logger,
                instrumentationPackage = instrumentationPackage.get(),
                instrumentationRunnerOptions = instrumentationRunnerOptions.get(),
                allTestCases = testCases,
                allPools = pools,
                retryQuota = retryQuota.get(),
                testTimeoutMillis = testTimeoutMillis.get()
            ).bind()

            testResults.junitReports().write(testResultsDirectory.get().asFile).bind()

            val htmlReportPath = testResults.htmlReport().write(reportDirectory.get().asFile).bind()

            pools.flatMap { it.devices }.safeUninstall(
                dispatcher = Dispatchers.Default,
                timeoutMillis = installTimeoutMillis.get(),
                applicationPackage = applicationPackage,
                instrumentationPackage = instrumentationPackage.get()
            )

            val testRunFailed =
                testResults.getTestCasesByResult { it is TestResult.Failed || it is TestResult.NotRun }.isNotEmpty()

            val summaryLogLevel = when {
                testRunFailed -> LogLevel.ERROR
                testResults.getTestCasesByResult { it is TestResult.Flaky }.isNotEmpty() -> LogLevel.WARN
                else -> LogLevel.LIFECYCLE
            }

            logger.log(summaryLogLevel, testResults.summary())

            if (testRunFailed) {
                RuntimeException("Test run failed. See the report at: file://$htmlReportPath").left().bind<Unit>()
            }
        }
    }
}

internal fun List<DevicePool>.validateDevicePools() =
    when {
        isEmpty() -> IllegalStateException("No devices found").left()
        any { it.devices.isEmpty() } -> {
            val emptyPools = filter { it.devices.isEmpty() }.map { it.poolName }
            IllegalStateException("No devices found in pools $emptyPools").left()
        }
        else -> Unit.right()
    }

internal fun List<TestCase>.validateTestCases() =
    when {
        isEmpty() -> IllegalStateException("No test cases found").left()
        else -> Unit.right()
    }

internal fun List<DevicePool>.filterProblematicDevices(problematicDevices: List<Device>) =
    map { pool ->
        pool.copy(devices = pool.devices - problematicDevices)
    }

internal fun TestCase.matchesFilter(filters: List<String>): Boolean {
    val fullyQualifiedTestMethod = "$fullyQualifiedClassName.$methodName"

    return filters.isEmpty() || annotations.intersect(filters).isNotEmpty() || filters.any {
        val filter = it.split('.')

        fullyQualifiedTestMethod.startsWith(it) ||
            fullyQualifiedTestMethod.split('.').takeLast(filter.size) == filter ||
            fullyQualifiedClassName.split('.').takeLast(filter.size) == filter
    }
}

private val PLACEHOLDER_APPLICATION_AAB = File.createTempFile("PLACEHOLDER_APPLICATION_AAB", null)
private val PLACEHOLDER_SIGNING_KEYSTORE = File.createTempFile("PLACEHOLDER_SIGNING_KEYSTORE", null)
private val PLACEHOLDER_DYNAMIC_MODULE_MANIFEST = File.createTempFile("PLACEHOLDER_DYNAMIC_MODULE_MANIFEST", null)
private const val PLACEHOLDER_DYNAMIC_MODULE_NAME = "PLACEHOLDER_DYNAMIC_MODULE_NAME"
private const val PLACEHOLDER_APPLICATION_PACKAGE = "PLACEHOLDER_APPLICATION_PACKAGE"
