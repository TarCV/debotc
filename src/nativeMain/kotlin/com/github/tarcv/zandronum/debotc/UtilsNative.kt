package com.github.tarcv.zandronum.debotc

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