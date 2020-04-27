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

    @Deprecated("Use 'PoolPerDevice' instead. 'EachDevice' will be removed in v1.3.0", ReplaceWith("PoolPerDevice"))
    object EachDevice : PoolingStrategy()

    @Deprecated("Use 'SinglePool' instead. 'AllDevices' will be removed in v1.3.0", ReplaceWith("SinglePool"))
    object AllDevices : PoolingStrategy()

    @Deprecated("Use 'Manual' instead. 'SpecificDevices' will be removed in v1.3.0", ReplaceWith("Manual"))
    @Serializable
    data class SpecificDevices(val deviceSerials: List<String>) : PoolingStrategy()
}
