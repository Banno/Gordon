package com.banno.gordon

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.DynamicFeatureExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.DynamicFeatureAndroidComponentsExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByName

internal fun Project.androidPlugin(): AndroidPlugin<*, *>? =
    when (val androidExtension = extensions.findByName("android")) {
        is ApplicationExtension -> AndroidPlugin.App(
            androidExtension,
            extensions.getByName<ApplicationAndroidComponentsExtension>("androidComponents")
        )
        is LibraryExtension -> AndroidPlugin.Library(
            androidExtension,
            extensions.getByName<LibraryAndroidComponentsExtension>("androidComponents")
        )
        is DynamicFeatureExtension -> AndroidPlugin.DynamicFeature(
            androidExtension,
            extensions.getByName<DynamicFeatureAndroidComponentsExtension>("androidComponents")
        )
        else -> null
    }

internal sealed class AndroidPlugin<
    out T : CommonExtension<*, *, *, *, *, *>,
    out U : AndroidComponentsExtension<out T, *, *>
    > {

    abstract val androidExtension: T
    abstract val componentsExtension: U

    class App(
        override val androidExtension: ApplicationExtension,
        override val componentsExtension: ApplicationAndroidComponentsExtension
    ) : AndroidPlugin<ApplicationExtension, ApplicationAndroidComponentsExtension>()

    class Library(
        override val androidExtension: LibraryExtension,
        override val componentsExtension: LibraryAndroidComponentsExtension
    ) : AndroidPlugin<LibraryExtension, LibraryAndroidComponentsExtension>()

    class DynamicFeature(
        override val androidExtension: DynamicFeatureExtension,
        override val componentsExtension: DynamicFeatureAndroidComponentsExtension
    ) : AndroidPlugin<DynamicFeatureExtension, DynamicFeatureAndroidComponentsExtension>()
}
