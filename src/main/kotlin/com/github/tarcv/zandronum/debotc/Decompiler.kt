package com.github.tarcv.zandronum.debotc

import com.github.tarcv.zandronum.debotc.BotCommand.NUM_BOTCMDS
import com.github.tarcv.zandronum.debotc.DataHeaders.*
import java.nio.ByteBuffer
import java.nio.ByteOrder.LITTLE_ENDIAN
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.HashSet

class Decompiler {
    companion object {
        @JvmStatic fun main(args: Array<String>) {
            if (args.size != 1) {
                throw IllegalArgumentException("Only one argument - path to compiled script should be passed")
            }
            val data0 = Files.readAllBytes(Paths.get(args[0]))

            val decompiler = Decompiler()
            decompiler.parse(data0)
            decompiler.print()
        }
    }

    private fun print() {
        val tempVarCounter = AtomicInteger()
        states.forEach { state ->
            val name = if (state.global) "" else "state ${state.name} "
            val suffix = if (state.global) "" else " // ${state.index}"
            println("$name{$suffix")
            state.events.forEach { event ->
                println("\tevent ${event.readableType}() {")
                for (i in 0 until event.varList) {
                    println("\t\tint var$i;")
                }
                val vmState = VmState(
                        Stack(),
                        Stack(),
                        strings,
                        states,
                        tempVarCounter
                )
                vmState.indent = 2
                event.commands.forEachIndexed { index, code ->
                    if (vmState.previousCommand == DH_CASEGOTO && code.type != DH_CASEGOTO) {
                        vmState.indentedPrintLn("}")
                    }
                    if (labelPositions.contains(code.positionBefore)) {
                        println("label${code.positionBefore}:")
                    }

                    code.print(vmState)

                    if (event.commands.lastIndex == index && labelPositions.contains(code.positionAfter)) {
                        println("label${code.positionAfter}:")
                    }

                    vmState.previousCommand = code.type
                }
                println("\t}")
            }
            println("}")
        }
    }

    private fun parse(data0: ByteArray) {
        if (alreadyParsed) {
            throw IllegalStateException("parse() can be called only once")
        }
        alreadyParsed = true

        data = Data(data0)

        parseCommands()
        normalizeStates()
    }

    private fun normalizeStates() {
        states.sortBy { state -> state.index }

        for (expectedIndex in -1 .. states.size - 2) {
            if (states.get(expectedIndex + 1).index != expectedIndex) {
                throw IllegalStateException("State index $expectedIndex is missing")
            }
        }
    }

    private fun parseCommands() {
        while (data.offset < data.data.size) {
            val offset = data.offset
            val index = data.readSigned32()
            val commandHeader: DataHeaders = toEnum(index)
//            println("$offset:${commandHeader.name}")
            when (commandHeader) {
                DH_COMMAND -> parseBotCommand(data)
                DH_ONENTER, DH_MAINLOOP, DH_ONEXIT -> parseEventHandler(commandHeader, data)
                DH_EVENT -> parseEvent(data)
                DH_ENDONENTER, DH_ENDMAINLOOP, DH_ENDONEXIT, DH_ENDEVENT -> finalizeEvent()
                DH_STATENAME -> parseStateName(data)
                DH_STATEIDX -> throw IllegalStateException("DH_STATEIDX without DH_STATENAME is not supported")
                DH_PUSHNUMBER, DH_PUSHSTRINGINDEX, DH_PUSHGLOBALVAR, DH_PUSHLOCALVAR, DH_INCGLOBALVAR, DH_DECGLOBALVAR,
                DH_ASSIGNGLOBALVAR, DH_ADDGLOBALVAR, DH_SUBGLOBALVAR, DH_MULGLOBALVAR,
                DH_DIVGLOBALVAR, DH_MODGLOBALVAR, DH_INCLOCALVAR, DH_DECLOCALVAR, DH_ASSIGNLOCALVAR,
                DH_ADDLOCALVAR, DH_SUBLOCALVAR, DH_MULLOCALVAR, DH_DIVLOCALVAR, DH_MODLOCALVAR,
                DH_INCGLOBALARRAY, DH_DECGLOBALARRAY, DH_ASSIGNGLOBALARRAY, DH_ADDGLOBALARRAY,
                DH_SUBGLOBALARRAY, DH_MULGLOBALARRAY, DH_DIVGLOBALARRAY, DH_MODGLOBALARRAY,
                DH_PUSHGLOBALARRAY
                -> parseCommandWithArg(commandHeader, 1)
                DH_IFGOTO, DH_IFNOTGOTO, DH_GOTO
                -> parseCommandWithArg(commandHeader, 1, isGoto = true)
                DH_SCRIPTVARLIST -> parseScriptVarList()
                DH_CASEGOTO -> parseCommandWithArg(commandHeader, 2, isGoto = true)
                DH_ORLOGICAL, DH_ANDLOGICAL, DH_ORBITWISE, DH_EORBITWISE, DH_ANDBITWISE,
                DH_EQUALS, DH_NOTEQUALS, DH_DROPSTACKPOSITION, DH_LESSTHAN, DH_LESSTHANEQUALS,
                DH_GREATERTHAN, DH_GREATERTHANEQUALS, DH_NEGATELOGICAL, DH_LSHIFT, DH_RSHIFT,
                DH_ADD, DH_SUBTRACT, DH_UNARYMINUS, DH_MULTIPLY, DH_DIVIDE, DH_MODULUS, DH_DROP,
                DH_SWAP, DH_ARRAYSET
                -> parseCommandWithArg(commandHeader, 0)
                DH_STRINGLIST -> addStrings()
                else -> throw IllegalStateException("Unexpected header $commandHeader at ${data.offset}")
            }
        }
    }

