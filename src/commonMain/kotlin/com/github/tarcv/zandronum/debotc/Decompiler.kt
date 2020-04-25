package com.github.tarcv.zandronum.debotc

import com.github.tarcv.zandronum.debotc.DataHeaders.*
import com.github.tarcv.zandronum.debotc.LiteralNode.Companion.consumedMarker
import com.github.tarcv.zandronum.debotc.StackChangingNode.AddsTo.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet
import kotlin.text.RegexOption.*
import kotlin.math.max

@ExperimentalUnsignedTypes
class Decompiler {

    fun print(printer: Printer) {
        if (!alreadyParsed) {
            throw IllegalStateException("print() can be called only after parsing")
        }

        printer.println("#!botc 1.0.0")
        printer.println("#include \"debotc_defs.bts\"")
        printer.println()

        val globalVariables = HashSet<Int>()
        val globalArrays = HashSet<Int>()

        // Have to scan for all global variables before printing
        states
                .map { state ->
                    val stateBuilder = StringBuilder()

                    val stateHeader = if (state.global) "" else "state \"${state.name}\": // ${state.index}"
                    stateBuilder.appendLine(stateHeader)

                    val stateVariables = HashSet<Int>()

                    // Have to scan for all global and state variables before printing
                    val stateScripts = state.events
                            .map { event ->
                                val eventScriptBuilder = StringBuilder()

                                eventScriptBuilder.appendLine("\t${event.botcTitle} {")

                                val eventNodes = buildGraphForEvent(event, globalVariables, globalArrays, stateVariables)
                                if (eventNodes.isNotEmpty()) {
                                    val currentNode = eventNodes[0]
                                    val script = compactAndStringifyNodes(currentNode)
                                    eventScriptBuilder.appendLine(script)
                                }

                                eventScriptBuilder.appendLine("\t}")
                            }
                            .fold(initBlockForState(stateVariables)) { acc, script ->
                                acc.appendLine(script)
                            }
                    stateBuilder.appendLine(stateScripts)

                    stateBuilder.appendLine()
                }
                .let {
                    printBlockForGlobal(printer, globalVariables, globalArrays)
                    printer.println()
                    it
                }
                .forEach {
                    printer.println(it.toString())
                }
    }

    private fun initBlockForState(stateVariables: Set<Int>): StringBuilder {
        return stateVariables
                .sorted()
                .map { "\tvar int \$local$it;" }
                .fold(StringBuilder()) { acc, variables ->
                    acc.appendLine(variables)
                }
                .appendLine()
    }

    private fun printBlockForGlobal(printer: Printer, globalVariables: Set<Int>, globalArrays: Set<Int>) {
        globalVariables
                .sorted()
                .forEach {
                    printer.println("var int \$global$it;")
                }
        globalArrays
                .sorted()
                .forEach {
                    printer.println("var int \$globalArray$it[];")
                }
    }

    private fun compactAndStringifyNodes(rootNode: BaseNode): String {
        var scriptText = ""

        var wasAtLeastOneChange = false

        wasAtLeastOneChange = optimizeWhile(rootNode) { node ->
            recoverComplexNodes(node)
        } || wasAtLeastOneChange

        wasAtLeastOneChange = optimizeWhile(rootNode) { node ->
            packToTextNodes(node)
        } || wasAtLeastOneChange

        assert(rootNode.outputs.size == 1)
        assert(rootNode.nextNode.inputs.size == 1)
        assert(rootNode.nextNode.outputs.size == 0 || rootNode.nextNode.outputs.size == 1)
        if (rootNode.nextNode.outputs.size == 1) {
            scriptText = (rootNode.nextNode as TextNode).asText.replace(Regex("^", MULTILINE), "\t\t")
            assert(rootNode.nextNode.nextNode is EndNode)
        } else {
            assert(rootNode.nextNode is EndNode)
        }

        return scriptText
    }

