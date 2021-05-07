package com.banno.gordon

import com.android.tools.build.bundletool.device.AdbServer
import com.android.tools.build.bundletool.device.Device
import com.google.common.collect.ImmutableList
import io.mockk.MockKStubScope
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Test
import shadow.bundletool.com.android.ddmlib.IShellOutputReceiver
import java.util.UUID

class CalculatePoolsTest {

    @Test
    fun poolPerDeviceStrategy() {
        val allDevices = listOf(
            anyDevice(),
            anyDevice(),
            anyDevice()
        )

        val adb: AdbServer = mockk {
            every { devices } returns ImmutableList.copyOf(allDevices)
        }

        val actual = calculatePools(adb, PoolingStrategy.PoolPerDevice, null).orNull()

        actual shouldEqual allDevices.map { DevicePool(it.serialNumber, listOf(it)) }
    }

    @Test
    fun singlePoolStrategy() {
        val allDevices = listOf(
            anyDevice(),
            anyDevice(),
            anyDevice()
        )

        val adb: AdbServer = mockk {
            every { devices } returns ImmutableList.copyOf(allDevices)
        }

        val actual = calculatePools(adb, PoolingStrategy.SinglePool, null).orNull()

        actual shouldEqual listOf(DevicePool("All-Devices", allDevices))
    }

    @Test
    fun phonesAndTabletsStrategy() {
        val tablets = listOf(
            anyDevice(isTablet = true),
            anyDevice(isTablet = true)
        )

        val phones = listOf(
            anyDevice(isTablet = false),
            anyDevice(isTablet = false)
        )

        val adb: AdbServer = mockk {
            every { devices } returns ImmutableList.copyOf(tablets + phones)
        }

        val actual = calculatePools(adb, PoolingStrategy.PhonesAndTablets, null).orNull()

        actual shouldEqual listOf(
            DevicePool("Tablets", tablets),
            DevicePool("Phones", phones)
        )
    }

    @Test
    fun phonesAndTabletsStrategyWithShortestWidth() {
        val shortestWidthDp = 720

        val tablets = listOf(
            anyDevice(tabletShortestWidthDp = shortestWidthDp),
            anyDevice(tabletShortestWidthDp = shortestWidthDp + 200)
        )

        val phones = listOf(
            anyDevice(tabletShortestWidthDp = shortestWidthDp - 1),
            anyDevice(tabletShortestWidthDp = shortestWidthDp - 200)
        )

        val adb: AdbServer = mockk {
            every { devices } returns ImmutableList.copyOf(tablets + phones)
        }

        val actual = calculatePools(adb, PoolingStrategy.PhonesAndTablets, shortestWidthDp).orNull()

        actual shouldEqual listOf(
            DevicePool("Tablets", tablets),
            DevicePool("Phones", phones)
        )
    }

    @Test
    fun manualStrategy() {
        val firstDevice = anyDevice(serial = "First")
        val secondDevice = anyDevice(serial = "Second")
        val thirdDevice = anyDevice(serial = "Third")
        val fourthDevice = anyDevice(serial = "Fourth")
        val fifthDevice = anyDevice(serial = "Fifth")

        val adb: AdbServer = mockk {
            every { devices } returns ImmutableList.of(
                firstDevice,
                secondDevice,
                thirdDevice,
                fourthDevice,
                fifthDevice
            )
        }

        val actual = calculatePools(
            adb,
            PoolingStrategy.Manual(
                mapOf(
                    "Pool1" to setOf("First", "Fourth", "Nonsense"),
                    "Pool2" to setOf("Second", "Fifth")
                )
            ),
            null
        ).orNull()

        actual shouldEqual listOf(
            DevicePool("Pool1", listOf(firstDevice, fourthDevice)),
            DevicePool("Pool2", listOf(secondDevice, fifthDevice))
        )
    }

    private fun anyDevice(
        serial: String = UUID.randomUUID().toString(),
        isTablet: Boolean = false,
        tabletShortestWidthDp: Int? = null
    ): Device = mockk {
        every { serialNumber } returns serial

        val shellOutputSlot = slot<IShellOutputReceiver>()

        fun MockKStubScope<Unit, Unit>.answersShellOutput(shellOutput: String) = answers {
            shellOutputSlot.captured.addOutput(shellOutput.toByteArray(), 0, shellOutput.length)
        }

        every {
            executeShellCommand("getprop ro.build.characteristics", capture(shellOutputSlot), any(), any())
        }.answersShellOutput(if (isTablet) "tablet" else " phone")

        every {
            executeShellCommand("wm size", capture(shellOutputSlot), any(), any())
        }.answersShellOutput("Physical size: ${tabletShortestWidthDp}x$tabletShortestWidthDp")

        every {
            executeShellCommand("wm density", capture(shellOutputSlot), any(), any())
        }.answersShellOutput("Physical density: 160")
    }
}