    private fun parseScriptVarList() {
        currentState.currentEvent.varList = data.readSigned32()
    }

    private fun finalizeEvent() {
        currentState.currentEvent.finalize()
    }

    private fun addStrings() {
        val numStrings = data.readSigned32()
        if (strings.isNotEmpty()) {
            throw IllegalStateException("Strings were already added")
        }
        for (i in 0 until numStrings) {
            val length = data.readSigned32()
            val str = data.readSzString(length)
            strings.add(str)
        }
    }

    private fun parseCommandWithArg(commandHeader: DataHeaders, numberArguments: Int, isGoto: Boolean = false) {
        val positionBefore = data.offset - 4
        val args = IntArray(numberArguments)
        for (i in 0 until numberArguments) {
            args[i] = data.readSigned32()
        }
        if (isGoto) {
            labelPositions.add(args.last())
        }
        currentState.currentEvent.addCommand(Command(positionBefore, data.offset, commandHeader, *args))
    }

    private fun parseStateName(data: Data) {
        val size = data.readSigned32()
        val name = data.readSzString(size)
        val stateIndexHeader = data.readSigned32()
        if (stateIndexHeader != DH_STATEIDX.ordinal) {
            throw IllegalStateException("Expected state index after state, $name")
        }

        val stateIndex = data.readSigned32()
        if (stateIndex < 0) {
            throw IllegalStateException("Illegal stateIndex: $stateIndex")
        }

        states.add(com.github.tarcv.zandronum.debotc.State(name, stateIndex))
    }

    private fun parseBotCommand(data: Data) {
        val positionBefore = data.offset - 4
        val command = data.readSigned32()
        if (command < 0 || command >= NUM_BOTCMDS.ordinal) {
            throw IllegalStateException("Illegal command $command in ${currentState.name}")
        }
        val argument = data.readSigned32()
        currentState.currentEvent.addCommand(Command(positionBefore, data.offset, DH_COMMAND, command, argument))
    }

    private fun parseEvent(data: Data) {
        val eventType: BotEventType = toEnum(data.readSigned32())
        if (currentState.global) {
            if (currentState.events.size >= MAX_NUM_GLOBAL_EVENTS) {
                throw IllegalStateException("Too many global events in the bot script")
            }
        } else {
            if (currentState.events.size >= MAX_NUM_EVENTS) {
                throw IllegalStateException("Too many local events in the bot script")
            }
        }
        currentState.events.add(BotEvent(data.offset, eventType))
    }

    private fun parseEventHandler(event: DataHeaders, data: Data) {
        if (currentState.global) {
            throw IllegalStateException("$event outside of a state definition")
        }

        currentState.events.add(WorldEvent(data.offset, event))
    }

    private var alreadyParsed: Boolean = false
    private lateinit var data: Data
    private val states: MutableList<State>
    private val currentState: State
        get() = states[states.size - 1]
    private val strings = ArrayList<String>()
    private val labelPositions = HashSet<Int>()


    init {
        states = ArrayList()
        states.add(State())
    }
}
const val MAX_NUM_GLOBAL_EVENTS = 32
const val MAX_NUM_EVENTS = 32

inline fun <reified T : Enum<T>> toEnum(index: Int): T {
    val enumValues = enumValues<T>()
    if (index < 0 || index >= enumValues.size) {
        throw IllegalArgumentException("Illegal value $index for ${T::class.java.simpleName}")
    }
    return enumValues[index]
}

class VmState(
        private val intStack: Stack<String>,
        private val stringStack: Stack<String>,
        val strings: List<String>,
        val states: List<State>,
        val tempVarIndex: AtomicInteger
) {
    fun pushIntAsIs(result: Int) {
        pushIntAsIs(result.toString())
    }

    fun pushIntAsIs(result: String) {
        intStack.push(result)
    }

    fun pushIntAsTemporary(result: String) {
        val tempIndex = tempVarIndex.getAndIncrement()
        indentedPrintLn("int temp$tempIndex = $result;")
        intStack.push("temp$tempIndex")
    }

    fun pushStrAsVar(result: String) {
        val tempIndex = tempVarIndex.getAndIncrement()
        indentedPrintLn("str temp$tempIndex = $result;")
        stringStack.push("temp$tempIndex")
    }

    fun tryPopIntFromStack(): String {
        return tryPopFromStack(intStack)
    }

    fun tryPopStrFromStack(): String {
        return tryPopFromStack(stringStack)
    }

    fun pushStrAsIs(s: String) {
        stringStack.push(s)
    }

    fun indentedPrintLn(s: String) {
        println("\t".repeat(indent) + s)
    }

    var previousCommand: DataHeaders = DH_STATEIDX
    var indent: Int = 0
}

class Data(
        val data: ByteArray
) {
    fun readSigned32(): Int {
        return readToBuffer(4).int
    }

    private fun readToBuffer(size: Int): ByteBuffer {
        val buf = ByteBuffer.wrap(data, offset, size).slice()
        buf.order(LITTLE_ENDIAN)
        offset += size
        return buf
    }

    private fun uByte(byte: Byte): Int {
        var result: Int = byte.toInt()
        if (result < 0) {
            result += 0x100
        }
        return result
    }

    fun readSzString(size: Int): String {
        val buf = readToBuffer(size)
        return String(buf.array(), buf.arrayOffset(), buf.limit() - buf.position())
    }

    var offset: Int = 0
        private set
}

fun tryPopFromStack(stack: Stack<out Any>): String {
    return if (stack.empty()) {
        "?"
    } else {
        stack.pop().toString()
    }
}
