package com.banno.gordon

import com.android.build.api.attributes.VariantAttr
import com.android.build.gradle.AppExtension
import com.android.build.gradle.TestedExtension
import com.android.build.gradle.api.ApkVariant
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.TestVariant
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.attributes.Attribute
import org.gradle.api.plugins.JavaBasePlugin.VERIFICATION_GROUP
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register

class GordonPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val androidPluginType = project.androidPluginType()
            ?: error("Gordon plugin must be applied after applying the application, library, or dynamic-feature Android plugin")

        val gordonExtension = project.extensions.create<GordonExtension>("gordon")

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

                val (appProject, appVariant) = when (androidPluginType) {
                    AndroidPluginType.LIBRARY ->
                        null to null
                    AndroidPluginType.APP ->
                        project to testedVariant as ApplicationVariant
                    AndroidPluginType.DYNAMIC_FEATURE ->
                        appDependencyOfFeature(project, testedVariant as ApkVariant)
                }

                val instrumentationRunnerOptions = InstrumentationRunnerOptions(
                    testInstrumentationRunner = testedVariant.mergedFlavor.testInstrumentationRunner
                        ?: "android.test.InstrumentationTestRunner",
                    testInstrumentationRunnerArguments = testedVariant.mergedFlavor.testInstrumentationRunnerArguments,
                    animationsDisabled = androidExtension.testOptions.animationsDisabled
                )

                dependsOn(testVariant.assembleProvider)

                if (appProject != null && appVariant != null) {
                    dependsOn(appProject.tasks.named("bundle${appVariant.name.capitalize()}"))
                    this.applicationAab.apply { set(appVariant.aabOutputFile(appProject)) }.finalizeValue()
                    this.applicationPackage.apply { set(appVariant.applicationId) }.finalizeValue()
                }

                appVariant?.signingConfig?.storeFile?.let {
                    this.signingKeystoreFile.apply { set(it) }.finalizeValue()
                }
                this.signingConfigCredentials.apply {
                    set(
                        SigningConfigCredentials(
                            storePassword = appVariant?.signingConfig?.storePassword,
                            keyAlias = appVariant?.signingConfig?.keyAlias,
                            keyPassword = appVariant?.signingConfig?.keyPassword
                        )
                    )
                }.finalizeValue()

                if (androidPluginType == AndroidPluginType.DYNAMIC_FEATURE) {
                    this.dynamicFeatureModuleName.apply { set(project.name) }.finalizeValue()
                }

                this.instrumentationApk.apply { set(testVariant.apkOutputFile()) }.finalizeValue()
                this.instrumentationPackage.apply { set(testVariant.applicationId) }.finalizeValue()

                this.poolingStrategy.apply { set(gordonExtension.poolingStrategy) }.finalizeValue()
                this.tabletShortestWidthDp.apply { set(gordonExtension.tabletShortestWidthDp) }.finalizeValue()
                this.retryQuota.apply { set(gordonExtension.retryQuota) }.finalizeValue()
                this.installTimeoutMillis.apply { set(gordonExtension.installTimeoutMillis) }.finalizeValue()
                this.testTimeoutMillis.apply { set(gordonExtension.testTimeoutMillis) }.finalizeValue()
                this.extensionTestFilter.apply { set(gordonExtension.testFilter) }.finalizeValue()
                this.extensionTestInstrumentationRunner.apply { set(gordonExtension.testInstrumentationRunner) }
                    .finalizeValue()

                this.androidInstrumentationRunnerOptions.apply { set(instrumentationRunnerOptions) }.finalizeValue()
            }
        }
    }

    private fun TestVariant.apkOutputFile() = packageApplicationProvider.flatMap {
        it.outputDirectory.file(it.apkNames.single())
    }

    private fun ApplicationVariant.aabOutputFile(appProject: Project) = appProject.layout.buildDirectory.file(
        "outputs/bundle/$name/${appProject.property("archivesBaseName")}-$baseName.aab"
    )

    private fun appDependencyOfFeature(
        featureProject: Project,
        featureVariant: ApkVariant
    ): Pair<Project, ApplicationVariant> = featureProject
        .configurations
        .getByName("${featureVariant.name}RuntimeClasspath")
        .incoming
        .resolutionResult
        .allComponents
        .filter { it.id is ProjectComponentIdentifier }
        .associateBy { featureProject.rootProject.project((it.id as ProjectComponentIdentifier).projectPath) }
        .entries
        .single { (project, _) -> project.androidPluginType() == AndroidPluginType.APP }
        .let { (appProject, component) ->
            val androidVariantAttributeKey = Attribute.of(VariantAttr.ATTRIBUTE.name, String::class.java)

            val appVariantName = component
                .variants
                .mapNotNull { it.attributes.getAttribute(androidVariantAttributeKey) }
                .single()

            val appExtension = appProject.extensions.getByType<AppExtension>()
            val appVariant = appExtension.applicationVariants.single { it.name == appVariantName }

            appProject to appVariant
        }
}
