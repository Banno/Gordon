package com.banno.gordon

import arrow.fx.IO
import arrow.fx.extensions.fx
import com.android.tools.build.bundletool.commands.BuildApksCommand
import com.android.tools.build.bundletool.commands.InstallApksCommand
import com.android.tools.build.bundletool.device.DdmlibAdbServer
import com.android.tools.build.bundletool.flags.FlagParser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.gradle.api.logging.Logger
import se.vidstige.jadb.JadbConnection
import se.vidstige.jadb.JadbDevice
import se.vidstige.jadb.RemoteFile
import se.vidstige.jadb.managers.Package
import se.vidstige.jadb.managers.PackageManager
import java.io.File
import kotlin.math.min

internal fun JadbConnection.getAllDevices(): IO<List<JadbDevice>> = IO {
    devices.toList()
}

internal fun JadbDevice.executeShellWithTimeout(
    timeoutMillis: Long,
    command: String,
    vararg args: String
): IO<String?> = ioWithTimeout(timeoutMillis) {
    executeShell(command, *args)
        .use { it.reader().use { reader -> reader.readText() } }
        .trim()
}

internal fun JadbDevice.isTablet(tabletShortestWidthDp: Int?): IO<Boolean> = when (tabletShortestWidthDp) {
    null -> executeShellWithTimeout(20_000, "getprop", "ro.build.characteristics").map { it!!.contains("tablet") }
    else ->
        executeShellWithTimeout(20_000, "wm", "size")
            .flatMap { sizeString ->
                executeShellWithTimeout(20_000, "wm", "density").map { sizeString!! to it!! }
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

internal fun JadbDevice.installApk(timeoutMillis: Long, apk: File, vararg options: String) = IO.fx {
    val remoteFile = RemoteFile("/data/local/tmp/" + apk.name)

    push(apk, remoteFile)

    val result = executeShellWithTimeout(timeoutMillis, "pm", "install", *options, remoteFile.path).bind()

    PackageManager(this@installApk).remove(remoteFile)

    when {
        result == null -> raiseError<Unit>(IllegalStateException("Install timed out for ${apk.name} on $serial")).bind()

        !result.contains("Success") -> raiseError<Unit>(IllegalStateException("Failed to install ${apk.name} on $serial\n$result")).bind()

        else -> Unit
    }
}

internal fun JadbDevice.installApkSet(timeoutMillis: Long, apkSet: File, dynamicModule: String?) = IO.fx {
    val result = ioWithTimeout(timeoutMillis) {
        InstallApksCommand.fromFlags(
            FlagParser().parse(
                *listOfNotNull(
                    "install-apks",
                    "--apks=${apkSet.path}",
                    "--allow-downgrade",
                    "--allow-test-only",
                    "--device-id=$serial",
                    dynamicModule?.let { "--modules=$it" }
                ).toTypedArray()
            ),
            DdmlibAdbServer.getInstance()
        ).execute()
    }.bind()

    if (result == null) {
        raiseError<Unit>(IllegalStateException("Install timed out for ${apkSet.name} on $serial")).bind()
    }
}

internal fun buildApkSet(aab: File) = IO {
    File.createTempFile(aab.nameWithoutExtension, ".apks").also {
        BuildApksCommand.fromFlags(
            FlagParser().parse(
                "build-apks",
                "--bundle=${aab.path}",
                "--output=${it.path}",
                "--overwrite"
            ),
            DdmlibAdbServer.getInstance()
        ).execute()
    }
}

internal fun PackageManager.safeUninstall(packageName: String) = try {
    uninstall(Package(packageName))
} catch (t: Throwable) {
}

internal fun List<JadbDevice>.uninstall(
    dispatcher: CoroutineDispatcher,
    applicationPackage: String?,
    instrumentationPackage: String
) = IO {
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
    dynamicModule: String?,
    applicationAab: File?,
    instrumentationApk: File,
    installTimeoutMillis: Long
) = IO {
    runBlocking {
        val applicationApkSet = applicationAab?.let(::buildApkSet)?.unsafeRunSync()

        map { device ->
            async(context = dispatcher, start = CoroutineStart.LAZY) {
                val packageManager = PackageManager(device)

                if (applicationPackage != null && applicationApkSet != null) {
                    logger.lifecycle("${device.serial}: installing $applicationPackage")
                    packageManager.safeUninstall(applicationPackage)
                    device.installApkSet(installTimeoutMillis, applicationApkSet, dynamicModule).unsafeRunSync()
                }

                logger.lifecycle("${device.serial}: installing $instrumentationPackage")
                packageManager.safeUninstall(instrumentationPackage)
                device.installApk(installTimeoutMillis, instrumentationApk, "-d", "-r", "-t").unsafeRunSync()
            }
        }.awaitAll()
    }
}
