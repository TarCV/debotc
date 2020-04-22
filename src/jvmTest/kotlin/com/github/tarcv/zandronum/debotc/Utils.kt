package com.github.tarcv.zandronum.debotc

import kotlin.test.DefaultAsserter.assertEquals
import kotlin.test.DefaultAsserter.assertTrue

class SplitNode(vararg secondBranchNodes: BaseNode): BaseNode("Split", 2) {
    val nodeJoiningBothBranches: BaseNode

    init {
        outputs[1] = secondBranchNodes[0]
        secondBranchNodes
                .drop(1)
                .fold(outputs[1]) { previousNode, itNode ->
                    previousNode.attachNode(itNode)
                }
        nodeJoiningBothBranches = secondBranchNodes.last()
    }

    // No need to check if it actually has such deps, so better safe than sorry
    override val hasNonStackDeps: HasNonStackDeps = HasNonStackDeps.NON_STACK_DEPS
}

fun assertIsSameStructure(expectedStructureRoot: BeginNode, rootNode: BeginNode) {
    val traversedNodes = HashSet<BaseNode>()
    assertIsSameStructureInternal(expectedStructureRoot, rootNode, traversedNodes)
}

private fun assertIsSameStructureInternal(expectedNode: BaseNode, actualNode: BaseNode, traversedNodes: HashSet<BaseNode>) {
    if (!traversedNodes.add(expectedNode)) return

    assertEqualNodes(expectedNode, actualNode)
    expectedNode.inputs.zip(actualNode.inputs).forEach {
        assertEqualNodes(it.first, it.second)
    }
    expectedNode.outputs.copy().zip(actualNode.outputs.copy()).forEach {
        assertIsSameStructureInternal(it.first, it.second, traversedNodes)
    }
}

private fun assertEqualNodes(expectedNode: BaseNode, actualNode: BaseNode) {
    assertEquals("", expectedNode::class, actualNode::class)
    assertEquals("", expectedNode.asText, actualNode.asText)

    // Order of inputs is never important
    assertEquals("Inputs of ${nodeToDebugString(actualNode)}",
            listToText(expectedNode.inputs.sortedBy { nodeToDebugString(it) }),
            listToText(actualNode.inputs.sortedBy { nodeToDebugString(it) }))

    // But order of outputs is always important
    assertEquals("Outputs of ${nodeToDebugString(actualNode)}",
            listToText(expectedNode.outputs.copy()), listToText(actualNode.outputs.copy()))
}

fun listToText(expectedList: Collection<BaseNode>): String {
    return expectedList.size.toString() + " items: " + expectedList.joinToString { nodeToDebugString(it) }
}

private fun nodeToDebugString(it: BaseNode) =
        it::class.simpleName + "[" + it.asText + "]"

internal inline fun optimizeWhileAsserted(
        rootNode: BaseNode,
        noinline visitor: (BaseNode) -> Boolean
): Boolean {
    var wasAtLeastOneChange = false

    assertInputsAndOutputsDiscoverable(rootNode)

    for (i in 0 until 100) {
        val traversedNodes = HashSet<BaseNode>()

        val shouldContinue = recurse(rootNode, traversedNodes, visitor)

        assertInputsAndOutputsDiscoverable(rootNode)

        wasAtLeastOneChange = wasAtLeastOneChange || shouldContinue

        if (!shouldContinue) break
    }

    assertTrue("", wasAtLeastOneChange)

    return wasAtLeastOneChange
}

internal fun assertInputsAndOutputsDiscoverable(rootNode: BaseNode) {
    val discoverableNodes = HashSet<BaseNode>()
    recurse(rootNode, discoverableNodes) { false }

    val traversedNodesWhileAsserting = HashSet<BaseNode>()
    recurse(rootNode, traversedNodesWhileAsserting) { node ->
        assert(node.inputs.all { discoverableNodes.contains(it) }) {
            "Undiscoverable inputs: " + node.inputs.subtract(discoverableNodes).joinToString { nodeToDebugString(it) }
        }

        assert(node.outputs.all { discoverableNodes.contains(it) }) {
            "Undiscoverable outputs: " + node.inputs.subtract(discoverableNodes).joinToString { nodeToDebugString(it) }
        }

        false
    }
}

internal fun recurseAsserted(
        node: BaseNode,
        traversedNodes: HashSet<BaseNode>,
        visitor: (BaseNode) -> Boolean
): Boolean {
    var visitorResult = visitor(node)

    traversedNodes.add(node)

    node.outputs.forEach {
        if (!traversedNodes.contains(it)) {
            visitorResult = recurseAsserted(it, traversedNodes, visitor) || visitorResult
        }
    }

    return visitorResult
}

fun setupExpectedFullSwitchNodeStructure(): BeginNode {
    val commandNode1 = CommandNode("Branch1")
    val commandNode2 = CommandNode("Branch2")
    val commandNode3 = CommandNode("Branch3")

    val endLabelNode = LabelNode(999)
    commandNode1.attachNode(endLabelNode)
    commandNode2.attachNode(endLabelNode)
    commandNode3.attachNode(endLabelNode)

    val newSwitchNode = FullSwitchNode(
            StackChangingNode.ArgumentHolder.createLiteralArgument("local1"),
            listOf(
                    listOf("1"),
                    listOf("2"),
                    listOf("3")
            ),
            listOf(endLabelNode) + listOf(commandNode1, commandNode2, commandNode3)
    )

    val expectedStructureRoot = BeginNode()
    expectedStructureRoot
            .attachNode(LabelNode(101))
            .attachNode(newSwitchNode)

    endLabelNode
            .attachNode(EndNode())

    return expectedStructureRoot
}

fun BaseNode.attachNode(node: BaseNode): BaseNode {
    nextNode = node
    return node
}
