package com.github.tarcv.zandronum.debotc

class Command(
        val positionBefore: Int,
        val positionAfter: Int,
        val type: DataHeaders, vararg args: Int) {
    fun processAndCreateNode(vmState: VmState): BaseNode {
        return type.processAndCreateNode(this, vmState)
    }

    val arguments: IntArray = args
}