    private fun packToTextNodes(node: BaseNode): Boolean {
        var changed = false

        changed = packPairsToTextNodes(node) || changed

        changed = convertToTextNodes(node) || changed

        changed = removeUnusedLabel(node) || changed

        changed = removeUnusedLiterals(node) || changed

        changed = packIfBlockToText(node) || changed

        changed = packSwitchBlockToText(node) || changed

        return changed
    }

    private fun packIfBlockToText(node: BaseNode): Boolean {
        var changed = false

        node.outputs.copy().forEach { nextNode ->
            if (nextNode.outputs.size == 2) {
                var condition: String
                var mainBranch: BaseNode
                var elseBranch: BaseNode
                val endingLabel: BaseNode

                when (nextNode) {
                    is IfNotGotoNode -> {
                        mainBranch = nextNode.nextNode
                        elseBranch = nextNode.jumpTargetNode
                        condition = nextNode.condition.argument.toString()
                    }
                    is IfGotoNode -> {
                        mainBranch = nextNode.jumpTargetNode
                        elseBranch = nextNode.nextNode
                        condition = invertCondition(nextNode.condition.argument.toString())
                    }
                    else -> return@forEach
                }

                if (mainBranch is TextNode
                        && canBeBranchingEnd(elseBranch)
                        && mainBranch.nextNode == elseBranch
                ) {
                    endingLabel = elseBranch
                } else if (canBeBranchingEnd(mainBranch)
                        && elseBranch is TextNode
                        && mainBranch == elseBranch.nextNode) {
                    endingLabel = mainBranch
                    mainBranch = elseBranch
                    elseBranch = endingLabel
                    condition = invertCondition(condition)
                } else if (mainBranch is TextNode
                        && elseBranch is TextNode
                        && mainBranch.nextNode == elseBranch.nextNode
                        && canBeBranchingEnd(mainBranch.nextNode)) {
                    endingLabel = mainBranch.nextNode as LabelNode
                } else {
                    return@forEach
                }

                changed = true

                val elseBranchText = if (elseBranch != endingLabel) {
                    " else {${lineSeparator}${elseBranch.asText.indent()}${lineSeparator}}"
                } else ""
                val text = "if ($condition) {${lineSeparator}" +
                        "${mainBranch.asText.indent()}${lineSeparator}" +
                        "}$elseBranchText"
                val newNode = TextNode(text)

                newNode.nextNode = endingLabel
                ArrayList(nextNode.inputs).forEach {
                    it.outputs.replace(nextNode, newNode)
                }
                if (endingLabel.inputs.contains(nextNode)) {
                    nextNode.outputs.replace(endingLabel, nullNode)
                }
                if (endingLabel.inputs.contains(mainBranch)) {
                    endingLabel.inEdgeFrom[mainBranch].destroy()
                }
                if (endingLabel.inputs.contains(elseBranch)) {
                    endingLabel.inEdgeFrom[elseBranch].destroy()
                }
            }
        }

        return changed
    }

    private fun recoverComplexNodes(node: BaseNode): Boolean {
        var changed = false

        changed = tryLiteralizeNextNode(node) || changed

        changed = joinNextLiteralNodes(node) || changed

        changed = inlineStackArgs(node) || changed

        changed = cleanupLiteralNode(node) || changed

        changed = joinNextSwitchNodes(node) || changed

        changed = removeUnusedLabel(node) || changed

        return changed
    }

    private fun cleanupLiteralNode(node: BaseNode): Boolean {
        var changed = false

        if (node.outputs.size > 0) {
            val nextNode = node.nextNode
            if (nextNode is LiteralNode) {
                val returns = nextNode.returns()
                val notConsumedReturns = returns.filter { !it.consumed }
                if (returns.size != notConsumedReturns.size) {
                    changed = true

                    if (notConsumedReturns.isNotEmpty()) {
                        val newNode = LiteralNode(prototypesFromReturns(notConsumedReturns))
                        replaceNode(nextNode, newNode)
                    } else {
                        cutNode(nextNode)
                    }
                }
            }
        }

        return changed
    }

