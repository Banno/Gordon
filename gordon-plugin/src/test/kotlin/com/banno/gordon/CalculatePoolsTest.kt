package com.banno.gordon

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
    fun poolPerDeviceStrategy() {
        val devices = listOf(
            anyJadbDevice(),
            anyJadbDevice(),
            anyJadbDevice()
        )

        val jadbConnection: JadbConnection = mock {
            on { this.devices } doReturn devices
        }

        val actual = calculatePools(jadbConnection, PoolingStrategy.PoolPerDevice).unsafeRunSync()

        actual shouldEqual devices.map { DevicePool(it.serial, listOf(it)) }
    }

    @Test
    fun singlePoolStrategy() {
        val devices = listOf(
            anyJadbDevice(),
            anyJadbDevice(),
            anyJadbDevice()
        )

        val jadbConnection: JadbConnection = mock {
            on { this.devices } doReturn devices
        }

        val actual = calculatePools(jadbConnection, PoolingStrategy.SinglePool).unsafeRunSync()

        actual shouldEqual listOf(DevicePool("All-Devices", devices))
    }

    @Test
    fun phonesAndTabletsStrategy() {
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

        val actual = calculatePools(jadbConnection, PoolingStrategy.PhonesAndTablets).unsafeRunSync()

        actual shouldEqual listOf(
            DevicePool("Tablets", tablets),
            DevicePool("Phones", phones)
        )
    }

    @Test
    fun manualStrategy() {
        val firstDevice = anyJadbDevice(serial = "First")
        val secondDevice = anyJadbDevice(serial = "Second")
        val thirdDevice = anyJadbDevice(serial = "Third")
        val fourthDevice = anyJadbDevice(serial = "Fourth")
        val fifthDevice = anyJadbDevice(serial = "Fifth")

        val jadbConnection: JadbConnection = mock {
            on { this.devices } doReturn listOf(
                firstDevice,
                secondDevice,
                thirdDevice,
                fourthDevice,
                fifthDevice
            )
        }

        val actual = calculatePools(
            jadbConnection,
            PoolingStrategy.Manual(
                mapOf(
                    "Pool1" to setOf("First", "Fourth", "Nonsense"),
                    "Pool2" to setOf("Second", "Fifth")
                )
            )
        ).unsafeRunSync()

        actual shouldEqual listOf(
            DevicePool("Pool1", listOf(firstDevice, fourthDevice)),
            DevicePool("Pool2", listOf(secondDevice, fifthDevice))
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
