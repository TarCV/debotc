package com.github.tarcv.zandronum.debotc

import platform.posix.*
import kotlinx.cinterop.*

/**
 * Native implementations of platform-dependent helpers
 */

actual fun String.Companion.format(format: String, arg: Int): String {
    return memScoped {
        val size = snprintf(null, 0, format, arg) + 1
        val buffer = allocArray<ByteVar>(size)
        snprintf(buffer, size.toULong(), format, arg)
        buffer.toKString()
    }
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
    return this.stringFromUtf8OrThrow(start, size)
}

actual fun StringBuilder.insertString(index: Int, string: String): StringBuilder {
    return this.insert(index, string)
}

actual val lineSeparator: String  = "\n" // TODO: get system line separator

actual fun assert(value: Boolean) {
    kotlin.assert(value)
}

actual interface Printer {
    actual fun println(msg: String)
    actual fun close()
    actual fun print(msg: String)
}

actual class FilePrinter actual constructor(path: String) : Printer {
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

actual fun readAllBytes(path: String): UByteArray {
    val file = fopen(path, "rb") ?: throw IllegalStateException("Failed to read file")

    try {
        fseek(file, 0, SEEK_END)
        val size = ftell(file).toInt();
        rewind(file)
        return memScoped {
            val buffer = allocArray<ByteVar>(size + 1)
            val elementsRead = fread(buffer, size.convert<size_t>(), 1, file).toInt()
            if (elementsRead != 1) {
                throw IllegalStateException("Failed to read file")
            }

            buffer.readBytes(size).toUByteArray()
        }
    } finally {
        fclose(file)
    }

}