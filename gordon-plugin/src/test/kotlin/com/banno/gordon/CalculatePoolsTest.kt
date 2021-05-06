package com.banno.gordon

import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
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

        val actual = calculatePools(jadbConnection, PoolingStrategy.PoolPerDevice, null).unsafeRunSync()

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

        val actual = calculatePools(jadbConnection, PoolingStrategy.SinglePool, null).unsafeRunSync()

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

        val actual = calculatePools(jadbConnection, PoolingStrategy.PhonesAndTablets, null).unsafeRunSync()

        actual shouldEqual listOf(
            DevicePool("Tablets", tablets),
            DevicePool("Phones", phones)
        )
    }

    @Test
    fun phonesAndTabletsStrategyWithShortestWidth() {
        val shortestWidthDp = 720

        val tablets = listOf(
            anyJadbDevice(tabletShortestWidthDp = shortestWidthDp),
            anyJadbDevice(tabletShortestWidthDp = shortestWidthDp + 200)
        )

        val phones = listOf(
            anyJadbDevice(tabletShortestWidthDp = shortestWidthDp - 1),
            anyJadbDevice(tabletShortestWidthDp = shortestWidthDp - 200)
        )

        val jadbConnection: JadbConnection = mock {
            on { this.devices } doReturn tablets + phones
        }

        val actual = calculatePools(jadbConnection, PoolingStrategy.PhonesAndTablets, shortestWidthDp).unsafeRunSync()

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
            ),
            null
        ).unsafeRunSync()

        actual shouldEqual listOf(
            DevicePool("Pool1", listOf(firstDevice, fourthDevice)),
            DevicePool("Pool2", listOf(secondDevice, fifthDevice))
        )
    }

    private fun anyJadbDevice(
        serial: String = UUID.randomUUID().toString(),
        isTablet: Boolean = false,
        tabletShortestWidthDp: Int? = null
    ): JadbDevice {
        return mock {
            on { this.serial } doReturn serial
            on { this.executeShell("getprop", "ro.build.characteristics") } doReturn ByteArrayInputStream(
                (if (isTablet) "tablet" else "phone").toByteArray()
            )
            on { this.executeShell("wm", "size") } doReturn ByteArrayInputStream(
                "Physical size: ${tabletShortestWidthDp}x$tabletShortestWidthDp".toByteArray()
            )
            on { this.executeShell("wm", "density") } doReturn ByteArrayInputStream(
                "Physical density: 160".toByteArray()
            )
        }
    }
}
