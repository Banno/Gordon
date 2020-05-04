package com.banno.gordon

import kotlinx.serialization.Serializable

@Serializable
sealed class PoolingStrategy : java.io.Serializable {

    object PoolPerDevice : PoolingStrategy()

    object SinglePool : PoolingStrategy()

    object PhonesAndTablets : PoolingStrategy()

    @Serializable
    data class Manual(val poolNameToDeviceSerials: Map<String, Set<String>>) : PoolingStrategy() {
        constructor(deviceSerials: List<String>) : this(deviceSerials.associateWith(::setOf))
    }
}
