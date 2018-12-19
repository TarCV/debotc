package com.github.tarcv.zandronum.debotc

import java.util.*

abstract class BaseEvent(
        val position: Int
) {
    fun finalize() {
        finalized = true
    }

    fun addCommand(command: Command) {
        if (finalized) {
            throw IllegalStateException("Unexpected command outside any event")
        }
        commands_.add(command)
    }

    abstract val readableType: String

    private val commands_: ArrayList<Command> = ArrayList()
    val commands: List<Command>
        get() = Collections.unmodifiableList(commands_)

    var finalized = false
        private set
    var varList: Int = 0
}

class BotEvent(
        position: Int,
        val eventType: BotEventType
) : BaseEvent(position) {
    override val readableType: String
        get() = eventType.name.substring("BOTEVENT_".length).toLowerCase()
}

class WorldEvent(
        position: Int,
        val eventType: DataHeaders
) : BaseEvent(position) {
    override val readableType: String
        get() = eventType.name.substring("DH_".length).toLowerCase()
}