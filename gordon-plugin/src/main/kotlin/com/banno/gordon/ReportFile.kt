package com.banno.gordon

import arrow.core.Either
import arrow.core.computations.either
import java.io.File

internal data class ReportFile(val fileName: String, val fileContent: String)

internal fun ReportFile.write(directory: File): Either<Throwable, String> = Either.catch {
    directory.mkdirs()
    File(directory, fileName)
        .also { it.writeText(fileContent) }
        .absolutePath
}

internal suspend fun List<ReportFile>.write(directory: File): Either<Throwable, Unit> = either {
    forEach { it.write(directory).bind() }
}

internal fun File.clear(): Either<Throwable, Unit> = Either.catch {
    deleteRecursively()
}
