package com.banno.gordon

import arrow.core.Either
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.slf4j.Logger
import java.io.File

class ReportHandlingKtTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `should replace illegal characters in relative file names`() {
        // GIVEN
        val relativeFilePath = "some user\\some_dir\\invalid:folder\\invalid:file.log"
        val expectedFilePath = "some user\\some_dir\\invalid_folder\\invalid_file.log"

        // WHEN
        val sanitizedFilePath = relativeFilePath.sanitizeFileNameForWindows()

        // THEN
        assertEquals(expectedFilePath, sanitizedFilePath)
    }

    @Test
    fun `should replace illegal characters in relative file names in unix paths`() {
        // GIVEN
        val relativeUnixFilePath = "some:file.log"
        val expectedFilePath = "some_file.log"

        // WHEN
        val sanitizedFilePath = relativeUnixFilePath.sanitizeFileNameForWindows()

        // THEN
        assertEquals(expectedFilePath, sanitizedFilePath)
    }

    @Test
    fun `should copy coverage files correctly from the device to the host`() {
        // GIVEN
        val deviceName = "emulator-5554"
        val device = anyDevice(deviceName)
        val buildDir = temporaryFolder.newFolder()
        val gradleTaskName = "gordon"
        val applicationPackage = "com.example.package"
        val coverageFileName = "com.example.package.class.methodname.ec"
        val localCoverageFilePath = "${buildDir.path}/outputs/code_coverage/$gradleTaskName/connected/$deviceName/$coverageFileName"
        val coverageFileContent = ByteArray(4) { 0 }

        device.mockCoverageFile(applicationPackage, 0, coverageFileName, coverageFileContent)

        // WHEN
        val copyResult: Either<Throwable, String> = device.copyCoverageFile(
            buildDir,
            gradleTaskName,
            applicationPackage,
            coverageFileName,
            mockk(relaxed = true)
        )

        // THEN
        assertTrue(copyResult.isRight())

        val coverageFile = File(localCoverageFilePath)
        assertTrue(coverageFile.exists())
        assertArrayEquals(coverageFileContent, coverageFile.readBytes())
    }

    @Test
    fun `should fail when checking if the given coverage file exists fails`() {
        // GIVEN
        val deviceName = "emulator-5554"
        val device = anyDevice(deviceName)
        val buildDir = temporaryFolder.newFolder()
        val gradleTaskName = "gordon"
        val applicationPackage = "com.example.package"
        val coverageFileName = "com.example.package.class.methodname.ec"
        val localCoverageFilePath = "${buildDir.path}/outputs/code_coverage/$gradleTaskName/connected/$deviceName/$coverageFileName"
        val logger = mockk<Logger>(relaxed = true)

        device.mockCoverageFileExistsException(applicationPackage, 0, coverageFileName)

        // WHEN
        val copyResult: Either<Throwable, String> = device.copyCoverageFile(
            buildDir,
            gradleTaskName,
            applicationPackage,
            coverageFileName,
            logger
        )

        // THEN
        verify { logger.error(match { it.contains("Could not check if the coverage file exists") }, any<Throwable>()) }

        assertTrue(copyResult.isLeft())
        assertFalse(File(localCoverageFilePath).exists())
    }

    @Test
    fun `should fail when the given coverage file does not exist on the device`() {
        // GIVEN
        val deviceName = "emulator-5554"
        val device = anyDevice(deviceName)
        val buildDir = temporaryFolder.newFolder()
        val gradleTaskName = "gordon"
        val applicationPackage = "com.example.package"
        val coverageFileName = "com.example.package.class.methodname.ec"
        val localCoverageFilePath = "${buildDir.path}/outputs/code_coverage/$gradleTaskName/connected/$deviceName/$coverageFileName"
        val logger = mockk<Logger>(relaxed = true)

        device.mockCoverageFileExists(applicationPackage, 0, coverageFileName, fileExists = false)

        // WHEN
        val copyResult: Either<Throwable, String> = device.copyCoverageFile(
            buildDir,
            gradleTaskName,
            applicationPackage,
            coverageFileName,
            logger
        )

        // THEN
        verify { logger.error(match { it.contains("Coverage file not found") }) }

        assertTrue(copyResult.isLeft())
        assertFalse(File(localCoverageFilePath).exists())
    }

    @Test
    fun `should fail when copying the given coverage file fails`() {
        // GIVEN
        val deviceName = "emulator-5554"
        val device = anyDevice(deviceName)
        val buildDir = temporaryFolder.newFolder()
        val gradleTaskName = "gordon"
        val applicationPackage = "com.example.package"
        val coverageFileName = "com.example.package.class.methodname.ec"
        val localCoverageFilePath = "${buildDir.path}/outputs/code_coverage/$gradleTaskName/connected/$deviceName/$coverageFileName"
        val logger = mockk<Logger>(relaxed = true)

        device.mockCoverageFileCopyException(applicationPackage, 0, coverageFileName)

        // WHEN
        val copyResult: Either<Throwable, String> = device.copyCoverageFile(
            buildDir,
            gradleTaskName,
            applicationPackage,
            coverageFileName,
            logger
        )

        // THEN
        verify { logger.error(match { it.contains("Could not copy coverage file") }, any<Throwable>()) }

        assertTrue(copyResult.isLeft())
        assertFalse(File(localCoverageFilePath).exists())
    }
}
