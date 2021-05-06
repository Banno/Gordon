package com.banno.gordon

import arrow.core.Either
import arrow.core.computations.either
import arrow.core.flatMap
import arrow.core.left
import com.android.tools.build.bundletool.commands.BuildApksCommand
import com.android.tools.build.bundletool.commands.InstallApksCommand
import com.android.tools.build.bundletool.device.DdmlibAdbServer
import com.android.tools.build.bundletool.flags.FlagParser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.gradle.api.logging.Logger
import se.vidstige.jadb.JadbConnection
import se.vidstige.jadb.JadbDevice
import se.vidstige.jadb.RemoteFile
import se.vidstige.jadb.managers.Package
import se.vidstige.jadb.managers.PackageManager
import java.io.File
import kotlin.math.min

internal class AdbTimeoutException : Exception()

internal fun JadbConnection.getAllDevices(): Either<Throwable, List<JadbDevice>> = Either.catch {
    devices.toList()
}

internal fun JadbDevice.safeExecuteShell(command: String, vararg args: String): Either<Throwable, String> = Either.catch {
    executeShell(command, *args)
        .use { it.reader().use { reader -> reader.readText() } }
        .trim()
}

internal fun JadbDevice.executeShellWithTimeout(
    timeoutMillis: Long,
    command: String,
    vararg args: String
): Either<Throwable, String> = Either.catchAndFlatten {
    val deferred = CoroutineScope(Dispatchers.IO).async { safeExecuteShell(command, *args) }

    runBlocking {
        withTimeoutOrNull(timeoutMillis) {
            deferred.await()
        } ?: AdbTimeoutException().left()
    }
}

internal fun JadbDevice.isTablet(tabletShortestWidthDp: Int?): Either<Throwable, Boolean> = when (tabletShortestWidthDp) {
    null -> executeShellWithTimeout(20_000, "getprop", "ro.build.characteristics").map { it.contains("tablet") }
    else ->
        executeShellWithTimeout(20_000, "wm", "size")
            .flatMap { sizeString ->
                executeShellWithTimeout(20_000, "wm", "density").map { sizeString to it }
            }
            .map { (sizeString, densityString) ->
                val shortestWidthPixels = min(
                    sizeString.substringAfterLast("size:").substringBefore('x').trim().toInt(),
                    sizeString.substringAfterLast('x').trim().toInt()
                )
                val density = densityString.substringAfterLast("density:").trim().toInt()

                val shortestWidthDp = shortestWidthPixels * 160 / density

                shortestWidthDp >= tabletShortestWidthDp
            }
}

internal fun JadbDevice.installApk(timeoutMillis: Long, apk: File, vararg options: String): Either<Throwable, Unit> = Either.catchAndFlatten {
    val remoteFile = RemoteFile("/data/local/tmp/" + apk.name)

    push(apk, remoteFile)

    val shellResult = executeShellWithTimeout(timeoutMillis, "pm", "install", *options, remoteFile.path)

    PackageManager(this@installApk).remove(remoteFile)

    shellResult.flatMap { result ->
        Either.conditionally(
            test = result.contains("Success"),
            ifFalse = { IllegalStateException(result) },
            ifTrue = {}
        )
    }
}.mapLeft { IllegalStateException("Failed to install ${apk.name} on $serial", it) }

internal fun JadbDevice.installApkSet(timeoutMillis: Long, apkSet: File, onDemandDynamicModuleName: String?): Either<Throwable, Unit> =
    Either.catch {
        InstallApksCommand.fromFlags(
            FlagParser().parse(
                *listOfNotNull(
                    "install-apks",
                    "--timeout-millis=$timeoutMillis",
                    "--apks=${apkSet.path}",
                    "--allow-downgrade",
                    "--allow-test-only",
                    "--device-id=$serial",
                    onDemandDynamicModuleName?.let { "--modules=$it" }
                ).toTypedArray()
            ),
            DdmlibAdbServer.getInstance()
        ).execute()
    }.mapLeft { IllegalStateException("Failed to install ${apkSet.name} on $serial", it) }

internal fun buildApkSet(aabFile: File, signingConfig: SigningConfig): Either<Throwable, File> = Either.catch {
    File.createTempFile(aabFile.nameWithoutExtension, ".apks").also { apksFile ->
        BuildApksCommand.fromFlags(
            FlagParser().parse(
                *listOfNotNull(
                    "build-apks",
                    "--bundle=${aabFile.path}",
                    "--output=${apksFile.path}",
                    "--overwrite",
                    signingConfig.storeFile?.let { "--ks=$it" },
                    signingConfig.storePassword?.let { "--ks-pass=pass:$it" },
                    signingConfig.keyAlias?.let { "--ks-key-alias=$it" },
                    signingConfig.keyPassword?.let { "--key-pass=pass:$it" }
                ).toTypedArray()
            ),
            DdmlibAdbServer.getInstance()
        ).execute()
    }
}

internal fun PackageManager.safeUninstall(packageName: String) {
    Either.catch { uninstall(Package(packageName)) }
}

internal fun List<JadbDevice>.safeUninstall(
    dispatcher: CoroutineDispatcher,
    applicationPackage: String?,
    instrumentationPackage: String
) {
    runBlocking {
        map { device ->
            async(context = dispatcher, start = CoroutineStart.LAZY) {
                PackageManager(device).run {
                    applicationPackage?.let(::safeUninstall)
                    safeUninstall(instrumentationPackage)
                }
            }
        }.awaitAll()
    }
}

internal fun List<JadbDevice>.reinstall(
    dispatcher: CoroutineDispatcher,
    logger: Logger,
    applicationPackage: String?,
    instrumentationPackage: String,
    onDemandDynamicModuleName: String?,
    applicationAab: File?,
    signingConfig: SigningConfig,
    instrumentationApk: File,
    installTimeoutMillis: Long
): Either<Throwable, Unit> = runBlocking {
    either {
        val applicationApkSet = applicationAab?.let { buildApkSet(it, signingConfig) }?.bind()

        map { device ->
            async(context = dispatcher, start = CoroutineStart.LAZY) {
                val packageManager = PackageManager(device)

                if (applicationPackage != null && applicationApkSet != null) {
                    logger.lifecycle("${device.serial}: installing $applicationPackage")
                    packageManager.safeUninstall(applicationPackage)
                    device.installApkSet(installTimeoutMillis, applicationApkSet, onDemandDynamicModuleName).bind()
                }

                logger.lifecycle("${device.serial}: installing $instrumentationPackage")
                packageManager.safeUninstall(instrumentationPackage)
                device.installApk(installTimeoutMillis, instrumentationApk, "-d", "-r", "-t").bind()
            }
        }.awaitAll()
    }
}
