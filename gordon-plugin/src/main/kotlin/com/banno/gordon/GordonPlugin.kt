package com.banno.gordon

import com.android.build.gradle.TestedExtension
import com.android.build.gradle.api.ApkVariant
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin.VERIFICATION_GROUP
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import java.io.File

class GordonPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val androidPluginType = project.androidPluginType()

        project.extensions.create<GordonExtension>("gordon")

        val androidExtension = project.extensions.getByType<TestedExtension>()

        androidExtension.testVariants.all {
            val testVariant = this
            val variantTaskName = testVariant.name
                .capitalize()
                .replace(Regex("AndroidTest$"), "")
                .replace(Regex("Debug$"), "")

            project.tasks.register<GordonTestTask>("gordon$variantTaskName") {
                group = VERIFICATION_GROUP
                val variantDescription = variantTaskName.takeIf { it.isNotBlank() }?.let { " for $it" } ?: ""
                description = "Installs and runs instrumentation tests$variantDescription."

                val testedVariant = testVariant.testedVariant
                val instrumentationApk = testVariant.requireMainApkOutputFile()
                val applicationApk = when (androidPluginType) {
                    AndroidPluginType.LIBRARY -> instrumentationApk
                    AndroidPluginType.APP -> (testedVariant as ApkVariant).requireMainApkOutputFile()
                }

                val instrumentationRunnerOptions = InstrumentationRunnerOptions(
                    testInstrumentationRunner = testedVariant.mergedFlavor.testInstrumentationRunner
                        ?: throw NoTestInstrumentationRunnerException,
                    testInstrumentationRunnerArguments = testedVariant.mergedFlavor.testInstrumentationRunnerArguments,
                    animationsDisabled = androidExtension.testOptions.animationsDisabled
                )

                dependsOn(testVariant.assembleProvider, testedVariant.assembleProvider)

                this.instrumentationApk.apply { set(project.layout.file(instrumentationApk)) }.finalizeValue()
                this.applicationApk.apply { set(project.layout.file(applicationApk)) }.finalizeValue()
                this.instrumentationRunnerOptions.apply { set(instrumentationRunnerOptions) }.finalizeValue()
            }
        }
    }

    private fun ApkVariant.requireMainApkOutputFile() = packageApplicationProvider.map { packageAppTask ->
        val apkNames = packageAppTask.apkNames

        val apkName = apkNames.singleOrNull()
            ?: apkNames.singleOrNull { it.contains("universal-") }
            ?: throw IllegalStateException("Gordon cannot be used without enabling universalApk in your abi splits")

        File(packageAppTask.outputDirectory, apkName)
    }

    object NoTestInstrumentationRunnerException : IllegalStateException(
        "Gordon cannot be used without a testInstrumentationRunner, such as `androidx.test.runner.AndroidJUnitRunner`"
    )
}
