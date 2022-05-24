package com.banno.gordon

import arrow.core.Either
import com.android.tools.build.bundletool.device.Device
import io.mockk.CapturingSlot
import io.mockk.MockKMatcherScope
import io.mockk.MockKStubScope
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import shadow.bundletool.com.android.ddmlib.IShellOutputReceiver
import java.util.UUID

fun anyDevice(
    serial: String = UUID.randomUUID().toString(),
    isTablet: Boolean = false,
    tabletShortestWidthDp: Int? = null,
    currentUserId: Int = 0
): Device = mockk {
    every { serialNumber } returns serial
    mockCurrentUserId(currentUserId)

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

fun Device.mockResponse(command: MockKMatcherScope.() -> String, response: () -> String) {
    val shellOutputSlot = slot<IShellOutputReceiver>()
    every {
        executeShellCommand(command(), capture(shellOutputSlot), any(), any())
    }.answersShellOutput(shellOutputSlot, response())
}

fun MockKStubScope<Unit, Unit>.answersShellOutput(shellOutputSlot: CapturingSlot<IShellOutputReceiver>, shellOutput: String) = answers {
    shellOutputSlot.captured.addOutput(shellOutput.toByteArray(), 0, shellOutput.length)
}

fun Device.mockCurrentUserId(userId: Int) {
    mockResponse({ match { it.contains("am get-current-user") } }) { "$userId" }
}

fun Device.mockCoverageFileExists(applicationPackage: String, userId: Int, fileName: String, fileExists: Boolean = true) {
    mockResponse({ match { it.isFileExistsCheck(applicationPackage, userId, fileName) } }) {
        if (fileExists) "0" else "1"
    }
}

fun Device.mockCoverageFileExistsException(applicationPackage: String, userId: Int, fileName: String) {
    mockkStatic(Device::executeShellWithTimeout)
    every {
        executeShellWithTimeout(any(), match { it.isFileExistsCheck(applicationPackage, userId, fileName) })
    } returns Either.Left(shadow.bundletool.com.android.ddmlib.TimeoutException())
}

fun Device.mockCoverageFile(applicationPackage: String, userId: Int, fileName: String, coverageFileContent: ByteArray) {
    mockkStatic(Device::executeShellWithTimeoutBinaryOutput)
    mockCoverageFileExists(applicationPackage, userId, fileName)

    every {
        executeShellWithTimeoutBinaryOutput(any(), match { it.isCopyCoverageFileCommand(applicationPackage, userId) })
    } returns Either.Right(coverageFileContent)
}

fun Device.mockCoverageFileCopyException(applicationPackage: String, userId: Int, fileName: String) {
    mockkStatic(Device::executeShellWithTimeoutBinaryOutput)
    mockCoverageFileExists(applicationPackage, userId, fileName)
    every {
        executeShellWithTimeoutBinaryOutput(any(), match { it.isCopyCoverageFileCommand(applicationPackage, userId) })
    } returns Either.Left(shadow.bundletool.com.android.ddmlib.TimeoutException())
}

private fun String.isFileExistsCheck(applicationPackage: String, userId: Int, fileName: String): Boolean =
    contains("run-as $applicationPackage --user $userId test -f") && contains(fileName)

private fun String.isCopyCoverageFileCommand(applicationPackage: String, userId: Int): Boolean =
    contains("run-as $applicationPackage --user $userId cat")
