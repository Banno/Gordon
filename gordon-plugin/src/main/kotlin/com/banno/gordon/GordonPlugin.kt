package com.banno.gordon

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

                val (applicationApk, applicationPackage) = when (androidPluginType) {
                    AndroidPluginType.LIBRARY ->
                        null to null
                    AndroidPluginType.APP ->
                        (testedVariant as ApkVariant).requireMainApkOutputFile() to testedVariant.applicationId
                }

                val instrumentationApk = testVariant.requireMainApkOutputFile()
                val instrumentationPackage = testVariant.applicationId

                val instrumentationRunnerOptions = InstrumentationRunnerOptions(
                    testInstrumentationRunner = testedVariant.mergedFlavor.testInstrumentationRunner
                        ?: "android.test.InstrumentationTestRunner",
                    testInstrumentationRunnerArguments = testedVariant.mergedFlavor.testInstrumentationRunnerArguments,
                    animationsDisabled = androidExtension.testOptions.animationsDisabled
                )

                dependsOn(testVariant.assembleProvider, testedVariant.assembleProvider)

                if (applicationPackage != null && applicationApk != null) {
                    this.applicationApk.apply { set(applicationApk) }.finalizeValue()
                    this.applicationPackage.apply { set(applicationPackage) }.finalizeValue()
                }

                this.instrumentationApk.apply { set(instrumentationApk) }.finalizeValue()
                this.instrumentationPackage.apply { set(instrumentationPackage) }.finalizeValue()
                this.androidInstrumentationRunnerOptions.apply { set(instrumentationRunnerOptions) }.finalizeValue()
            }
        }
    }

    private fun ApkVariant.requireMainApkOutputFile() = packageApplicationProvider.flatMap { packageAppTask ->
        val apkNames = packageAppTask.apkNames

        val apkName = apkNames.singleOrNull()
            ?: apkNames.singleOrNull { it.contains("universal-") }
            ?: throw IllegalStateException("Gordon cannot be used without enabling universalApk in your abi splits")

        packageAppTask.outputDirectory.file(apkName)
    }
}
