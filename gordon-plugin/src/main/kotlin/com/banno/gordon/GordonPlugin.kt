package com.banno.gordon

import com.android.build.gradle.TestedExtension
import com.android.build.gradle.api.ApkVariant
import com.android.build.gradle.api.TestVariant
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

                val (applicationAab, applicationPackage) = when (androidPluginType) {
                    AndroidPluginType.LIBRARY ->
                        null to null
                    AndroidPluginType.APP -> {
                        dependsOn(project.tasks.named("bundle${testedVariant.name.capitalize()}"))
                        (testedVariant as ApkVariant).aabOutputFile(project) to testedVariant.applicationId
                    }
                    AndroidPluginType.DYNAMIC_FEATURE -> {
                        TODO()//Find the app module on which the project depends and depend on said app's aab and applicationId
                    }
                }

                dependsOn(testVariant.assembleProvider)
                val instrumentationApk = testVariant.apkOutputFile()
                val instrumentationPackage = testVariant.applicationId

                val instrumentationRunnerOptions = InstrumentationRunnerOptions(
                    testInstrumentationRunner = testedVariant.mergedFlavor.testInstrumentationRunner
                        ?: "android.test.InstrumentationTestRunner",
                    testInstrumentationRunnerArguments = testedVariant.mergedFlavor.testInstrumentationRunnerArguments,
                    animationsDisabled = androidExtension.testOptions.animationsDisabled
                )

                if (applicationPackage != null && applicationAab != null) {
                    this.applicationAab.apply { set(applicationAab) }.finalizeValue()
                    this.applicationPackage.apply { set(applicationPackage) }.finalizeValue()
                }

                this.instrumentationApk.apply { set(instrumentationApk) }.finalizeValue()
                this.instrumentationPackage.apply { set(instrumentationPackage) }.finalizeValue()
                this.androidInstrumentationRunnerOptions.apply { set(instrumentationRunnerOptions) }.finalizeValue()
            }
        }
    }

    private fun TestVariant.apkOutputFile() = packageApplicationProvider.flatMap {
        it.outputDirectory.file(it.apkNames.single())
    }

    private fun ApkVariant.aabOutputFile(project: Project) = project.layout.buildDirectory.file(
        "outputs/bundle/$name/${project.name}-${productFlavors.joinToString("-") { it.name }}-${buildType.name}.aab"
    )
}
