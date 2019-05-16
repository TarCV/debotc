package com.github.tarcv.zandronum.debotc

import kotlin.collections.ArrayList

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

    abstract val botcTitle: String

    private val _commands: MutableList<Command> = ArrayList<Command>()
    val commands: List<Command>
        get() = _commands

    private var finalized = false
        private set
    var varList: Int = 0
}

class BotEvent(
        private val eventType: BotEventType
) : BaseEvent() {
    override val botcTitle: String
        get() = "event \"${eventType.botcName}\""
}

class WorldEvent(
        private val eventType: DataHeaders
) : BaseEvent() {
    override val botcTitle: String
        get() = eventType.name.substring("DH_".length).toLowerCase()
}