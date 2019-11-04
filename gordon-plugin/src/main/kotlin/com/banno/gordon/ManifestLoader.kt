package com.banno.gordon

import arrow.fx.IO
import arrow.fx.extensions.fx
import com.shazam.axmlparser.AXMLParser
import java.io.File
import java.util.zip.ZipFile

internal fun getManifestPackage(apk: File): IO<String> = IO.fx {
    val manifestPackage: String? = loadFromManifest(apk) {
        moveToTag("manifest")

        attributes()
            .firstOrNull { it.name == "package" }
            ?.value
    }

    manifestPackage ?: raiseError<String>(IllegalStateException("Manifest package not found")).bind()
}

private fun <T> loadFromManifest(apk: File, block: AXMLParser.() -> T) = ZipFile(apk).use { zip ->
    zip.getInputStream(zip.getEntry("AndroidManifest.xml")).use {
        AXMLParser(it).block()
    }
}

private data class Attribute(
    val name: String,
    val value: String
)

private fun AXMLParser.moveToTag(tag: String) {
    var eventType = type

    while (eventType != AXMLParser.END_DOCUMENT) {
        if (eventType == AXMLParser.START_TAG && name == tag) break
        eventType = next()
    }
}

private fun AXMLParser.attributes(): List<Attribute> = (0 until attributeCount)
    .map { Attribute(getAttributeName(it), getAttributeValueString(it)) }
