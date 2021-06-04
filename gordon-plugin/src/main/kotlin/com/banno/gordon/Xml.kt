package com.banno.gordon

internal class XmlElement(
    private val name: String,
    private val indentation: Int = 0,
    private val text: String? = null,
    private val attributes: MutableMap<String, String> = mutableMapOf(),
    private val children: MutableList<XmlElement> = mutableListOf()
) {

    fun attribute(name: String, value: String) {
        attributes[name] = value
    }

    fun element(name: String, block: XmlElement.() -> Unit = {}) {
        children.add(XmlElement(name, indentation + 1).apply(block))
    }

    fun element(name: String, text: String, block: XmlElement.() -> Unit = {}) {
        children.add(XmlElement(name, indentation + 1, text).apply(block))
    }

    override fun toString(): String = StringBuilder().run {
        append(TAB.repeat(indentation))
        append("<$name")
        attributes.forEach { append(" ${it.key}=\"${it.value.escape()}\"") }
        if (text == null && children.isEmpty()) {
            append("/>")
        } else {
            if (text != null) {
                append(">")
                append(text.escape())
            } else {
                appendLine(">")
                children.forEach { appendLine(it.toString()) }
                append(TAB.repeat(indentation))
            }
            append("</$name>")
        }
        toString()
    }

    companion object {
        private const val TAB = "  "

        private fun String.escape() = this
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("&", "&amp;")
            .replace("'", "&apos;")
            .replace("\"", "&quot;")
    }
}

internal fun xmlDocument(
    rootElementName: String,
    rootElementText: String?,
    block: XmlElement.() -> Unit = {}
): String = StringBuilder().run {
    appendLine("<?xml version='1.0' encoding='UTF-8'?>")
    appendLine(XmlElement(name = rootElementName, text = rootElementText).apply(block).toString())
    toString()
}

internal fun xmlDocument(rootElementName: String, block: XmlElement.() -> Unit = {}) =
    xmlDocument(rootElementName, null, block)