    class NodeHolder(
            val nodes: ArrayList<BaseNode> = ArrayList(),
            private var prevNode: BaseNode? = null
    ) {
        fun add(node: BaseNode) {
            prevNode = prevNode.let {
                if (it != null) {
                    it.nextNode = node
                }
                node
            }
            nodes.add(node)
        }
    }

    private fun buildGraphForEvent(
            event: BaseEvent,
            globalVariables: MutableSet<Int>,
            globalArrays: MutableSet<Int>,
            stateVariables: MutableSet<Int>
    ): ArrayList<BaseNode> {
        val vmState = VmState(
                strings,
                globalVariables, globalArrays, stateVariables
        )

        val gotos = ArrayList<AbstractGotoNode>()
        val terminatingCommands = ArrayList<TerminatingFunctionNode>()
        val labels = HashMap<Int, LabelNode>()

        val nodeHolder = NodeHolder()

        nodeHolder.add(BeginNode())

        event.commands.forEachIndexed { index, code ->
            if (labelPositions.contains(code.positionBefore)) {
                val labelNode = LabelNode(code.positionBefore)
                labels[code.positionBefore] = labelNode
                nodeHolder.add(labelNode)
            }

            val newNode = code.processAndCreateNode(vmState)
            nodeHolder.add(newNode)
            if (newNode is AbstractGotoNode) {
                gotos.add(newNode)
            } else if (newNode is TerminatingFunctionNode) {
                terminatingCommands.add(newNode)
            }

            if (event.commands.lastIndex == index && labelPositions.contains(code.positionAfter)) {
                val labelNode = LabelNode(code.positionAfter)
                labels[code.positionAfter] = labelNode
                nodeHolder.add(labelNode)
            }

            vmState.previousCommand = code.type
        }

        val endNode = EndNode()
        nodeHolder.add(endNode)

        gotos.forEach {
            val targetByte = it.targetByte
            it.jumpTargetNode = labels[targetByte] ?: throw IllegalStateException("Label${String.format("%X", targetByte)} not found")

            if (it is GotoNode) {
                replaceGotoWithEdge(it)
            }
        }
        terminatingCommands.forEach {
            it.jumpTargetNode = endNode
            replaceGotoWithEdge(it)
        }

        return nodeHolder.nodes
    }

    private fun invertCondition(condition: String): String {
        return if (Regex("^!\\(.+\\)$").matches(condition)) {
            condition.substring(2, condition.length - 1)
        } else {
            "!($condition)"
        }
    }

    fun parse(data0: UByteArray, disasmPrinter: Printer?) {
        if (alreadyParsed) {
            throw IllegalStateException("parse() can be called only once")
        }
        alreadyParsed = true

        data = Data(data0)

        parseCommands(disasmPrinter)
        normalizeStates()
    }

    private fun normalizeStates() {
        states.sortBy { state -> state.index }

        for (expectedIndex in -1 .. states.size - 2) {
            if (states[expectedIndex + 1].index != expectedIndex) {
                throw IllegalStateException("State index $expectedIndex is missing")
            }
        }
    }

