/**
 * Platform-dependent helpers
 */
package com.github.tarcv.zandronum.debotc

expect val lineSeparator: String

expect fun assert(value: Boolean)

expect fun StringBuilder.appendLine(): StringBuilder
expect fun StringBuilder.appendLine(line: String): StringBuilder
expect fun StringBuilder.appendLine(line: StringBuilder): StringBuilder

expect fun StringBuilder.insertString(index: Int, string: String): StringBuilder

expect fun String.Companion.format(format: String, arg: Int): String

expect fun ByteArray.stringFromUtf8BytesOrThrow(start: Int = 0, size: Int = this.size): String

@ExperimentalUnsignedTypes
expect fun readAllBytes(path: String): UByteArray

expect interface Printer {
    fun println(msg: String = "")

    fun close()
    fun print(msg: String)
}
expect class FilePrinter(path: String): Printer
expect object consolePrinter: Printer