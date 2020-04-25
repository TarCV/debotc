package com.github.tarcv.zandronum.debotc

import java.nio.file.Files
import java.nio.file.Paths

@ExperimentalUnsignedTypes
fun main(rawArgs: Array<String>) {
    val args = rawArgs.toMutableList()

    val disasmMode = if (args[0] == "--disasm") {
        args.removeAt(0)
        true
    } else {
        false
    }
    if (args.size != 1 && args.size != 2) {
        throw IllegalArgumentException("Wrong arguments${System.lineSeparator()}debotc <script.o> [<script.botc>]")
    }

    val data = Files.readAllBytes(Paths.get(args[0]))
    val data0 = data.toUByteArray()

    val printer = if (args.size == 2) {
        FilePrinter(args[1])
    } else {
        consolePrinter
    }

    printer.use { p ->
        val decompiler = Decompiler()

        if (disasmMode) {
            decompiler.parse(data0, p)
        } else {
            decompiler.parse(data0, null)
            decompiler.print(p)
        }
    }
}