    private fun parseCommands(disasmDumper: Printer? = null) {
        val parseState = ParseState(data, labelPositions)
        while (data.offset < data.data.size) {
            val offset = data.offset
            val index = data.readSigned32()
            val commandHeader: DataHeaders = toEnum(index)

            if (disasmDumper != null) {
                disasmDumper.println()
                disasmDumper.print("${String.format("%08X", offset)} | ${commandHeader.name} ")
            }

            val command = commandHeader.parseCommand(parseState)

            val disasmDescription: DisasmDescription
            if (command == null) {
                disasmDescription = when (commandHeader) {
                    DH_ONENTER, DH_MAINLOOP, DH_ONEXIT -> parseEventHandler(commandHeader)
                    DH_EVENT -> parseEvent(data)
                    DH_ENDONENTER, DH_ENDMAINLOOP, DH_ENDONEXIT, DH_ENDEVENT -> finalizeEvent()
                    DH_STATENAME -> parseStateName(data)
                    DH_STATEIDX -> throw IllegalStateException("DH_STATEIDX without DH_STATENAME is not supported")
                    DH_SCRIPTVARLIST -> parseScriptVarList()
                    DH_STRINGLIST -> addStrings()

                    else -> throw IllegalStateException("Unexpected header $commandHeader at ${data.offset}")
                }
            } else {
                disasmDescription = commandHeader.describeForDisasm(command)
                currentState.currentEvent.addCommand(command)
            }

            if (disasmDumper != null) {
                disasmDumper.print(disasmDescription.arguments)
                disasmDumper.print(" ".repeat(max(0, 30 - disasmDescription.arguments.length - commandHeader.name.length)))
                disasmDumper.print(" | ${disasmDescription.readable}")
            }
        }
    }

    private fun parseScriptVarList(): DisasmDescription {
        val varList = data.readSigned32()
        currentState.currentEvent.varList = varList
        return DisasmDescription(
                varList.toString(),
                "// define $varList script variables"
        )
    }

    private fun finalizeEvent(): DisasmDescription {
        eventEnds.add((data.offset - 4).toString())
        currentState.currentEvent.finalize()
        return DisasmDescription(
                "",
                "}"
        )
    }

    private fun addStrings(): DisasmDescription {
        val numStrings = data.readSigned32()
        if (strings.isNotEmpty()) {
            throw IllegalStateException("Strings were already added")
        }
        for (i in 0 until numStrings) {
            val length = data.readSigned32()
            val str = data.readSzString(length)
            strings.add(str)
        }

        val joinedStrings = strings.joinToString(", ") { "\"$it\"" }
        return DisasmDescription(
                "$numStrings, $joinedStrings",
                "strings = {$joinedStrings}"
        )
    }

    private fun parseStateName(data: Data): DisasmDescription {
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

        val state = State(name, stateIndex)
        states.add(state)

        return DisasmDescription(
                "\"$name\" (DH_STATEIDX $stateIndex)",
                "state ${state.name} {"
        )
    }

    private fun parseEvent(data: Data): DisasmDescription {
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
        val event = BotEvent(eventType)
        currentState.events.add(event)

        return DisasmDescription(
                eventType.name,
                "${event.botcTitle} {"
        )
    }

    private fun parseEventHandler(event: DataHeaders): DisasmDescription {
        if (currentState.global) {
            throw IllegalStateException("$event outside of a state definition")
        }

        val event = WorldEvent(event)
        currentState.events.add(event)

        return DisasmDescription(
                "",
                event.botcTitle + " {"
        )
    }

    private var alreadyParsed: Boolean = false
    private lateinit var data: Data
    private val states: MutableList<State>
    private val currentState: State
        get() = states[states.size - 1]
    private val strings = ArrayList<String>()
    private val labelPositions = HashSet<Int>()
    private val eventEnds = HashSet<String>()

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
        throw IllegalArgumentException("Illegal value $index for ${T::class.simpleName}")
    }
    return enumValues[index]
}

class VmState(
        val strings: List<String>,
        internal val globalVariables: MutableSet<Int>,
        internal val globalArrays: MutableSet<Int>,
        internal val stateVariables: MutableSet<Int>
) {
    fun defineGlobalVariable(variableIndex: Int) {
        globalVariables.add(variableIndex)
    }

    fun defineStateVariable(variableIndex: Int) {
        stateVariables.add(variableIndex)
    }

    fun defineGlobalArray(arrayIndex: Int) {
        globalArrays.add(arrayIndex)
    }

    lateinit var previousCommand: DataHeaders
}

