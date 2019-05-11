package com.github.tarcv.zandronum.debotc

import com.github.tarcv.zandronum.debotc.StackChangingNode.*
import com.github.tarcv.zandronum.debotc.StackChangingNode.AddsTo.*
import com.github.tarcv.zandronum.debotc.StackChangingNode.ArgumentHolder.Companion.createLiteralArgument
import com.github.tarcv.zandronum.debotc.StackChangingNode.ArgumentHolder.Companion.createNormalStackArgument
import kotlin.IllegalArgumentException
import kotlin.collections.ArrayList

abstract class BaseNode(open val asText: String, outputNum: Int) {
    private val _inputs: ArrayList<BaseNode> = ArrayList()
    val inputs : List<BaseNode>
        get() = _inputs.toList()

    val outputs = OutputsHolder(outputNum)

    var nextNode: BaseNode
        get() = outputs[0]
        set(value) {
            outputs[0] = value
        }

    val inEdgeFrom = EdgesFrom()
    val outEdgeTo = EdgesTo()

    fun addInput(node: BaseNode) {
        assert(!_inputs.contains(node) || this == nullNode || node == nullNode)
        _inputs.add(node)
    }

    class Edge(
            from: BaseNode,
            to: BaseNode
    ) {

        fun destroy() {
            verifyStillValid()

            if (from.nextNode != to) throw IllegalArgumentException("Only nextNode edges can be destroyed")
            from.nextNode = nullNode
            to._inputs.remove(from)

            assert(!from.outputs.contains(to))
            assert(!to._inputs.contains(from))
        }

        private fun verifyStillValid() {
            if (!from.outputs.contains(to)
            || !to._inputs.contains(from)) {
                throw AssertionError()
            }
        }

        private var from = from
            private set

        var to = to
            set(value) {
                verifyStillValid()

                assert(from.outputs.contains(field))
                from.outputs.replace(field, value)

                assert(!to._inputs.contains(from))
                assert(value == nullNode || value._inputs.count { it == from } == 1)
                assert(!from.outputs.contains(field))
                assert(from.outputs.contains(value))

                field = value

                verifyStillValid()
            }
    }

    inner class EdgesTo {
        operator fun get(to: BaseNode): Edge {
            if (!outputs.contains(to)) {
                throw IllegalArgumentException("No such edge")
            }
            return Edge(this@BaseNode, to)
        }
    }

    inner class EdgesFrom {
        operator fun get(from: BaseNode): Edge {
            if (!_inputs.contains(from)) {
                throw IllegalArgumentException("No such edge")
            }
            return Edge(from, this@BaseNode)
        }
    }

    inner class OutputsHolder(num: Int) {
        private val holder = Array<BaseNode>(num) { _ -> nullNode }

        val size: Int = holder.size
        operator fun get(index: Int): BaseNode = holder[index]

        operator fun set(index: Int, value: BaseNode) {
            if (this[index] != nullNode) {
                if (index == 0) {
                    replace(this[index], value)
                } else {
                    throw AssertionError("After output is set, only replace can be used")
                }
            }
            privateSet(index, value)
        }

        fun replace(oldValue: BaseNode, newValue: BaseNode) {
            if (holder.count { it == oldValue } != 1 && oldValue !is LabelNode) {
                throw IllegalArgumentException()
            }

            privateSet(holder.indexOf(oldValue), newValue)
            oldValue._inputs.remove(this@BaseNode)
        }

        private fun privateSet(index: Int, value: BaseNode) {
            assert(!holder.contains(value) || value == nullNode)
            value.addInput(this@BaseNode)
            holder[index] = value
        }

        fun contains(node: BaseNode): Boolean {
            val found = holder.contains(node)
            if (found) {
                assert(holder.count { it == node } == 1)
            }
            return found
        }
        fun forEach(function: (BaseNode) -> Unit) = holder.forEach(function)

        fun all(predicate: (BaseNode) -> Boolean): Boolean = holder.all(predicate)

        fun copy(): List<BaseNode> {
            return holder.asList()
        }

        fun iterator(): Iterator<BaseNode> = holder.iterator()
    }
}

// Hack to avoid immutability in Kotlin Native
class NullNode: BaseNode("NULL", 0) { }
val nullNode = NullNode()

