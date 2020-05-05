package com.github.tarcv.zandronum.debotc

import java.nio.file.Files
import java.nio.file.Paths

@ExperimentalUnsignedTypes
fun main(args: Array<String>) {
    if (args.size != 1 && args.size != 2) {
        throw IllegalArgumentException("Wrong arguments${System.lineSeparator()}debotc <script.o> [<script.botc>]")
    }

    val data = Files.readAllBytes(Paths.get(args[0]))
    val data0 = data.toUByteArray()

    val decompiler = Decompiler()
    decompiler.parse(data0)

    val printer = if (args.size == 2) {
        FilePrinter(args[1])
    } else {
        consolePrinter
    }
    try {
        decompiler.print(printer)
    } finally {
        printer.close()
    }
}