@ExperimentalUnsignedTypes
class Data constructor(
        val data: UByteArray
) {
    fun readSigned32(): Int {
        val result: UInt = data[offset + 0]*0x1u +
                data[offset + 1]*0x100u +
                data[offset + 2]*0x10000u +
                data[offset + 3]*0x1000000u
        offset += 4
        return result.toInt()
    }

    fun readSzString(size: Int): String {
        val result = data.asByteArray().stringFromUtf8BytesOrThrow(offset, size)
        offset += size
        return result
    }

    var offset: Int = 0
        private set
}

val changingAddTos = StackChangingNode.AddsTo.values().filter { it != DOES_NOT_PUSH_TO_STACK }
fun inlineStackArgs(node: BaseNode): Boolean {
    var changed = false

    if (node.outputs.size == 1 && node is LiteralNode) {
        val nextNode = node.outputs[0]
        if (nextNode.inputs.size == 1 && nextNode is ConsumesStack) {
            val nextNodeArguments = nextNode.arguments.filter { it.argument is StackChangingNode.StackArgument }
            if (nextNodeArguments.isNotEmpty()) {

                val outputsByAddTo = changingAddTos
                        .map { changingAddTo ->
                            changingAddTo to node.returns()
                                    .filter { it.addsTo == changingAddTo }
                                    .filter { !it.consumed }
                                    .asReversed()
                        }
                        .toMap()

                val hasAllArgs = nextNodeArguments.all { holder ->
                    holder.argument.let {
                        if (it is StackChangingNode.StackArgument) {
                            outputsByAddTo[it.addsTo]!!.size > it.depth
                        } else {
                            true
                        }
                    }
                }

                if (hasAllArgs) {
                    assert(node.arguments.isEmpty()) // LiteralNodes have no dependencies
                    changed = true

                    val maxConsumedDepths = changingAddTos.map { it to -1 }.toMap().toMutableMap()

                    nextNodeArguments.forEach { holder ->
                        holder.argument = holder.argument.let {
                            if (it is StackChangingNode.StackArgument) {
                                val compatibleOutputs = outputsByAddTo[it.addsTo]!!
                                val inlinedReturn = compatibleOutputs[it.depth]
                                maxConsumedDepths[it.addsTo] = max(maxConsumedDepths[it.addsTo]!!, it.depth)
                                node.markReturnAsConsumed(inlinedReturn.index)
                                StackChangingNode.LiteralArgument(inlinedReturn.value)
                            } else {
                                it
                            }
                        }
                    }
                    changingAddTos.forEach { changingAddTo ->
                        val allReturns = node.returns()
                                .filter { it.addsTo == changingAddTo }
                                .asReversed()
                        if (allReturns.isNotEmpty()) {
                            if (allReturns.subList(0, maxConsumedDepths[changingAddTo]!! + 1).any { !it.consumed })
                                throw AssertionError()
                        }
                    }
                    assert(nextNode.arguments.all { it.argument is StackChangingNode.LiteralArgument })

                    tryLiteralizeNextNode(node)
                }
            }
        }
    }
    return changed
}

private fun tryLiteralizeNextNode(node: BaseNode): Boolean {
    var changed = false

    if (node.outputs.size > 0) {
        node.outputs.forEach { nextNode ->
            if (nextNode is StackChangingNode && nextNode.outputs.size == 1) {
                val allArgsAreStatic = nextNode.arguments.all { it.argument is StackChangingNode.LiteralArgument }

                if (allArgsAreStatic) {
                    // We can only replace function nodes (nodes returning values) with literal
                    //  as far as all dangerous commands are statements (don't return any values)
                    assert(BotCommand.BOTCMD_DELAY.returnType == BotCommandReturnType.RETURNVAL_VOID)

                    val returns = nextNode.returns()
                    val replacingNode = when (returns.count { it.addsTo != DOES_NOT_PUSH_TO_STACK }) {
                        0 -> {
                            if (returns.size != 1) throw AssertionError()
                            CommandNode(returns[0].addsTo.asText() + returns[0].value)
                        }
                        else -> LiteralNode(nextNode.returns().map { it.value to it.addsTo })
                    }
                    if (nextNode::class != replacingNode::class) {
                        replaceNode(nextNode, replacingNode)
                        changed = true
                    }
                }
            }
        }
    }
    return changed
}

