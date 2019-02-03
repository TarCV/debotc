package com.github.tarcv.zandronum.debotc

import com.github.tarcv.zandronum.debotc.StackChangingNode.AddsTo.*
import com.github.tarcv.zandronum.debotc.StackChangingNode.ArgumentHolder.Companion.createNormalStackArgument
import com.github.tarcv.zandronum.debotc.StackChangingNode.Companion.consumesNormalStack

enum class DataHeaders(requiredArgs: Int = 0)
{
    DH_COMMAND(2) {
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
    },
    DH_IFGOTO(1) {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return IfGotoNode(createNormalStackArgument(0), command.arguments[0])
        }
    },
    DH_IFNOTGOTO(1) {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return IfNotGotoNode(createNormalStackArgument(0), command.arguments[0])
        }
    },
    DH_GOTO(1) {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return GotoNode(command.arguments[0])
        }
    },
    DH_ORLOGICAL {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return OperatorNode("||", 2)
            
        }
    },
    DH_ANDLOGICAL {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return OperatorNode("&&", 2)
            
        }
    },
    DH_ORBITWISE {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return OperatorNode("|", 2)
            
        }
    },
    DH_EORBITWISE {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return OperatorNode("^", 2)
            
        }
    },
    DH_ANDBITWISE {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return OperatorNode("&", 2)
            
        }
    },
    DH_EQUALS {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return OperatorNode("==", 2)
            
        }
    },
    DH_NOTEQUALS {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return OperatorNode("!=", 2)
            
        }
    },
    DH_LESSTHAN {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return OperatorNode("<", 2)
            
        }
    },
    DH_LESSTHANEQUALS {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return OperatorNode("<=", 2)
            
        }
    },
    DH_GREATERTHAN {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return OperatorNode(">", 2)
            
        }
    },
    DH_GREATERTHANEQUALS {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return OperatorNode(">=", 2)
            
        }
    },
    DH_NEGATELOGICAL {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return OperatorNode("!", 1)
        }
    },
    DH_LSHIFT {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return OperatorNode("<<", 2)
            
        }
    },
    DH_RSHIFT {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return OperatorNode(">>", 2)
            
        }
    },
    DH_ADD {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return OperatorNode("+", 2)
            
        }
    },
    DH_SUBTRACT {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return OperatorNode("-", 2)
            
        }
    },
    DH_UNARYMINUS {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return OperatorNode("-", 1)
        }
    },
    DH_MULTIPLY {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return OperatorNode("*", 2)
            
        }
    },
    DH_DIVIDE {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return OperatorNode("/", 2)
            
        }
    },
    DH_MODULUS {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return OperatorNode("%", 2)
        }
    },
    DH_PUSHNUMBER {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return LiteralNode(command.arguments[0].toString(), ADDS_TO_NORMAL_STACK)
        }
    },
    DH_PUSHSTRINGINDEX {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return LiteralNode("\"${vmState.strings[command.arguments[0]]}\"", ADDS_TO_STRING_STACK)
            
        }
    },
    DH_PUSHGLOBALVAR {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return LiteralNode("\$global${command.arguments[0]}", ADDS_TO_NORMAL_STACK)
            
        }
    },
    DH_PUSHLOCALVAR {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return LiteralNode("\$local${command.arguments[0]}", ADDS_TO_NORMAL_STACK)
            
        }
    },
    DH_DROPSTACKPOSITION {
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
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return CommandNode("\$global${command.arguments[0]}++;")
        }
    },
    DH_DECGLOBALVAR(1) {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return CommandNode("\$global${command.arguments[0]}--;")
        }
    },
    DH_ASSIGNGLOBALVAR(1) {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return CustomStackConsumingNode(1, DONT_PUSHES_TO_STACK) { stackArgs -> "\$global${command.arguments[0]} = ${stackArgs[0]};" }
        }
    },
    DH_ADDGLOBALVAR(1) {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return CustomStackConsumingNode(1, DONT_PUSHES_TO_STACK) { stackArgs -> "\$global${command.arguments[0]} += ${stackArgs[0]};" }
        }
    },
    DH_SUBGLOBALVAR(1) {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return CustomStackConsumingNode(1, DONT_PUSHES_TO_STACK) { stackArgs -> "\$global${command.arguments[0]} -= ${stackArgs[0]};" }
        }
    },
    DH_MULGLOBALVAR(1) {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return CustomStackConsumingNode(1, DONT_PUSHES_TO_STACK) { stackArgs -> "\$global${command.arguments[0]} *= ${stackArgs[0]};" }
        }
    },
    DH_DIVGLOBALVAR(1) {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return CustomStackConsumingNode(1, DONT_PUSHES_TO_STACK) { stackArgs -> "\$global${command.arguments[0]} /= ${stackArgs[0]};" }
        }
    },
    DH_MODGLOBALVAR(1) {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return CustomStackConsumingNode(1, DONT_PUSHES_TO_STACK) { stackArgs -> "\$global${command.arguments[0]} %= ${stackArgs[0]};" }
        }
    },
    DH_INCLOCALVAR(1) {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return CustomStackConsumingNode(0, DONT_PUSHES_TO_STACK) { "\$local${command.arguments[0]}++;" }
        }
    },
    DH_DECLOCALVAR(1) {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return CustomStackConsumingNode(0, DONT_PUSHES_TO_STACK) { "\$local${command.arguments[0]}--;" }
        }
    },
    DH_ASSIGNLOCALVAR(1) {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return CustomStackConsumingNode(1, DONT_PUSHES_TO_STACK) { stackArgs -> "\$local${command.arguments[0]} = ${stackArgs[0]};" }
        }
    },
    DH_ADDLOCALVAR(1) {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return CustomStackConsumingNode(1, DONT_PUSHES_TO_STACK) { stackArgs -> "\$local${command.arguments[0]} += ${stackArgs[0]};" }
        }
    },
    DH_SUBLOCALVAR(1) {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return CustomStackConsumingNode(1, DONT_PUSHES_TO_STACK) { stackArgs -> "\$local${command.arguments[0]} -= ${stackArgs[0]};" }
        }
    },
    DH_MULLOCALVAR(1) {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return CustomStackConsumingNode(1, DONT_PUSHES_TO_STACK) { stackArgs -> "\$local${command.arguments[0]} *= ${stackArgs[0]};" }
        }
    },
    DH_DIVLOCALVAR(1) {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return CustomStackConsumingNode(1, DONT_PUSHES_TO_STACK) { stackArgs -> "\$local${command.arguments[0]} /= ${stackArgs[0]};" }
        }
    },
    DH_MODLOCALVAR(1) {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return CustomStackConsumingNode(1, DONT_PUSHES_TO_STACK) { stackArgs -> "\$local${command.arguments[0]} %= ${stackArgs[0]};" }
        }
    },
    DH_CASEGOTO(2) {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return if (vmState.previousCommand != DH_CASEGOTO) {
                SwitchAndCaseNode(createNormalStackArgument(0), command.arguments[0].toString(), command.arguments[1])
            } else {
                CaseGotoNode(command.arguments[0].toString(), command.arguments[1])
            }
        }
    },
    DH_DROP {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return dropFromStack()
        }
    },
    DH_INCGLOBALARRAY(1) {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return CustomStackConsumingNode(1, DONT_PUSHES_TO_STACK) { stackArgs -> "\$globalArray${command.arguments[0]}[${stackArgs[0]}]++;" }
        }
    },
    DH_DECGLOBALARRAY(1) {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return CustomStackConsumingNode(1, DONT_PUSHES_TO_STACK) { stackArgs -> "\$globalArray${command.arguments[0]}[${stackArgs[0]}]--;" }
        }
    },
    DH_ASSIGNGLOBALARRAY(1) {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return CustomStackConsumingNode(2, DONT_PUSHES_TO_STACK) { stackArgs -> "\$globalArray${command.arguments[0]}[${stackArgs[0]}] = ${stackArgs[1]};" }
        }
    },
    DH_ADDGLOBALARRAY(1) {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return CustomStackConsumingNode(2, DONT_PUSHES_TO_STACK) { stackArgs -> "\$globalArray${command.arguments[0]}[${stackArgs[0]}] += ${stackArgs[1]};" }
        }
    },
    DH_SUBGLOBALARRAY(1) {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return CustomStackConsumingNode(2, DONT_PUSHES_TO_STACK) { stackArgs -> "\$globalArray${command.arguments[0]}[${stackArgs[0]}] -= ${stackArgs[1]};" }
        }
    },
    DH_MULGLOBALARRAY(1) {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return CustomStackConsumingNode(2, DONT_PUSHES_TO_STACK) { stackArgs -> "\$globalArray${command.arguments[0]}[${stackArgs[0]}] *= ${stackArgs[1]};" }
        }
    },
    DH_DIVGLOBALARRAY(1) {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return CustomStackConsumingNode(2, DONT_PUSHES_TO_STACK) { stackArgs -> "\$globalArray${command.arguments[0]}[${stackArgs[0]}] /= ${stackArgs[1]};" }
        }
    },
    DH_MODGLOBALARRAY(1) {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return CustomStackConsumingNode(2, DONT_PUSHES_TO_STACK) { stackArgs -> "\$globalArray${command.arguments[0]}[${stackArgs[0]}] %= ${stackArgs[1]};" }
        }
    },
    DH_PUSHGLOBALARRAY(1) {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return CustomStackConsumingNode(1, ADDS_TO_NORMAL_STACK) { stackArgs -> "\$globalArray${command.arguments[0]}[${stackArgs[0]}]" }
        }
    },
    DH_SWAP{
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
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            return FunctionNode("memset", consumesNormalStack(3), DONT_PUSHES_TO_STACK)
        }
    },

    NUM_DATAHEADERS {
        override fun processAndCreateNode(command: Command, vmState: VmState): BaseNode {
            throw IllegalStateException("Must not be called")
        }
    };

    abstract fun processAndCreateNode(command: Command, vmState: VmState): BaseNode
}

private fun dropFromStack(): BaseNode {
    return DropStackNode("// item dropped from stack")
}
