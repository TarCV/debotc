package com.github.tarcv.zandronum.debotc

import java.nio.charset.StandardCharsets

/**
 * JVM implementations of platform-dependent helpers
 */
val lineSeparator: String = System.lineSeparator()

fun StringBuilder.appendLine(): StringBuilder {
    return this.appendln()
}

fun StringBuilder.appendLine(line: String): StringBuilder {
    return this.appendln(line)
}

fun StringBuilder.appendLine(line: StringBuilder): StringBuilder {
    return this.appendln(line)
}

fun ByteArray.stringFromUtf8BytesOrThrow(start: Int, size: Int): String {
    return String(this, start, size, StandardCharsets.UTF_8)
}

fun StringBuilder.insertString(index: Int, string: String): StringBuilder {
    return this.insert(index, string)
}

fun assert(value: Boolean) {
    kotlin.assert(value)
}