private fun replaceNode(nodeToReplace: BaseNode, replacingNode: BaseNode) {
    assert(nodeToReplace.outputs.size == 1)
    val nextNode = nodeToReplace.nextNode
    nodeToReplace.outEdgeTo[nextNode].destroy()
    replacingNode.nextNode = nextNode
    ArrayList(nodeToReplace.inputs).forEach {
        it.outputs.replace(nodeToReplace, replacingNode)
    }
}

private fun cutNode(node: BaseNode) {
    assert(node.outputs.size == 1)

    val nextNode = node.nextNode
    nextNode.inEdgeFrom[node].destroy()
    val inputs = ArrayList(node.inputs)
    inputs.forEach { prevNode ->
        prevNode.outputs.replace(node, nextNode)

        assert(!prevNode.outputs.contains(node))
        assert(prevNode.outputs.contains(nextNode))
    }
}

fun joinNextLiteralNodes(nodeBeforeLiteral: BaseNode): Boolean {
    if (nodeBeforeLiteral.outputs.size == 0) return false

    var changed = false
    nodeBeforeLiteral.outputs
            .forEach { node ->
        if (node.outputs.size > 0) {
            val nextNode = node.nextNode
            if (node is LiteralNode && node.outputs.size == 1
                    && nextNode is LiteralNode && node.inputs.size == 1) {

                val literalPairs = ArrayList<Pair<String, StackChangingNode.AddsTo>>()
                literalPairs.addAll(prototypesFromReturns(node.returns()))
                literalPairs.addAll(prototypesFromReturns(nextNode.returns()))

                val newNode = LiteralNode(literalPairs)
                val oldInputs = ArrayList(node.inputs)
                val oldNextNodeOutputs = ArrayList(nextNode.outputs.copy())
                replaceNode(nextNode, newNode)
                cutNode(node)

                assert(oldInputs.all {
                    !it.outputs.contains(node)
                            && !it.outputs.contains(nextNode)
                            && it.outputs.contains(newNode)
                })
                assert(oldNextNodeOutputs.all {
                    !it.inputs.contains(node)
                            && !it.inputs.contains(nextNode)
                            && it.inputs.contains(newNode)
                })

                changed = true
            }
        }
    }
    return changed
}

private fun prototypesFromReturns(returns: List<StackChangingNode.Return>): List<Pair<String, StackChangingNode.AddsTo>> {
    return returns.map {
        val value = if (it.consumed) {
            consumedMarker
        } else {
            it.value
        }
        (value to it.addsTo)
    }
}

fun optimizeWhile(
        node: BaseNode,
        visitor: (BaseNode) -> Boolean
): Boolean {
    var wasAtLeastOneChange = false

    for (i in 0 until 100) {
        val traversedNodes = HashSet<BaseNode>()
        val shouldContinue = recurse(node, traversedNodes, visitor)

        wasAtLeastOneChange = wasAtLeastOneChange || shouldContinue

        if (!shouldContinue) break
    }

    return wasAtLeastOneChange
}

fun recurse(
        node: BaseNode,
        traversedNodes: HashSet<BaseNode>,
        visitor: (BaseNode) -> Boolean
): Boolean {
    var visitorResult = visitor(node)

    traversedNodes.add(node)

    node.outputs.forEach {
        if (!traversedNodes.contains(it)) {
            visitorResult = recurse(it, traversedNodes, visitor) || visitorResult
        }
    }

    return visitorResult
}

