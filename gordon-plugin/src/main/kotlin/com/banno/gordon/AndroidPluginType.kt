package com.banno.gordon

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.hasPlugin

internal enum class AndroidPluginType {
    APP, LIBRARY
}

internal fun Project.androidPluginType() = when {
    plugins.hasPlugin(AppPlugin::class) -> AndroidPluginType.APP
    plugins.hasPlugin(LibraryPlugin::class) -> AndroidPluginType.LIBRARY
    else -> throw IllegalStateException("Gordon plugin must be applied after applying the Android application plugin or Android library plugin")
}
