package com.github.tarcv.zandronum.debotc

import java.util.*

abstract class BaseEvent {
    fun finalize() {
        finalized = true
    }

    fun addCommand(command: Command) {
        if (finalized) {
            throw IllegalStateException("Unexpected command outside any event")
        }
        _commands.add(command)
    }

    abstract val readableType: String

    private val _commands: ArrayList<Command> = ArrayList()
    val commands: List<Command>
        get() = Collections.unmodifiableList(_commands)

    private var finalized = false
        private set
    var varList: Int = 0
}

class BotEvent(
        val eventType: BotEventType
) : BaseEvent() {
    override val readableType: String
        get() = eventType.name.substring("BOTEVENT_".length).toLowerCase()
}

class WorldEvent(
        val eventType: DataHeaders
) : BaseEvent() {
    override val readableType: String
        get() = eventType.name.substring("DH_".length).toLowerCase()
}