fun convertNodeToText(nextNode: BaseNode): String {
    assert(nextNode !is LabelNode)
    assert(!nextNode.asText.contains("stack["))
    assert(nextNode.asText.trim() != ";")
    return nextNode.asText
}

fun convertToTextNodes(node: BaseNode): Boolean {
    var changed = false

    node.outputs.copy().forEach { nextNode ->
        if (nextNode.outputs.size == 1
                && nextNode !is LabelNode
                && nextNode !is TextNode
                && (nextNode !is LiteralNode || nextNode.returns().any { !it.consumed })
        ) {
            changed = true

            val newNode = TextNode(convertNodeToText(nextNode))
            val nodeAfterNext = nextNode.nextNode
            newNode.nextNode = nodeAfterNext
            ArrayList(nextNode.inputs).forEach {
                it.outputs.replace(nextNode, newNode)
            }
            nodeAfterNext.inEdgeFrom[nextNode].destroy()
        }
    }

    return changed
}

fun packPairsToTextNodes(node: BaseNode): Boolean {
    var changed = false

    node.outputs.copy().forEach { nextNode ->
        if (nextNode.outputs.size == 1 && nextNode !is LabelNode) {
            changed = true

            val nodeAfterNext = nextNode.nextNode
            if (nodeAfterNext !is LabelNode && nodeAfterNext.inputs.size == 1 && nodeAfterNext.outputs.size == 1
                    && (nextNode !is LiteralNode || nextNode.returns().any { !it.consumed })
                    && (nodeAfterNext !is LiteralNode || nodeAfterNext.returns().any { !it.consumed })
            ) {
                val newNode = TextNode("${convertNodeToText(nextNode)}${lineSeparator}${convertNodeToText(nodeAfterNext)}")
                newNode.nextNode = nodeAfterNext.nextNode
                ArrayList(nextNode.inputs).forEach {
                    it.outputs.replace(nextNode, newNode)
                }
                nodeAfterNext.nextNode.inEdgeFrom[nodeAfterNext].destroy()
            }
        }
    }

    return changed
}

fun removeUnusedLabel(nodeBefore: BaseNode): Boolean {
    var changed = false

    nodeBefore.outputs.copy().forEach { node ->
        if (node is LabelNode && node !is EndNode && node.inputs.size == 1) {
            changed = true

            cutNode(node)
        }
    }
    return changed
}

fun removeUnusedLiterals(nodeBefore: BaseNode): Boolean {
    var changed = false

    nodeBefore.outputs.copy().forEach { node ->
        if (node is LiteralNode && node.returns().all { it.consumed }) {
            changed = true

            cutNode(node)
        }
    }
    return changed
}

fun replaceGotoWithEdge(jumpingNode: JumpingNode) {
    val node = jumpingNode as BaseNode
    assert(node.outputs.size == 2)

    val jumpTargetNode = jumpingNode.jumpTargetNode
    val nextNode = node.nextNode
    nextNode.inEdgeFrom[node].destroy() // remove the artifact of reading word-by-word

    val inputs = ArrayList(node.inputs)
    inputs.forEach { prevNode ->
        prevNode.outputs.replace(node, jumpTargetNode)

        assert(!prevNode.outputs.contains(node))
        assert(prevNode.outputs.contains(jumpTargetNode))
    }
    node.outputs.replace(jumpTargetNode, nullNode)
    assert(!jumpTargetNode.inputs.contains(node))
    assert(!nextNode.inputs.contains(node))
}

private fun canBeBranchingEnd(node: BaseNode) = node is LabelNode || node is EndNode

