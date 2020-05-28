package com.banno.gordon

import arrow.fx.IO
import arrow.fx.extensions.fx
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

internal fun JadbConnection.getAllDevices(): IO<List<JadbDevice>> = IO {
    devices.toList()
}

internal fun JadbDevice.executeShellWithTimeout(
    timeoutMillis: Long,
    command: String,
    vararg args: String
): IO<String?> = IO {
    val deferred = CoroutineScope(Dispatchers.IO).async {
        executeShell(command, *args)
            .use { it.reader().use { reader -> reader.readText() } }
            .trim()
    }

    runBlocking {
        withTimeoutOrNull(timeoutMillis) {
            deferred.await()
        }
    }
}

internal fun JadbDevice.isTablet(tabletShortestWidthDp: Int?): IO<Boolean> = when (tabletShortestWidthDp) {
    null -> executeShellWithTimeout(20_000, "getprop", "ro.build.characteristics").map { it!!.contains("tablet") }
    else -> executeShellWithTimeout(20_000, "wm", "size")
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
    applicationApk: File?,
    instrumentationApk: File,
    installTimeoutMillis: Long
) = IO {
    runBlocking {
        map { device ->
            async(context = dispatcher, start = CoroutineStart.LAZY) {
                val packageManager = PackageManager(device)

                if (applicationPackage != null && applicationApk != null) {
                    logger.lifecycle("${device.serial}: installing $applicationPackage")
                    packageManager.safeUninstall(applicationPackage)
                    device.installApk(installTimeoutMillis, applicationApk, "-d", "-r").unsafeRunSync()
                }

                logger.lifecycle("${device.serial}: installing $instrumentationPackage")
                packageManager.safeUninstall(instrumentationPackage)
                device.installApk(installTimeoutMillis, instrumentationApk, "-d", "-r", "-t").unsafeRunSync()
            }
        }.awaitAll()
    }
}
