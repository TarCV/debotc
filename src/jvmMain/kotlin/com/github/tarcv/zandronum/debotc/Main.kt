package com.github.tarcv.zandronum.debotc

import java.nio.file.Files
import java.nio.file.Paths

@ExperimentalUnsignedTypes
fun main(args: Array<String>) {
    if (args.size != 1) {
        throw IllegalArgumentException("Only one argument - path to compiled script should be passed")
    }

    val data = Files.readAllBytes(Paths.get(args[0]))
    val data0 = data.toUByteArray()

    val decompiler = Decompiler()
    decompiler.parse(data0)
    decompiler.print()
}