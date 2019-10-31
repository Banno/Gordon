package com.banno.gordon

import arrow.fx.IO
import org.jf.dexlib.AnnotationDirectoryItem
import org.jf.dexlib.AnnotationItem
import org.jf.dexlib.ClassDefItem
import org.jf.dexlib.DexFile
import org.jf.dexlib.Util.AccessFlags
import java.io.File
import java.util.zip.ZipFile

internal fun loadTestSuite(instrumentationApk: File): IO<List<TestCase>> = IO {
    ZipFile(instrumentationApk).use { zip ->
        zip.entries()
            .toList()
            .filter { it.name.startsWith("classes") && it.name.endsWith(".dex") }
            .map { zipEntry ->
                File.createTempFile("dex", zipEntry.name).also {
                    it.deleteOnExit()
                    it.outputStream().use { output ->
                        zip.getInputStream(zipEntry).use { input -> input.copyTo(output) }
                    }
                }
            }
            .map(::DexFile)
            .flatMap { it.ClassDefsSection.items }
            .filter {
                val isKotlinInterfaceDefaultImplementation = it.className.endsWith("\$DefaultImpls")
                val isInterface = (it.accessFlags and AccessFlags.INTERFACE.value != 0)
                val isAbstract = (it.accessFlags and AccessFlags.ABSTRACT.value != 0)

                !isInterface &&
                        !isAbstract &&
                        !isKotlinInterfaceDefaultImplementation
            }
            .flatMap { classDefItem ->
                (classDefItem.annotations?.methodAnnotations ?: emptyList())
                    .mapNotNull { method ->
                        if (method.isTestMethod) {
                            TestCase(
                                fullyQualifiedClassName = classDefItem.className,
                                methodName = method.method.methodName.stringValue,
                                isIgnored = method.isIgnored || classDefItem.isIgnored
                            )
                        } else null
                    }
            }
    }
}

private val ClassDefItem.className
    get() = classType.typeDescriptor.drop(1).dropLast(1).replace('/', '.')

private val ClassDefItem.isIgnored
    get() = annotations?.classAnnotations?.annotations?.any { it.typeDescriptor == IGNORE_ANNOTATION } ?: false

private val AnnotationDirectoryItem.MethodAnnotation.isIgnored
    get() = annotationSet.annotations?.any { it.typeDescriptor == IGNORE_ANNOTATION } ?: false

private val AnnotationDirectoryItem.MethodAnnotation.isTestMethod
    get() = annotationSet.annotations?.any { it.typeDescriptor == TEST_ANNOTATION } ?: false

private val AnnotationItem.typeDescriptor
    get() = encodedAnnotation.annotationType.typeDescriptor

private const val IGNORE_ANNOTATION = "Lorg/junit/Ignore;"
private const val TEST_ANNOTATION = "Lorg/junit/Test;"
