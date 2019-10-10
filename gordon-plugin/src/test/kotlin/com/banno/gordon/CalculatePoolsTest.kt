package com.banno.gordon

import com.banno.gordon.PoolingStrategy.AllDevices
import com.banno.gordon.PoolingStrategy.EachDevice
import com.banno.gordon.PoolingStrategy.PhonesAndTablets
import com.banno.gordon.PoolingStrategy.SpecificDevices
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyVararg
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import org.junit.Test
import se.vidstige.jadb.JadbConnection
import se.vidstige.jadb.JadbDevice
import java.io.ByteArrayInputStream
import java.util.UUID

class CalculatePoolsTest {

    @Test
    fun allDevicesIsOnePoolOfAllDevices() {
        val devices = listOf(
            anyJadbDevice(),
            anyJadbDevice(),
            anyJadbDevice()
        )

        val jadbConnection: JadbConnection = mock {
            on { this.devices } doReturn devices
        }

        val actual = calculatePools(jadbConnection, AllDevices).unsafeRunSync()

        actual shouldEqual listOf(DevicePool("All-Devices", devices))
    }

    @Test
    fun eachDeviceIsItsOwnPool() {
        val devices = listOf(
            anyJadbDevice(),
            anyJadbDevice(),
            anyJadbDevice()
        )

        val jadbConnection: JadbConnection = mock {
            on { this.devices } doReturn devices
        }

        val actual = calculatePools(jadbConnection, EachDevice).unsafeRunSync()

        actual shouldEqual devices.map { DevicePool(it.serial, listOf(it)) }
    }

    @Test
    fun specificDevicesFiltersOutDevices() {
        val firstDevice = anyJadbDevice(serial = "First")
        val secondDevice = anyJadbDevice(serial = "Second")
        val thirdDevice = anyJadbDevice(serial = "Third")

        val jadbConnection: JadbConnection = mock {
            on { this.devices } doReturn listOf(
                firstDevice,
                secondDevice,
                thirdDevice
            )
        }

        val actual = calculatePools(jadbConnection, SpecificDevices(listOf("First", "Third"))).unsafeRunSync()

        actual shouldEqual listOf(
            DevicePool("First", listOf(firstDevice)),
            DevicePool("Third", listOf(thirdDevice))
        )
    }

    @Test
    fun specificDevicesHasEmptyPoolIfDeviceIsNotFound() {
        val firstDevice = anyJadbDevice(serial = "First")

        val jadbConnection: JadbConnection = mock {
            on { this.devices } doReturn listOf(firstDevice)
        }

        val actual = calculatePools(jadbConnection, SpecificDevices(listOf("First", "Third"))).unsafeRunSync()

        actual shouldEqual listOf(
            DevicePool("First", listOf(firstDevice)),
            DevicePool("Third", emptyList())
        )
    }

    @Test
    fun phonesAndTabletsShouldBeGroupedAsSuch() {
        val tablets = listOf(
            anyJadbDevice(isTablet = true),
            anyJadbDevice(isTablet = true)
        )

        val phones = listOf(
            anyJadbDevice(isTablet = false),
            anyJadbDevice(isTablet = false)
        )

        val jadbConnection: JadbConnection = mock {
            on { this.devices } doReturn tablets + phones
        }

        val actual = calculatePools(jadbConnection, PhonesAndTablets).unsafeRunSync()

        actual shouldEqual listOf(
            DevicePool("Tablets", tablets),
            DevicePool("Phones", phones)
        )
    }

    private fun anyJadbDevice(
        serial: String = UUID.randomUUID().toString(),
        isTablet: Boolean = false
    ): JadbDevice {
        return mock {
            on { this.serial } doReturn serial
            on { this.executeShell(any<String>(), anyVararg()) } doReturn ByteArrayInputStream(
                if (isTablet) {
                    "tablet".toByteArray()
                } else {
                    "phone".toByteArray()
                }
            )
        }
    }
}
