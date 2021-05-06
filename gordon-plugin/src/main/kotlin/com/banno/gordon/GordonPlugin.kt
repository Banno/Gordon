package com.banno.gordon

import com.android.build.api.artifact.ArtifactType
import com.android.build.api.attributes.VariantAttr
import com.android.build.api.extension.AndroidComponentsExtension
import com.android.build.api.variant.Variant
import com.android.build.api.variant.VariantBuilder
import com.android.build.gradle.AppExtension
import com.android.build.gradle.TestedExtension
import com.android.build.gradle.api.ApkVariant
import com.android.build.gradle.api.ApplicationVariant
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.attributes.Attribute
import org.gradle.api.plugins.JavaBasePlugin.VERIFICATION_GROUP
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register

class GordonPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val androidPluginType = project.androidPluginType()
            ?: error("Gordon plugin must be applied after applying the application, library, or dynamic-feature Android plugin")

        val gordonExtension = project.extensions.create<GordonExtension>("gordon")

        val componentsExtension = project.extensions.getByName<AndroidComponentsExtension<VariantBuilder, Variant>>("androidComponents")
        val testedExtension = project.extensions.getByType<TestedExtension>()

        componentsExtension
            .androidTests { testVariantProperties ->
                val variantTaskName = testVariantProperties.name
                    .capitalize()
                    .replace(Regex("AndroidTest$"), "")
                    .replace(Regex("Debug$"), "")

                project.tasks.register<GordonTestTask>("gordon$variantTaskName") {
                    group = VERIFICATION_GROUP
                    val variantDescription = variantTaskName.takeIf { it.isNotBlank() }?.let { " for $it" } ?: ""
                    description = "Installs and runs instrumentation tests$variantDescription."

                    this.rootProjectBuildDirectory.set(project.rootProject.layout.buildDirectory)

                    val testedVariantProperties = testVariantProperties.testedVariant

                    this.instrumentationApkDir.set(testVariantProperties.artifacts.get(ArtifactType.APK))
                    this.instrumentationPackage.set(testVariantProperties.applicationId)

                    if (androidPluginType != AndroidPluginType.LIBRARY) {
                        this.applicationPackage.set(testedVariantProperties.applicationId)
                    }

                    if (androidPluginType == AndroidPluginType.DYNAMIC_FEATURE) {
                        this.dynamicFeatureModuleManifest.set(testedVariantProperties.artifacts.get(ArtifactType.MERGED_MANIFEST))
                        this.dynamicFeatureModuleName.set(project.name)
                    }

                    val testVariant = testedExtension.testVariants.single { it.name == testVariantProperties.name }
                    val testedVariant = testVariant.testedVariant
                    val (appAabProvider, appVariant) = when (androidPluginType) {
                        AndroidPluginType.LIBRARY ->
                            null to null
                        AndroidPluginType.APP ->
                            testedVariantProperties.artifacts.get(ArtifactType.BUNDLE) to testedVariant as ApplicationVariant
                        AndroidPluginType.DYNAMIC_FEATURE -> {
                            val (appProject, appVariant) = appDependencyOfFeature(project, testedVariant as ApkVariant)
                            dependsOn(appProject.tasks.named("bundle${appVariant.name.capitalize()}"))
                            appVariant.aabOutputFile(appProject) to appVariant
                        }
                    }

                    appAabProvider?.let(this.applicationAab::set)

                    appVariant?.signingConfig?.storeFile?.let(this.signingKeystoreFile::set)
                    this.signingConfigCredentials.set(
                        SigningConfigCredentials(
                            storePassword = appVariant?.signingConfig?.storePassword,
                            keyAlias = appVariant?.signingConfig?.keyAlias,
                            keyPassword = appVariant?.signingConfig?.keyPassword
                        )
                    )

                    val instrumentationRunnerOptions = testVariantProperties.instrumentationRunner.map { instrumentationRunner ->
                        InstrumentationRunnerOptions(
                            testInstrumentationRunner = instrumentationRunner,
                            testInstrumentationRunnerArguments = testedVariant.mergedFlavor.testInstrumentationRunnerArguments,
                            animationsDisabled = testedExtension.testOptions.animationsDisabled
                        )
                    }
                    this.androidInstrumentationRunnerOptions.set(instrumentationRunnerOptions)

                    this.poolingStrategy.set(gordonExtension.poolingStrategy)
                    this.tabletShortestWidthDp.set(gordonExtension.tabletShortestWidthDp)
                    this.retryQuota.set(gordonExtension.retryQuota)
                    this.installTimeoutMillis.set(gordonExtension.installTimeoutMillis)
                    this.testTimeoutMillis.set(gordonExtension.testTimeoutMillis)
                    this.extensionTestFilter.set(gordonExtension.testFilter)
                    this.extensionTestInstrumentationRunner.set(gordonExtension.testInstrumentationRunner)
                }
            }
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
