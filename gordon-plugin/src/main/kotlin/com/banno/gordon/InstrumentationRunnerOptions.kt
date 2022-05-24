package com.banno.gordon

import kotlinx.serialization.Serializable

@Serializable
internal data class InstrumentationRunnerOptions(
    val testInstrumentationRunner: String,
    val testInstrumentationRunnerArguments: Map<String, String>,
    val animationsDisabled: Boolean
) : java.io.Serializable

internal fun InstrumentationRunnerOptions.isCoverageEnabled(): Boolean =
    testInstrumentationRunnerArguments["coverage"] == "true"