fun joinNextSwitchNodes(node: BaseNode): Boolean {
    var changed = false

    node.outputs.forEach { nextNode ->
        if (!changed && nextNode is SwitchAndCaseNode) {
            var afterNextNode = nextNode.nextNode
            val caseNodes = ArrayList<CaseGotoNode>()
            caseNodes.add(nextNode)
            while (afterNextNode is CaseGotoNode) {
                if (afterNextNode.inputs.size != 1) throw AssertionError("SwitchCase node should have exactly one input node")
                caseNodes.add(afterNextNode)
                afterNextNode = afterNextNode.nextNode
            }
            if (afterNextNode is DropStackNode) {
                changed = true

                class ConditionsToTargetNodePair(
                        val conditions: List<String>,
                        val targetNode: BaseNode
                )
                val targetNodeToConditions = caseNodes
                        .groupBy { it.jumpTargetNode }
                        .entries.map {
                    val conditions = it.value.map {
                        (it.condition.argument as StackChangingNode.LiteralArgument).value
                    }
                    ConditionsToTargetNodePair(conditions, it.key)
                }
                val targets = listOf(afterNextNode.nextNode) + targetNodeToConditions.map { it.targetNode }
                val conditions = targetNodeToConditions.map { it.conditions }
                val switchNode = FullSwitchNode(nextNode.conditionTarget, conditions, targets)

                ArrayList(nextNode.inputs).forEach {
                    it.outputs.replace(nextNode, switchNode)
                }
                caseNodes.forEach {
                    it.outEdgeTo[it.jumpTargetNode].to = nullNode
                }
                afterNextNode.outEdgeTo[afterNextNode.nextNode].destroy()
            } else {
                throw IllegalStateException("Last byte of a switch block wasn't a drop stack op-code")
            }
        }
    }

    return changed
}

fun packSwitchBlockToText(node: BaseNode): Boolean {
    var changed = false

    node.outputs.copy().forEach { nextNode ->
        if (nextNode is FullSwitchNode) {
            if (!nextNode.outputs.all { it is TextNode || canBeBranchingEnd(it) }) return@forEach

            var endingLabel: BaseNode? = null
            for (it in nextNode.outputs.iterator()) {
                val discoveredEndingLabel: BaseNode = when {
                    canBeBranchingEnd(it) -> it
                    canBeBranchingEnd(it.nextNode) -> it.nextNode as LabelNode // For now only support cases with 'break's
                    else -> return@forEach
                }

                if (endingLabel == null) {
                    endingLabel = discoveredEndingLabel
                } else if (endingLabel != discoveredEndingLabel) {
                    return@forEach
                }
            }

            if (endingLabel == null) return@forEach

            changed = true

            val defaultCase = if (nextNode.nextNode != endingLabel) {
                listOf("default:" + lineSeparator + "\t" + nextNode.nextNode.asText)
            } else {
                emptyList()
            }
            val cases = nextNode.conditions
                    .mapIndexed { i, groupedConditions ->
                        val conditionLines = groupedConditions.joinToString(lineSeparator) { "case $it:" }
                        val body = nextNode.jumpTargets[i].asText.indent()
                        conditionLines + lineSeparator + body + lineSeparator + "\tbreak;"
                    }
                    .plus(defaultCase)
                    .joinToString(lineSeparator)
            val text = "switch (" + nextNode.conditionTarget + ") {${lineSeparator}" +
                    cases + lineSeparator +
                    "}"
            val newNode = TextNode(text)
            newNode.nextNode = endingLabel

            ArrayList(nextNode.inputs).forEach {
                it.outputs.replace(nextNode, newNode)
            }
            nextNode.outputs.copy().forEach { branch ->
                if (endingLabel.inputs.contains(branch)) {
                    endingLabel.inEdgeFrom[branch].destroy()
                }
            }
            if (endingLabel.inputs.contains(nextNode)) {
                endingLabel.inEdgeFrom[nextNode].destroy()
            }        }
    }

    return changed
}

@ExperimentalUnsignedTypes
class ParseState(
        val data: Data,
        val labelPositions: HashSet<Int>
)

private fun String.indent() =
        this.replace(Regex("^", MULTILINE), "\t")