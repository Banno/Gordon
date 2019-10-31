package com.banno.gordon

import arrow.fx.IO
import arrow.fx.extensions.fx
import java.io.File

internal data class ReportFile(val fileName: String, val fileContent: String)

internal fun ReportFile.write(directory: File) = IO {
    directory.mkdirs()
    File(directory, fileName)
        .also { it.writeText(fileContent) }
        .absolutePath
}

internal fun List<ReportFile>.write(directory: File) = IO.fx {
    forEach { it.write(directory).bind() }
}

internal fun File.clear(): IO<Unit> = IO {
    deleteRecursively()
}
