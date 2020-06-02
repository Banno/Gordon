package com.banno.gordon

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.DynamicFeaturePlugin
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.hasPlugin

internal enum class AndroidPluginType {
    APP, LIBRARY, DYNAMIC_FEATURE
}

internal fun Project.androidPluginType() = when {
    plugins.hasPlugin(AppPlugin::class) -> AndroidPluginType.APP
    plugins.hasPlugin(LibraryPlugin::class) -> AndroidPluginType.LIBRARY
    plugins.hasPlugin(DynamicFeaturePlugin::class) -> AndroidPluginType.DYNAMIC_FEATURE
    else -> null
}
