package com.banno.gordon

import arrow.core.Either
import arrow.core.computations.either
import arrow.core.flatMap
import com.android.tools.build.bundletool.commands.BuildApksCommand
import com.android.tools.build.bundletool.commands.InstallApksCommand
import com.android.tools.build.bundletool.device.AdbServer
import com.android.tools.build.bundletool.device.DdmlibAdbServer
import com.android.tools.build.bundletool.device.Device
import com.android.tools.build.bundletool.flags.FlagParser
import com.android.tools.build.bundletool.model.utils.DefaultSystemEnvironmentProvider
import com.android.tools.build.bundletool.model.utils.SdkToolsLocator
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.gradle.api.logging.Logger
import shadow.bundletool.com.android.ddmlib.CollectingOutputReceiver
import shadow.bundletool.com.android.ddmlib.ShellCommandUnresponsiveException
import shadow.bundletool.com.android.ddmlib.TimeoutException
import java.io.File
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.math.min

internal class AdbTimeoutException(cause: Throwable) : Exception(cause)

internal fun initializeDefaultAdbServer(): Either<Throwable, AdbServer> = Either.catch {
    val adbPath = SdkToolsLocator()
        .locateAdb(DefaultSystemEnvironmentProvider())
        .orElseThrow { IllegalStateException("Unable to locate ADB. Please define ANDROID_HOME or PATH environment variable.") }
    DdmlibAdbServer
        .getInstance()
        .apply { init(adbPath) }
}

internal fun AdbServer.getAllDevices(): Either<Throwable, Set<Device>> = Either.catch {
    devices.toSet()
}

internal fun Device.executeShellWithTimeout(timeoutMillis: Long, command: String): Either<Throwable, String> = Either.catch {
    CollectingOutputReceiver()
        .also { executeShellCommand(command, it, timeoutMillis, TimeUnit.MILLISECONDS) }
        .output
        .trim()
}.mapLeft {
    when (it) {
        is ShellCommandUnresponsiveException,
        is TimeoutException -> AdbTimeoutException(it)
        else -> it
    }
}

internal fun Device.isTablet(tabletShortestWidthDp: Int?): Either<Throwable, Boolean> = when (tabletShortestWidthDp) {
    null -> executeShellWithTimeout(20_000, "getprop ro.build.characteristics").map { it.contains("tablet") }
    else ->
        executeShellWithTimeout(20_000, "wm size")
            .flatMap { sizeString ->
                executeShellWithTimeout(20_000, "wm density").map { sizeString to it }
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

internal fun Device.installApk(timeoutMillis: Long, apk: File): Either<Throwable, Unit> =
    Either.catch({ IllegalStateException("Failed to install ${apk.name} on $serialNumber", it) }) {
        installApks(
            ImmutableList.of(apk.toPath()),
            Device.InstallOptions.builder()
                .setTimeout(Duration.ofMillis(timeoutMillis))
                .setAllowDowngrade(true)
                .setAllowReinstall(true)
                .setAllowTestOnly(true)
                .build()
        )
    }

internal fun Device.installApkSet(adb: AdbServer, timeoutMillis: Long, apkSet: File, onDemandDynamicModuleName: String?): Either<Throwable, Unit> =
    Either.catch({ IllegalStateException("Failed to install ${apkSet.name} on $serialNumber", it) }) {
        InstallApksCommand.fromFlags(
            FlagParser().parse(
                *listOfNotNull(
                    "install-apks",
                    "--timeout-millis=$timeoutMillis",
                    "--apks=${apkSet.path}",
                    "--allow-downgrade",
                    "--allow-test-only",
                    "--device-id=$serialNumber",
                    onDemandDynamicModuleName?.let { "--modules=$it" }
                ).toTypedArray()
            ),
            adb
        ).execute()
    }

internal fun buildApkSet(adb: AdbServer, aabFile: File, signingConfig: SigningConfig): Either<Throwable, File> = Either.catch {
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
            adb
        ).execute()
    }
}

internal fun Device.safeUninstall(timeoutMillis: Long, packageName: String) {
    executeShellWithTimeout(timeoutMillis, "pm uninstall $packageName")
}

internal fun List<Device>.safeUninstall(
    dispatcher: CoroutineDispatcher,
    timeoutMillis: Long,
    applicationPackage: String?,
    instrumentationPackage: String
) {
    runBlocking {
        map { device ->
            async(context = dispatcher, start = CoroutineStart.LAZY) {
                device.run {
                    applicationPackage?.let { safeUninstall(timeoutMillis, it) }
                    safeUninstall(timeoutMillis, instrumentationPackage)
                }
            }
        }.awaitAll()
    }
}

internal fun List<Device>.reinstall(
    dispatcher: CoroutineDispatcher,
    logger: Logger,
    applicationPackage: String?,
    instrumentationPackage: String,
    onDemandDynamicModuleName: String?,
    applicationAab: File?,
    signingConfig: SigningConfig,
    instrumentationApk: File,
    installTimeoutMillis: Long,
    adb: AdbServer
): Either<Throwable, Unit> = either.eager {
    val applicationApkSet = applicationAab?.let { buildApkSet(adb, it, signingConfig) }?.bind()

    runBlocking {
        map { device ->
            async(context = dispatcher, start = CoroutineStart.LAZY) {
                either.eager<Throwable, Unit> {
                    if (applicationPackage != null && applicationApkSet != null) {
                        logger.lifecycle("${device.serialNumber}: installing $applicationPackage")
                        device.safeUninstall(installTimeoutMillis, applicationPackage)
                        device.installApkSet(adb, installTimeoutMillis, applicationApkSet, onDemandDynamicModuleName).bind()
                    }

                    logger.lifecycle("${device.serialNumber}: installing $instrumentationPackage")
                    device.safeUninstall(installTimeoutMillis, instrumentationPackage)
                    device.installApk(installTimeoutMillis, instrumentationApk).bind()
                }
            }
        }.awaitAll()
    }.forEach { it.bind() }
}
