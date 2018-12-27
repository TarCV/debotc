package com.github.tarcv.zandronum.debotc

import com.github.tarcv.zandronum.debotc.BotCommand.NUM_BOTCMDS
import com.github.tarcv.zandronum.debotc.DataHeaders.*
import com.github.tarcv.zandronum.debotc.LiteralNode.Companion.consumedMarker
import com.github.tarcv.zandronum.debotc.StackChangingNode.AddsTo.*
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout
import java.nio.ByteBuffer
import java.nio.ByteOrder.LITTLE_ENDIAN
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.ArrayList
import kotlin.collections.HashSet
import com.mxgraph.view.mxGraph
import com.mxgraph.swing.mxGraphComponent
import com.mxgraph.util.mxEvent
import java.util.Stack
import java.util.Arrays.asList
import javax.swing.JFrame
import kotlin.text.RegexOption.*


class Decompiler(
        private val optimizing: Boolean = true
) {
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

                val eventNodes = buildGraphForEvent(tempVarCounter, event)
                if (eventNodes.isNotEmpty()) {
                    val currentNode = eventNodes[0]
                    compactAndPrintNodes(currentNode, "State ${state.index} - Event ${event.readableType}",
                            true /*state.index == 1 && event.readableType == "mainloop" */)
                }

                println("\t}")
            }
            println("}")
        }
    }

    private fun compactAndPrintNodes(rootNode: BaseNode, title: String, drawGraph: Boolean): Boolean {
        lateinit var origGraph: JFrame
        lateinit var optimizedGraph: JFrame
        lateinit var textGraph: JFrame
        if (drawGraph) {
            origGraph = showGraph(rootNode, "$title - From bytes")
        }

        var wasAtLeastOneChange = false

        wasAtLeastOneChange = optimizeWhile(rootNode) { node ->
            recoverComplexNodes(node)
        } || wasAtLeastOneChange

        if (drawGraph && wasAtLeastOneChange) {
            optimizedGraph = showGraph(rootNode, "$title - Optimized", true)
        }

        wasAtLeastOneChange = optimizeWhile(rootNode) { node ->
            packToTextNodes(node)
        } || wasAtLeastOneChange

        if (drawGraph && wasAtLeastOneChange) {
            textGraph = showGraph(rootNode, "$title - Text", true)
        }

        assert(rootNode.outputs.size == 1)
        assert(rootNode.nextNode.inputs.size == 1)
        assert(rootNode.nextNode.outputs.size == 0 || rootNode.nextNode.outputs.size == 1)
        if (rootNode.nextNode.outputs.size == 1) {
            val code = (rootNode.nextNode as TextNode).asText.replace(Regex("^", MULTILINE), "\t\t")
            println(code)
            assert(rootNode.nextNode.nextNode is EndNode)
        } else {
            assert(rootNode.nextNode is EndNode)
        }

        if (drawGraph) {
            if (wasAtLeastOneChange) {
                textGraph.dispose()
                optimizedGraph.dispose()
            }
            origGraph.dispose()
        }

        return wasAtLeastOneChange
    }

    private fun packToTextNodes(node: BaseNode): Boolean {
        var changed = false

        changed = packPairsToTextNodes(node) || changed

        changed = convertToTextNodes(node) || changed

        changed = removeUnusedLabel(node) || changed

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
                    " else {${System.lineSeparator()}${elseBranch.asText.indent()}${System.lineSeparator()}}"
                } else ""
                val text = "if ($condition) {${System.lineSeparator()}" +
                        "${mainBranch.asText.indent()}${System.lineSeparator()}" +
                        "}$elseBranchText"
                val newNode = TextNode(text)

                newNode.nextNode = endingLabel
                ArrayList(nextNode.inputs).forEach {
                    it.outputs.replace(nextNode, newNode)
                }
                if (endingLabel.inputs.contains(nextNode)) {
                    nextNode.outputs.replace(endingLabel, BaseNode.nullNode)
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

    class CasePair(
            val condition: String,
            val targetNode: BaseNode
    )

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

    private fun showGraph(node: BaseNode, title: String, right: Boolean = false): JFrame {
        val graph = mxGraph()
        val parent = graph.defaultParent
        val frame = JFrame()

        val nodeToVertex = HashMap<BaseNode, Any>()

        graph.model.beginUpdate()
        val rootVertex: Any
        try {
            rootVertex = drawVertexes(graph, parent, nodeToVertex, node, 100.0, 0.0)
        } finally {
            graph.model.endUpdate()
        }

        mxHierarchicalLayout(graph).execute(parent, asList(rootVertex))

        val graphComponent = mxGraphComponent(graph)

        graphComponent.isConnectable = false
        graphComponent.addListener(mxEvent.START_EDITING) { sender, evt ->
            if (sender != null) {
                val cell = evt.properties["cell"]
                nodeToVertex.entries.find { it.value == cell }?.key?.run {
                    replaceGotoWithEdge(this as GotoNode)
                }
            }
        }
        frame.title = title
        frame.contentPane.add(graphComponent)
        frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
        frame.setSize(670, 800)
        if (right) {
            frame.setLocation(670, frame.y)
        }
        frame.isVisible = true

        return frame
    }

    private fun drawVertexes(graph: mxGraph, parent: Any, nodeToVertex: HashMap<BaseNode, Any>, node: BaseNode, x: Double, y: Double): Any {
        if (nodeToVertex.containsKey(node)) {
            return nodeToVertex[node]!!
        }

        var actualX = x + 50.0 * node.inputs.size
        if (node is LabelNode) actualX = 0.0

        val inputs = node.inputs.joinToString { it.javaClass.simpleName + "@" + System.identityHashCode(it) }
        val text = "${node.javaClass.simpleName}@${System.identityHashCode(node)}[ins: $inputs${System.lineSeparator()}, outs: ${node.outputs.size}]${System.lineSeparator()}${node.asText}"
        val height = if (node is TextNode) {
            20.0 * (1 + node.asText.count { it == '\n' })
        } else {
            20.0
        }
        val thisVertex = graph.insertVertex(parent, null, text, actualX, y, 40.0, height)
        nodeToVertex[node] = thisVertex

        var currentX = x
        node.outputs.forEach {
            val childVertex = drawVertexes(graph, parent, nodeToVertex, it, currentX, y + height + 10.0)
            graph.insertEdge(parent, null, "", thisVertex, childVertex)
            currentX += 50.0
        }

        return thisVertex
    }

    class NodeHolder(
            val nodes: ArrayList<BaseNode> = ArrayList(),
            var prevNode: BaseNode? = null
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

    private fun buildGraphForEvent(tempVarCounter: AtomicInteger, event: BaseEvent): ArrayList<BaseNode> {
        val vmState = VmState(
                strings
        )
        vmState.indent = 2

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
            it.jumpTargetNode = labels[targetByte] ?: throw IllegalStateException("Label$targetByte not found")

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

    private fun labelIfUsed(optimized: String, labelGroup: MatchGroup, indent: String): String {
        val gotoPos = labelGroup.range.start - 10
        val gotosToLabel = optimized.allIndexesOf(Regex("""(?<=\W)goto label${labelGroup.value}"""))
        val label = if (gotosToLabel.any { it != gotoPos }) {
            "label${labelGroup.value}:"
        } else {
            ""
        }
        return label
    }

    private fun String.allIndexesOf(regex: Regex): List<Int> {
        val positions = ArrayList<Int>()
        var from = 0
        while (true) {
            val matchResult = regex.find(this, from)
            if (matchResult != null) {
                positions.add(matchResult.range.start)
                from = matchResult.range.endInclusive + 1
            } else {
                break
            }
        }
        return positions
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
        eventEnds.add((data.offset - 4).toString())
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
        throw IllegalArgumentException("Illegal value $index for ${T::class.java.simpleName}")
    }
    return enumValues[index]
}

class VmState(
        val strings: List<String>
) {
    lateinit var previousCommand: DataHeaders
    var indent: Int = 0
    val output = StringBuilder()
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

// Doesn't throw ConcurrentModificationException but make sure not to change future iterations items
fun <T> List<T>.forEachReplacing(function: (T) -> Unit) {
    val oldSize = this.size
    for (i in 0 .. oldSize) {
        val item = this[i]
        function(item)

        assert(this.size == oldSize)
    }
}

val changingAddTos = StackChangingNode.AddsTo.values().filter { it != DONT_PUSHES_TO_STACK }
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
                                maxConsumedDepths[it.addsTo] = Integer.max(maxConsumedDepths[it.addsTo]!!, it.depth)
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
                    val replacingNode = when (returns.count { it.addsTo != DONT_PUSHES_TO_STACK }) {
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
        if (nextNode.outputs.size == 1 && nextNode !is LabelNode && nextNode !is TextNode) {
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
            if (nodeAfterNext !is LabelNode && nodeAfterNext.inputs.size == 1 && nodeAfterNext.outputs.size == 1) {
                val newNode = TextNode("${convertNodeToText(nextNode)}${System.lineSeparator()}${convertNodeToText(nodeAfterNext)}")
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
    node.outputs.replace(jumpTargetNode, BaseNode.nullNode)
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
                val targets = asList(afterNextNode.nextNode) + targetNodeToConditions.map { it.targetNode }
                val conditions = targetNodeToConditions.map { it.conditions }
                val switchNode = FullSwitchNode(nextNode.conditionTarget, conditions, targets)

                ArrayList(nextNode.inputs).forEach {
                    it.outputs.replace(nextNode, switchNode)
                }
                caseNodes.forEach {
                    it.outEdgeTo[it.jumpTargetNode].to = BaseNode.nullNode
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
                val discoveredEndingLabel: BaseNode = if (canBeBranchingEnd(it)) {
                    it
                } else if (canBeBranchingEnd(it.nextNode)) {
                    it.nextNode as LabelNode // For now only support cases with 'break's
                } else {
                    return@forEach
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
                listOf("default:" + System.lineSeparator() + "\t" + nextNode.nextNode.asText)
            } else {
                emptyList()
            }
            val cases = nextNode.conditions
                    .mapIndexed { i, groupedConditions ->
                        val conditionLines = groupedConditions.joinToString(System.lineSeparator()) { "case $it:" }
                        val body = nextNode.jumpTargets[i].asText.indent()
                        conditionLines + System.lineSeparator() + body + System.lineSeparator() + "\tbreak;"
                    }
                    .plus(defaultCase)
                    .joinToString(System.lineSeparator())
            val text = "switch (" + nextNode.conditionTarget + ") {${System.lineSeparator()}" +
                    cases + System.lineSeparator() +
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

private fun String.indent() =
        replace(Regex("^", MULTILINE), "\t")