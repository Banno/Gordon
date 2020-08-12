package com.banno.gordon

import kotlinx.serialization.Serializable
import java.io.File

internal data class SigningConfig(
    val storeFile: File?,
    val storePassword: String?,
    val keyAlias: String?,
    val keyPassword: String?
)

@Serializable
internal data class SigningConfigCredentials(
    val storePassword: String?,
    val keyAlias: String?,
    val keyPassword: String?
) : java.io.Serializable
