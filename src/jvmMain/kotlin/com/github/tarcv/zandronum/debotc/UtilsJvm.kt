package com.github.tarcv.zandronum.debotc

import java.io.Closeable
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

/**
 * JVM implementations of platform-dependent helpers
 */
actual val lineSeparator: String = System.lineSeparator()

actual fun String.Companion.format(format: String, arg: Int): String {
    return String.format(format, arg)
}

actual fun StringBuilder.appendLine(): StringBuilder {
    return this.appendln()
}

actual fun StringBuilder.appendLine(line: String): StringBuilder {
    return this.appendln(line)
}

actual fun StringBuilder.appendLine(line: StringBuilder): StringBuilder {
    return this.appendln(line)
}

actual fun ByteArray.stringFromUtf8BytesOrThrow(start: Int, size: Int): String {
    return String(this, start, size, StandardCharsets.UTF_8)
}

actual fun StringBuilder.insertString(index: Int, string: String): StringBuilder {
    return this.insert(index, string)
}

actual fun assert(value: Boolean) {
    kotlin.assert(value)
}

actual interface Printer: Closeable {
    actual fun println(msg: String)
    actual override fun close()
    actual fun print(msg: String)
}

actual class FilePrinter actual constructor(path: String) : Printer {
    private val writer = File(path).printWriter()

    override fun close() {
        writer.close()
    }

    override fun print(msg: String) {
        writer.print(msg)
    }

    override fun println(msg: String) {
        writer.println(msg)
    }
}

actual object consolePrinter: Printer {
    override fun close() {
        // no op
    }

    override fun print(msg: String) {
        kotlin.io.print(msg)
    }

    override fun println(msg: String) {
        kotlin.io.println(msg)
    }
}

actual fun readAllBytes(path: String): UByteArray
    return Files.readAllBytes(filePath).toUByteArray()
}
