package com.github.tarcv.zandronum.debotc

import com.github.tarcv.zandronum.debotc.StackChangingNode.AddsTo.*
import com.github.tarcv.zandronum.debotc.StackChangingNode.ArgumentHolder.Companion.createNormalStackArgument
import com.github.tarcv.zandronum.debotc.StackChangingNode.Companion.consumesNormalStack

@ExperimentalUnsignedTypes
enum class DataHeaders(requiredArgs: Int = 0)
{
    DH_COMMAND(2) {
        override fun parseCommand(parseState: ParseState): Command? {
            val positionBefore = parseState.data.offset - 4
            val command = parseState.data.readSigned32()
            if (command < 0 || command >= BotCommand.NUM_BOTCMDS.ordinal) {
                throw IllegalStateException("Illegal command $command at offset $positionBefore")
            }
            val argument = parseState.data.readSigned32()

            return Command(positionBefore, parseState.data.offset, DH_COMMAND, command, argument)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            val botCommand: BotCommand = toEnum(command.arguments[0])
            val expectedSecondArg = botCommand.numArgs + botCommand.numStringArgs
            if (expectedSecondArg != command.arguments[1]) {
                throw IllegalStateException(
                        "Second arg of DH_COMMAND (num of stack pops) is out of date:" +
                                " ${command.arguments[1]} != $expectedSecondArg for ${botCommand.readableName}")
            }
            return botCommand.createNode()
        }
    },
    DH_STATEIDX {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode = throw IllegalStateException()
    },
    DH_STATENAME {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode = throw IllegalStateException()
    },
    DH_ONENTER {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode = throw IllegalStateException()
    },
    DH_MAINLOOP {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode = throw IllegalStateException()
    },
    DH_ONEXIT {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode = throw IllegalStateException()
    },
    DH_EVENT {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode = throw IllegalStateException()

        override fun describeReadableForDisasm(command: Command): String {
            throw IllegalStateException("Should not happen")
        }
    },
    DH_ENDONENTER {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode = throw IllegalStateException()
    },
    DH_ENDMAINLOOP {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode = throw IllegalStateException()
    },
    DH_ENDONEXIT {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode = throw IllegalStateException()
    },
    DH_ENDEVENT {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode = throw IllegalStateException()

        override fun describeReadableForDisasm(command: Command): String = "}"
    },
    DH_IFGOTO(1) {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 1, isGoto = true)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return IfGotoNode(createNormalStackArgument(0), command.arguments[0])
        }
    },
    DH_IFNOTGOTO(1) {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 1, isGoto = true)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return IfNotGotoNode(createNormalStackArgument(0), command.arguments[0])
        }
    },
    DH_GOTO(1) {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 1, isGoto = true)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return GotoNode(command.arguments[0])
        }
    },
    DH_ORLOGICAL {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 0)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return OperatorNode("||", 2)
            
        }
    },
    DH_ANDLOGICAL {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 0)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return OperatorNode("&&", 2)
            
        }
    },
    DH_ORBITWISE {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 0)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return OperatorNode("|", 2)
            
        }
    },
    DH_EORBITWISE {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 0)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return OperatorNode("^", 2)
            
        }
    },
    DH_ANDBITWISE {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 0)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return OperatorNode("&", 2)
            
        }
    },
    DH_EQUALS {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 0)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return OperatorNode("==", 2)
            
        }
    },
    DH_NOTEQUALS {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 0)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return OperatorNode("!=", 2)
            
        }
    },
    DH_LESSTHAN {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 0)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return OperatorNode("<", 2)
            
        }
    },
    DH_LESSTHANEQUALS {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 0)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return OperatorNode("<=", 2)
            
        }
    },
    DH_GREATERTHAN {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 0)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return OperatorNode(">", 2)
            
        }
    },
    DH_GREATERTHANEQUALS {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 0)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return OperatorNode(">=", 2)
            
        }
    },
    DH_NEGATELOGICAL {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 0)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return OperatorNode("!", 1)
        }
    },
    DH_LSHIFT {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 0)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return OperatorNode("<<", 2)
            
        }
    },
    DH_RSHIFT {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 0)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return OperatorNode(">>", 2)
            
        }
    },
    DH_ADD {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 0)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return OperatorNode("+", 2)
            
        }
    },
    DH_SUBTRACT {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 0)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return OperatorNode("-", 2)
            
        }
    },
    DH_UNARYMINUS {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 0)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return OperatorNode("-", 1)
        }
    },
    DH_MULTIPLY {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 0)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return OperatorNode("*", 2)
            
        }
    },
    DH_DIVIDE {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 0)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return OperatorNode("/", 2)
            
        }
    },
    DH_MODULUS {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 0)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return OperatorNode("%", 2)
        }
    },
    DH_PUSHNUMBER {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 1)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return LiteralNode(command.arguments[0].toString(), ADDS_TO_NORMAL_STACK)
        }
    },
    DH_PUSHSTRINGINDEX {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 1)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return LiteralNode("\"${vmState.strings[command.arguments[0]]}\"", ADDS_TO_STRING_STACK)
        }

        override fun describeReadableForDisasm(command: Command): String {
            val cmd = command as Command
            return "stringStack.push(strings[${cmd.arguments[0]}])"
        }
    },
    DH_PUSHGLOBALVAR {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 1)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            vmState.defineGlobalVariable(command.arguments[0])
            return LiteralNode("\$global${command.arguments[0]}", ADDS_TO_NORMAL_STACK)
            
        }
    },
    DH_PUSHLOCALVAR {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 1)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            vmState.defineStateVariable(command.arguments[0])
            return LiteralNode("\$local${command.arguments[0]}", ADDS_TO_NORMAL_STACK)
            
        }
    },
    DH_DROPSTACKPOSITION {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 0)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return dropFromStack()
        }
    },
    DH_SCRIPTVARLIST {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return CommandNode("")
        }
    },
    DH_STRINGLIST {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return CommandNode("")
        }
    },
    DH_INCGLOBALVAR(1) {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 1)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            vmState.defineGlobalVariable(command.arguments[0])
            return CommandNode("\$global${command.arguments[0]}++;")
        }
    },
    DH_DECGLOBALVAR(1) {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 1)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            vmState.defineGlobalVariable(command.arguments[0])
            return CommandNode("\$global${command.arguments[0]}--;")
        }
    },
    DH_ASSIGNGLOBALVAR(1) {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 1)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            vmState.defineGlobalVariable(command.arguments[0])
            return CustomStackConsumingNode(1, DOES_NOT_PUSH_TO_STACK) { stackArgs -> "\$global${command.arguments[0]} = ${stackArgs[0]};" }
        }
    },
    DH_ADDGLOBALVAR(1) {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 1)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            vmState.defineGlobalVariable(command.arguments[0])
            return CustomStackConsumingNode(1, DOES_NOT_PUSH_TO_STACK) { stackArgs -> "\$global${command.arguments[0]} += ${stackArgs[0]};" }
        }
    },
    DH_SUBGLOBALVAR(1) {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 1)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            vmState.defineGlobalVariable(command.arguments[0])
            return CustomStackConsumingNode(1, DOES_NOT_PUSH_TO_STACK) { stackArgs -> "\$global${command.arguments[0]} -= ${stackArgs[0]};" }
        }
    },
    DH_MULGLOBALVAR(1) {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 1)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            vmState.defineGlobalVariable(command.arguments[0])
            return CustomStackConsumingNode(1, DOES_NOT_PUSH_TO_STACK) { stackArgs -> "\$global${command.arguments[0]} *= ${stackArgs[0]};" }
        }
    },
    DH_DIVGLOBALVAR(1) {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 1)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            vmState.defineGlobalVariable(command.arguments[0])
            return CustomStackConsumingNode(1, DOES_NOT_PUSH_TO_STACK) { stackArgs -> "\$global${command.arguments[0]} /= ${stackArgs[0]};" }
        }
    },
    DH_MODGLOBALVAR(1) {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 1)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            vmState.defineGlobalVariable(command.arguments[0])
            return CustomStackConsumingNode(1, DOES_NOT_PUSH_TO_STACK) { stackArgs -> "\$global${command.arguments[0]} %= ${stackArgs[0]};" }
        }
    },
    DH_INCLOCALVAR(1) {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 1)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            vmState.defineStateVariable(command.arguments[0])
            return CustomStackConsumingNode(0, DOES_NOT_PUSH_TO_STACK) { "\$local${command.arguments[0]}++;" }
        }
    },
    DH_DECLOCALVAR(1) {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 1)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            vmState.defineStateVariable(command.arguments[0])
            return CustomStackConsumingNode(0, DOES_NOT_PUSH_TO_STACK) { "\$local${command.arguments[0]}--;" }
        }
    },
    DH_ASSIGNLOCALVAR(1) {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 1)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            vmState.defineStateVariable(command.arguments[0])
            return CustomStackConsumingNode(1, DOES_NOT_PUSH_TO_STACK) { stackArgs -> "\$local${command.arguments[0]} = ${stackArgs[0]};" }
        }
    },
    DH_ADDLOCALVAR(1) {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 1)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            vmState.defineStateVariable(command.arguments[0])
            return CustomStackConsumingNode(1, DOES_NOT_PUSH_TO_STACK) { stackArgs -> "\$local${command.arguments[0]} += ${stackArgs[0]};" }
        }
    },
    DH_SUBLOCALVAR(1) {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 1)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            vmState.defineStateVariable(command.arguments[0])
            return CustomStackConsumingNode(1, DOES_NOT_PUSH_TO_STACK) { stackArgs -> "\$local${command.arguments[0]} -= ${stackArgs[0]};" }
        }
    },
    DH_MULLOCALVAR(1) {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 1)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            vmState.defineStateVariable(command.arguments[0])
            return CustomStackConsumingNode(1, DOES_NOT_PUSH_TO_STACK) { stackArgs -> "\$local${command.arguments[0]} *= ${stackArgs[0]};" }
        }
    },
    DH_DIVLOCALVAR(1) {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 1)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            vmState.defineStateVariable(command.arguments[0])
            return CustomStackConsumingNode(1, DOES_NOT_PUSH_TO_STACK) { stackArgs -> "\$local${command.arguments[0]} /= ${stackArgs[0]};" }
        }
    },
    DH_MODLOCALVAR(1) {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 1)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            vmState.defineStateVariable(command.arguments[0])
            return CustomStackConsumingNode(1, DOES_NOT_PUSH_TO_STACK) { stackArgs -> "\$local${command.arguments[0]} %= ${stackArgs[0]};" }
        }
    },
    DH_CASEGOTO(2) {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 2, isGoto = true)
        }

        override fun describeReadableForDisasm(command: Command): String {
            return "if (stack[0] == ${command.arguments[0]}) goto label${String.format("%X", command.arguments[1])}"
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return if (vmState.previousCommand != DH_CASEGOTO) {
                SwitchAndCaseNode(createNormalStackArgument(0), command.arguments[0].toString(), command.arguments[1])
            } else {
                CaseGotoNode(command.arguments[0].toString(), command.arguments[1])
            }
        }
    },
    DH_DROP {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 0)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return dropFromStack()
        }
    },
    DH_INCGLOBALARRAY(1) {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 1)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return CustomStackConsumingNode(1, DOES_NOT_PUSH_TO_STACK) { stackArgs ->
                vmState.defineGlobalArray(command.arguments[0])
                "\$globalArray${command.arguments[0]}[${stackArgs[0]}]++;"
            }
        }
    },
    DH_DECGLOBALARRAY(1) {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 1)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return CustomStackConsumingNode(1, DOES_NOT_PUSH_TO_STACK) { stackArgs ->
                vmState.defineGlobalArray(command.arguments[0])
                "\$globalArray${command.arguments[0]}[${stackArgs[0]}]--;" }
        }
    },
    DH_ASSIGNGLOBALARRAY(1) {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 1)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return CustomStackConsumingNode(2, DOES_NOT_PUSH_TO_STACK) { stackArgs ->
                vmState.defineGlobalArray(command.arguments[0])
                "\$globalArray${command.arguments[0]}[${stackArgs[0]}] = ${stackArgs[1]};" }
        }
    },
    DH_ADDGLOBALARRAY(1) {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 1)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return CustomStackConsumingNode(2, DOES_NOT_PUSH_TO_STACK) { stackArgs ->
                vmState.defineGlobalArray(command.arguments[0])
                "\$globalArray${command.arguments[0]}[${stackArgs[0]}] += ${stackArgs[1]};" }
        }
    },
    DH_SUBGLOBALARRAY(1) {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 1)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return CustomStackConsumingNode(2, DOES_NOT_PUSH_TO_STACK) { stackArgs ->
                vmState.defineGlobalArray(command.arguments[0])
                "\$globalArray${command.arguments[0]}[${stackArgs[0]}] -= ${stackArgs[1]};" }
        }
    },
    DH_MULGLOBALARRAY(1) {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 1)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return CustomStackConsumingNode(2, DOES_NOT_PUSH_TO_STACK) { stackArgs ->
                vmState.defineGlobalArray(command.arguments[0])
                "\$globalArray${command.arguments[0]}[${stackArgs[0]}] *= ${stackArgs[1]};" }
        }
    },
    DH_DIVGLOBALARRAY(1) {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 1)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return CustomStackConsumingNode(2, DOES_NOT_PUSH_TO_STACK) { stackArgs ->
                vmState.defineGlobalArray(command.arguments[0])
                "\$globalArray${command.arguments[0]}[${stackArgs[0]}] /= ${stackArgs[1]};" }
        }
    },
    DH_MODGLOBALARRAY(1) {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 1)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return CustomStackConsumingNode(2, DOES_NOT_PUSH_TO_STACK) { stackArgs ->
                vmState.defineGlobalArray(command.arguments[0])
                "\$globalArray${command.arguments[0]}[${stackArgs[0]}] %= ${stackArgs[1]};" }
        }
    },
    DH_PUSHGLOBALARRAY(1) {
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 1)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return CustomStackConsumingNode(1, ADDS_TO_NORMAL_STACK) { stackArgs ->
                vmState.defineGlobalArray(command.arguments[0])
                "\$globalArray${command.arguments[0]}[${stackArgs[0]}]" }
        }
    },
    DH_SWAP{
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 0)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return object : StackChangingNode(
                    consumesNormalStack(2),
                    arrayOf(
                            ReturnPrototype(ADDS_TO_NORMAL_STACK, {arguments -> "${arguments[1]}"}),
                            ReturnPrototype(ADDS_TO_NORMAL_STACK, {arguments -> "${arguments[0]}"})
                    )
            ) {
                override val asText: String
                    get() = "(swap last stack items)"
            }
        }
    },
    DH_DUP{
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return object : StackChangingNode(
                    consumesNormalStack(1),
                    arrayOf(
                            ReturnPrototype(ADDS_TO_NORMAL_STACK, {arguments -> "${arguments[0]}"}),
                            ReturnPrototype(ADDS_TO_NORMAL_STACK, {arguments -> "${arguments[0]}"})
                    )

            ) {
                override val asText: String
                    get() = "(duplicate stack item)"
            }
        }
    },
    DH_ARRAYSET{
        override fun parseCommand(parseState: ParseState): Command? {
            return parseCommandWithArg(parseState, this, 0)
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return FunctionNode("memset", consumesNormalStack(3), DOES_NOT_PUSH_TO_STACK)
        }
    },

    NUM_DATAHEADERS {
        override fun parseCommand(parseState: ParseState): Command? {
            throw IllegalStateException("Must not be called")
        }

        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            throw IllegalStateException("Must not be called")
        }
    };

    abstract fun processAndCreateNode(command: Command, vmState: VmState): BaseNode

    open fun parseCommand(parseState: ParseState): Command? = null

    open fun describeReadableForDisasm(command: Command): String {
        var text = processAndCreateNode(command, VmState(emptyList(), HashSet(), HashSet(), HashSet())).asText
        while (text.endsWith(";")) {
            text = text.removeSuffix(";").trim()
        }
        while( text.startsWith("(") && text.endsWith(")")) {
            text = text.removePrefix("(").removeSuffix(")").trim()
        }
        if (text.startsWith("stack = ")) {
            text = "stack.push( ${text.removePrefix("stack = ")} )"
        }
        if (text.contains("stack[") || text.contains("stringStack")) {
            text += " // stack arguments are popped by the function"
        }
        text = text.replace(Regex("(?<=[\\s|^])goto label(?=[\\dA-F])"), "goto 0x")
        return text
    }

    fun describeForDisasm(command: Command?): DisasmDescription {
        if (command != null) {
            return DisasmDescription(
                    command.arguments.joinToString(", ") ?: "",
                    describeReadableForDisasm(command)
            )
        } else {
            throw IllegalStateException("Should not be called for non commands")
        }
    }
}

class DisasmDescription(
        val arguments: String,
        val readable: String
)

private fun dropFromStack(): BaseNode {
    return DropStackNode("// item dropped from stack")
}

@ExperimentalUnsignedTypes
private fun parseCommandWithArg(parseState: ParseState, commandHeader: DataHeaders, numberArguments: Int, isGoto: Boolean = false): Command {
    val positionBefore = parseState.data.offset - 4
    val args = IntArray(numberArguments)
    for (i in 0 until numberArguments) {
        args[i] = parseState.data.readSigned32()
    }
    if (isGoto) {
        parseState.labelPositions.add(args.last())
    }
    return Command(positionBefore, parseState.data.offset, commandHeader, *args)
}