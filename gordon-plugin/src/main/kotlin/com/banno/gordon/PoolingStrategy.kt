package com.banno.gordon

import kotlinx.serialization.Serializable

@Serializable
sealed class PoolingStrategy : java.io.Serializable {

    object EachDevice : PoolingStrategy()

    object AllDevices : PoolingStrategy()

    object PhonesAndTablets : PoolingStrategy()

    @Serializable
    data class SpecificDevices(val deviceSerials: List<String>) : PoolingStrategy()
}
