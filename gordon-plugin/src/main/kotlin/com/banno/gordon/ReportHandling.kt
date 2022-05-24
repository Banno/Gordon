package com.banno.gordon

import arrow.core.Either
import arrow.core.computations.either
import arrow.core.flatMap
import com.android.tools.build.bundletool.device.Device
import com.banno.gordon.CoverageException.CoverageFileCopyException
import com.banno.gordon.CoverageException.CoverageFileDiscoveryException
import com.banno.gordon.CoverageException.CoverageFileNotFoundException
import org.slf4j.Logger
import java.io.File

private const val ILLEGAL_FILE_CHARACTERS_ON_WINDOWS_REGEX = "[^a-zA-Z0-9\\\\.\\-\\s]"

/**
 * The TestCase's class and method name
 *
 * Example: "DummyClassTest.testMethod"
 */
internal val TestCase.classAndMethodName
    get() = "${fullyQualifiedClassName.substringAfterLast('.')}.$methodName"

/**
 * The TestCase's full qualified class and method name
 *
 * Example: "com.example.DummyClassTest.testMethod"
 */
internal val TestCase.fullClassAndMethodName
    get() = "$fullyQualifiedClassName.$methodName"

/**
 * The coverage file's name
 *
 * Example: "com.example.DummyClassTest.testMethod.ec"
 */
internal val TestCase.coverageFileName
    get() = "$fullClassAndMethodName.ec"

/**
 * Get the path where to save coverage files on the device
 *
 * Example: "/data/user/0/com.example/files"
 */
@Suppress("SdCardPath") // We don't have access to a Context here
fun getDeviceCoveragePathFor(userId: String, targetPackage: String) =
    "/data/user/$userId/$targetPackage/files"

/**
 * Get the full path of a coverage file
 *
 * Example: "/data/user/0/com.android.example/files/com.example.DummyClassTest.testMethod.ec"
 */
internal fun getCoverageFileFullPath(userId: String, testedApplicationPackage: String, test: TestCase): String {
    return "${getDeviceCoveragePathFor(userId, testedApplicationPackage)}/${test.coverageFileName}"
}

/**
 * Replace characters that are illegal on Windows.
 *
 * Background: Coverage files get stored in a folder named after the device, but device names can contain characters that are illegal for directories on Windows
 */
internal fun String.sanitizeFileNameForWindows(): String = replace(Regex(ILLEGAL_FILE_CHARACTERS_ON_WINDOWS_REGEX), "_")

sealed class CoverageException(val coverageFileFullPath: String, e: Throwable? = null) : RuntimeException(e) {
    class CoverageFileDiscoveryException(coverageFileFullPath: String, e: Throwable) : CoverageException(coverageFileFullPath, e)
    class CoverageFileNotFoundException(coverageFileFullPath: String) : CoverageException(coverageFileFullPath)
    class CoverageFileCopyException(coverageFileFullPath: String, e: Throwable) : CoverageException(coverageFileFullPath, e)
}

/**
 * Copy the coverage file with the given name from the device to the host.
 *
 * @param coverageFileName The coverage file's name on the device. Do not pass its full path!
 * @return Either a `Throwable` or the absolute Path to the created local coverage file
 */
fun Device.copyCoverageFile(
    buildDir: File,
    taskName: String,
    applicationPackage: String,
    coverageFileName: String,
    logger: Logger
): Either<Throwable, String> {
    return copyCoverageFile(buildDir, taskName, applicationPackage, coverageFileName)
        .mapLeft { exception: Throwable ->
            when (exception) {
                is CoverageFileDiscoveryException -> logger.error("Could not check if the coverage file exists: ${exception.coverageFileFullPath}", exception)
                is CoverageFileCopyException -> logger.error("Could not copy coverage file ${exception.coverageFileFullPath}", exception)
                is CoverageFileNotFoundException -> logger.error("Coverage file not found ${exception.coverageFileFullPath}")
                else -> logger.error("Could not determine the current user id in order to copy the coverage file $coverageFileName", exception)
            }
            exception
        }.map { coverageFileFullPath: String ->
            logger.info("Copied coverage file to $coverageFileFullPath")
            coverageFileFullPath
        }
}

/**
 * Copy the coverage file with the given name from the device to the host.
 *
 * @param coverageFileName The coverage file's name on the device. Do not pass its full path!
 * @return Either a `Throwable` or the absolute Path to the created local coverage file
 */
private fun Device.copyCoverageFile(
    buildDir: File,
    taskName: String,
    applicationPackage: String,
    coverageFileName: String
): Either<Throwable, String> {
    val localCoverageDirPath = "${buildDir.path}/outputs/code_coverage/$taskName/connected/${serialNumber.sanitizeFileNameForWindows()}"

    return Either
        .catch { File(localCoverageDirPath).also { it.mkdirs() } }
        .flatMap { either.eager { getCurrentUserId().bind() } }
        .flatMap { userId: String ->
            val remoteCoverageDir = getDeviceCoveragePathFor(userId, applicationPackage)
            val remoteCoverageFileFullPath = "$remoteCoverageDir/$coverageFileName".trim()

            coverageFileExists(remoteCoverageFileFullPath, userId, applicationPackage)
                .mapLeft { e: Throwable -> CoverageFileDiscoveryException(remoteCoverageFileFullPath, e) }
                .flatMap { fileExists: Boolean ->
                    if (!fileExists) {
                        Either.Left(CoverageFileNotFoundException(remoteCoverageFileFullPath))
                    } else {
                        executeShellWithTimeoutBinaryOutput(20_000L, "run-as $applicationPackage --user $userId cat $remoteCoverageFileFullPath")
                            .mapLeft { e: Throwable -> CoverageFileCopyException(remoteCoverageFileFullPath, e) }
                            .map { shellOutput ->
                                val localCoverageFile = File("$localCoverageDirPath/$coverageFileName")
                                    .also {
                                        it.writeBytes(shellOutput)
                                    }
                                localCoverageFile.absolutePath
                            }
                    }
                }
        }
}

private fun Device.coverageFileExists(fileName: String, userId: String, applicationPackage: String): Either<Throwable, Boolean> =
    executeShellWithTimeout(20_000L, "run-as $applicationPackage --user $userId test -f $fileName; echo \$?")
        // Check the exit code
        .map { shellOutput -> shellOutput == "0" }
