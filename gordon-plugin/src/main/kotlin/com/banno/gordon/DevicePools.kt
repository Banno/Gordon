package com.banno.gordon

import arrow.core.Either
import arrow.core.computations.either
import com.android.tools.build.bundletool.device.AdbServer
import com.android.tools.build.bundletool.device.Device

internal typealias PoolName = String

internal data class DevicePool(
    val poolName: PoolName,
    val devices: List<Device>
)

internal fun calculatePools(
    adb: AdbServer,
    strategy: PoolingStrategy,
    tabletShortestWidthDp: Int?
): Either<Throwable, List<DevicePool>> = either.eager {
    val allDevices = adb.getAllDevices().bind()

    when (strategy) {
        is PoolingStrategy.PoolPerDevice -> allDevices.map { DevicePool(it.serialNumber, listOf(it)) }

        is PoolingStrategy.SinglePool -> listOf(DevicePool("All-Devices", allDevices.toList()))

        is PoolingStrategy.PhonesAndTablets -> {
            val deviceAndIsTablet = allDevices.map { it to it.isTablet(tabletShortestWidthDp).bind() }

            listOf(
                DevicePool("Tablets", deviceAndIsTablet.filter { it.second }.map { it.first }),
                DevicePool("Phones", deviceAndIsTablet.filter { !it.second }.map { it.first })
            )
        }

        is PoolingStrategy.Manual ->
            strategy.poolNameToDeviceSerials.map { (poolName, deviceSerials) ->
                DevicePool(poolName, deviceSerials.mapNotNull { serial -> allDevices.find { it.serialNumber == serial } })
            }
    }
}
