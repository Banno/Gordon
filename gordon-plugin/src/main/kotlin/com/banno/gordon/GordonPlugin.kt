package com.banno.gordon

import com.android.build.VariantOutput
import com.android.build.gradle.TestedExtension
import com.android.build.gradle.api.ApkVariant
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin.VERIFICATION_GROUP
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register

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

                this.instrumentationApk.apply { set(instrumentationApk) }.finalizeValue()
                this.applicationApk.apply { set(applicationApk) }.finalizeValue()
                this.instrumentationRunnerOptions.apply { set(instrumentationRunnerOptions) }.finalizeValue()
            }
        }
    }

    private fun ApkVariant.requireMainApkOutputFile() = outputs
        .singleOrNull { it.outputType == VariantOutput.MAIN || it.name.startsWith("universal-") }
        ?.outputFile
        ?: throw IllegalStateException("Gordon cannot be used without enabling universalApk in your abi splits")

    object NoTestInstrumentationRunnerException : IllegalStateException(
        "Gordon cannot be used without a testInstrumentationRunner, such as `androidx.test.runner.AndroidJUnitRunner`"
    )
}
