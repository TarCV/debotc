package com.github.tarcv.zandronum.debotc

class Command(
        val positionBefore: Int,
        val positionAfter: Int,
        val type: DataHeaders, vararg args: Int) {
    fun print(vmState: VmState) {
        type.print(arguments, vmState)
    }

    val arguments: IntArray = args
}
