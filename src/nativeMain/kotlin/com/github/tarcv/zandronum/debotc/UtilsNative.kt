package com.github.tarcv.zandronum.debotc

import platform.posix.*
import kotlinx.cinterop.*

/**
 * Native implementations of platform-dependent helpers
 */
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
    return this.stringFromUtf8OrThrow(start, size)
}

actual fun StringBuilder.insertString(index: Int, string: String): StringBuilder {
    return this.insert(index, string)
}

actual val lineSeparator: String  = "\n" // TODO: get system line separator

actual fun assert(value: Boolean) {
    kotlin.assert(value)
}

actual class FilePrinter actual constructor(private val path: String) : Printer {
    private val file = fopen(path, "a")

    override fun close() {
        fclose(file)
    }

    override fun print(msg: String) {
        fputs(msg, file)
    }

    override fun println(msg: String) {
        fputs(msg, file)
        fputs(lineSeparator, file)
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