class BeginNode: BaseNode("(BEGIN)", 1)
class EndNode: BaseNode("(END)", 0)

open class CommandNode(asText: String): BaseNode(asText.tryAppendSemicolon(), 1)

class TextNode(asText: String): BaseNode(asText, 1)

interface ConsumesStack {
    val arguments: List<ArgumentHolder>
}

abstract class StackChangingNode(
        override val arguments: List<ArgumentHolder>,
        private val returnPrototypes: Array<ReturnPrototype>,
        outputNum: Int = 1
)
    : BaseNode("", outputNum), ConsumesStack {
    enum class AddsTo {
        DONT_PUSHES_TO_STACK {
            override fun asText(): String = ""
        },
        ADDS_TO_NORMAL_STACK {
            override fun asText(): String = "stack = "
        },
        ADDS_TO_STRING_STACK {
            override fun asText(): String = "stringStack = "
        };

        abstract fun asText(): String
    }

    private val consumedReturns = HashSet<Int>()
    private fun isReturnConsumed(index: Int) = consumedReturns.contains(index)
    fun markReturnAsConsumed(index: Int) {
        returns()[index] // make sure it exists
        assert(!isReturnConsumed(index))
        consumedReturns.add(index)
    }

    init {
        returnPrototypes.forEachIndexed { index, returnPrototype ->
            if (returnPrototype.consumed) {
                markReturnAsConsumed(index)
            }
        }
    }

    fun returns(): List<Return> {
        return returnPrototypes.mapIndexed { i, it ->
            Return(i, it.addsTo, it.returnTransform(arguments))
        }
    }

    class ReturnPrototype (
        val addsTo: AddsTo,
        val returnTransform: (arguments: List<ArgumentHolder>) -> String,
        val consumed: Boolean = false
    )
    inner class Return constructor(
            val index: Int,
            val addsTo: AddsTo,
            val value: String
    ) {
        val consumed: Boolean
            get() = isReturnConsumed(index)
    }

    override val asText: String
        get() = returnsAsText(this)

    abstract class Argument

    class ArgumentHolder(
            var argument: Argument
    ) {
        override fun toString() = argument.toString()

        companion object {
            fun createNormalStackArgument(depth: Int): ArgumentHolder {
                return ArgumentHolder(NormalStackArgument(depth))
            }

            fun createStringStackArgument(depth: Int): ArgumentHolder {
                return ArgumentHolder(StringStackArgument(depth))
            }

            fun createLiteralArgument(value: String): ArgumentHolder {
                return ArgumentHolder(LiteralArgument(value))
            }
        }
    }

    interface StackArgument {
        val addsTo: AddsTo
        val depth: Int
    }

    class NormalStackArgument(override val depth: Int): Argument(), StackArgument {
        init {
            assert(depth >= 0)
        }

        override val addsTo: AddsTo = ADDS_TO_NORMAL_STACK

        override fun toString(): String = "stack[$depth]"
    }
    class StringStackArgument(override val depth: Int): Argument(), StackArgument {
        init {
            assert(depth >= 0)
        }

        override val addsTo: AddsTo = ADDS_TO_STRING_STACK

        override fun toString(): String = "stringStack[$depth]"
    }
    class LiteralArgument(val value: String): Argument() {
        override fun toString(): String = value
    }

    companion object {
        fun consumesNormalStack(numArgs: Int): ArrayList<ArgumentHolder> {
            val args = ArrayList<ArgumentHolder>()
            for (i in (numArgs - 1) downTo  0) {
                args.add(createNormalStackArgument(i))
            }
            return args
        }
    }
}

class CustomStackConsumingNode(
        numArgs: Int,
        addsTo: AddsTo,
        private val transformer: (List<ArgumentHolder>) -> String
)
: StackChangingNode(consumesNormalStack(numArgs), arrayOf(ReturnPrototype(addsTo, { arguments -> transformer(arguments) })))

open class FunctionNode(val name: String, arguments: List<ArgumentHolder>, addsTo: AddsTo)
    : StackChangingNode(
        arguments,
        arrayOf(ReturnPrototype(addsTo, { _ -> "$name(${arguments.joinToString { it.toString() }})" }))
)

