package com.banno.gordon

import kotlinx.serialization.Serializable

@Serializable
internal data class InstrumentationRunnerOptions(
    val testInstrumentationRunner: String,
    val testInstrumentationRunnerArguments: Map<String, String>,
    val animationsDisabled: Boolean
) : java.io.Serializable
