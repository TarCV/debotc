package com.github.tarcv.zandronum.debotc

import java.util.ArrayList

class State constructor(var name: String, val index: Int) {
    constructor() : this("(global)", -1)

    val global: Boolean
        get() = index == -1
    val events = ArrayList<BaseEvent>()
    val currentEvent: BaseEvent
        get() = events[events.size - 1]
}