class OperatorNode(val name: String, numArgs: Int)
    : StackChangingNode(
        consumesNormalStack(numArgs),
        arrayOf(when (numArgs) {
            2 -> ReturnPrototype(ADDS_TO_NORMAL_STACK, { arguments -> "(" + arguments[0] + " " + name + " " + arguments[1] + ")" })
            1 -> ReturnPrototype(ADDS_TO_NORMAL_STACK, { arguments -> "(" + name + arguments[0] + ")" })
            else -> throw AssertionError()
        })
)

class LiteralNode(pairs: List<Pair<String, AddsTo>>)
    : StackChangingNode(
        emptyList(),
        pairs.map {ReturnPrototype(it.second, { _ -> it.first }, it.first == consumedMarker) }.toTypedArray()
) {
    constructor(value: String, addsTo: AddsTo) : this(listOf(value to addsTo))

    companion object {
        const val consumedMarker = "!%**CONSUMED**"
    }
}

class DropStackNode(override val asText: String)
    : StackChangingNode(
        listOf(createNormalStackArgument(0)),
        arrayOf(ReturnPrototype(DONT_PUSHES_TO_STACK, { arguments -> "// dropped '${arguments[0]}'" }))
)

open class LabelNode(byte: Int) : BaseNode("label$byte", 1)

interface JumpingNode {
    var jumpTargetNode: BaseNode
}

open class AbstractGotoNode(val targetByte: Int) : BaseNode("goto label$targetByte", 2), JumpingNode {
    override var jumpTargetNode: BaseNode
        get() = outputs[1]
        set(value) {
            outputs[1] = value
        }
}

class TerminatingFunctionNode(name: String, arguments: List<ArgumentHolder>, addsTo: AddsTo)
    : FunctionNode(name, arguments, addsTo), JumpingNode {
    override var jumpTargetNode: BaseNode
        get() = outputs[1]
        set(value) {
            outputs[1] = value
        }
}

class GotoNode(targetByte: Int): AbstractGotoNode(targetByte)

open class IfGotoNode(val condition: ArgumentHolder, targetByte: Int)
    : AbstractGotoNode(targetByte), ConsumesStack
{
    override val arguments: List<ArgumentHolder>
        get() = listOf(condition)

    override val asText: String
        get() = "if (${condition.argument}) ${super.asText}"
}

class IfNotGotoNode(val condition: ArgumentHolder, targetByte: Int)
    : AbstractGotoNode(targetByte), ConsumesStack {
    override val arguments: List<ArgumentHolder>
        get() = listOf(condition)

    override val asText: String
        get() = "if (! $condition) ${super.asText}"
}

open class CaseGotoNode(condition: String, targetByte: Int): IfGotoNode(createLiteralArgument(condition), targetByte) {
    override val asText: String
        get() = "case ($condition) ${super.asText}"
}

class SwitchAndCaseNode(val conditionTarget: ArgumentHolder, condition: String, targetByte: Int)
    : CaseGotoNode(condition, targetByte), ConsumesStack {

    override val arguments: List<ArgumentHolder>
        get() = listOf(conditionTarget)

    override val asText: String
        get() = "switch($conditionTarget):${lineSeparator} case ($condition) ${super.asText}"
}

class FullSwitchNode(
        val conditionTarget: ArgumentHolder,
        val conditions: List<List<String>>,
        allJumpTargets: Collection<BaseNode>
)
    : BaseNode("", allJumpTargets.size), ConsumesStack
{
    init {
        allJumpTargets.forEachIndexed { i, target ->
            outputs[i] = target
        }
    }

    override val arguments: List<ArgumentHolder>
        get() = listOf(conditionTarget)

    override val asText: String
        get() {
            val conditionsAsText = conditions.joinToString(";   ") {
                it.joinToString(", ")
            }
            return "switch($conditionTarget):${lineSeparator} cases: $conditionsAsText"
        }

    val jumpTargets: List<BaseNode>
        get() = outputs.copy().subList(1, outputs.size)
}

fun returnsAsText(node: StackChangingNode): String {
    return node.returns()
            .filter { !it.consumed }
            .joinToString(lineSeparator, postfix = ";") {
                it.addsTo.asText() + it.value
            }
            .tryAppendSemicolon()
}

fun String.tryAppendSemicolon(): String {
    return if (!endsWith(";")) {
        "$this;"
    } else {
        this
    }
}