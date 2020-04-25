package com.github.tarcv.zandronum.debotc

@ExperimentalUnsignedTypes
fun main(rawArgs: Array<String>) {
    val args = rawArgs.toMutableList()

    val disasmMode = if (args.isNotEmpty() && args[0] == "--disasm") {
        args.removeAt(0)
        true
    } else {
        false
    }
    if (args.size != 1 && args.size != 2) {
        throw IllegalArgumentException("Wrong arguments${lineSeparator}debotc [--disasm] <script.o> [<script.botc>]")
    }

    val data0: UByteArray = readAllBytes(args[0])

    val printer = if (args.size == 2) {
        FilePrinter(args[1])
    } else {
        consolePrinter
    }

    try {
        val decompiler = Decompiler()

        if (disasmMode) {
            decompiler.parse(data0, printer)
        } else {
            decompiler.parse(data0, null)
            decompiler.print(printer)
        }
    } finally {
        printer.close()
    }
}
