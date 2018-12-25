package com.github.tarcv.zandronum.debotc

import com.github.tarcv.zandronum.debotc.LiteralNode.Companion.consumedMarker
import com.github.tarcv.zandronum.debotc.StackChangingNode.AddsTo.ADDS_TO_NORMAL_STACK
import org.junit.Assert
import org.junit.Test

class CompactTest {
    @Test
    fun compactFunctionArguments() {
        val rootNode = BeginNode()
        rootNode
                .attachNode(LiteralNode("local1", ADDS_TO_NORMAL_STACK))
                .attachNode(LiteralNode("1", ADDS_TO_NORMAL_STACK))
                .attachNode(FunctionNode("Function", StackChangingNode.consumesNormalStack(2), ADDS_TO_NORMAL_STACK))
                .attachNode(EndNode())


        optimizeWhileAsserted(rootNode) { currentNode ->
            var changed: Boolean = joinNextLiteralNodes(currentNode)
            changed = inlineStackArgs(currentNode) || changed
            changed
        }

        val expectedStructureRoot = BeginNode()
        expectedStructureRoot
                .attachNode(LiteralNode(listOf(
                        consumedMarker to ADDS_TO_NORMAL_STACK,
                        consumedMarker to ADDS_TO_NORMAL_STACK,
                        "Function(local1, 1)" to ADDS_TO_NORMAL_STACK
                )))
                .attachNode(EndNode())
        assertIsSameStructure(expectedStructureRoot, rootNode)
    }

    @Test
    fun testConvertToTextNodes() {
        val rootNode = BeginNode()
        rootNode
                .attachNode(CommandNode("Test command"))
                .attachNode(EndNode())

        optimizeWhileAsserted(rootNode) {
            node -> convertToTextNodes(node)
        }

        val expectedStructureRoot = BeginNode()
        expectedStructureRoot
                .attachNode(TextNode("Test command"))
                .attachNode(EndNode())
        assertIsSameStructure(expectedStructureRoot, rootNode)
    }

    @Test
    fun testConvertPairsToTextNodes() {
        val rootNode = BeginNode()
        rootNode
                .attachNode(CommandNode("TestCommand1"))
                .attachNode(CommandNode("TestCommand2"))
                .attachNode(EndNode())

        optimizeWhileAsserted(rootNode) {
            node -> packPairsToTextNodes(node)
        }

        val expectedStructureRoot = BeginNode()
        expectedStructureRoot
                .attachNode(TextNode("TestCommand1" + System.lineSeparator() + "TestCommand2"))
                .attachNode(EndNode())
        assertIsSameStructure(expectedStructureRoot, rootNode)
    }

    @Test
    fun testDeletingUnusedLabels() {
        val rootNode = BeginNode()
        rootNode
                .attachNode(CommandNode("TestCommand1"))
                .attachNode(LabelNode(777))
                .attachNode(CommandNode("TestCommand2"))
                .attachNode(EndNode())

        optimizeWhileAsserted(rootNode) {
            node -> removeUnusedLabel(node)
        }

        val expectedStructureRoot = BeginNode()
        expectedStructureRoot
                .attachNode(CommandNode("TestCommand1"))
                .attachNode(CommandNode("TestCommand2"))
                .attachNode(EndNode())
        assertIsSameStructure(expectedStructureRoot, rootNode)
    }

    @Test
    fun testFixingGotos() {
        val rootNode = BeginNode()
        val gotoNode = GotoNode(888)
        run {
            val gotoTarget = LabelNode(888)
            val splitNode = SplitNode(LabelNode(777))
            rootNode
                    .attachNode(splitNode)
                    .attachNode(gotoNode)
                    .attachNode(splitNode.lastNodeOfSecondBranch)
                    .attachNode(gotoTarget)
                    .attachNode(LabelNode(999))
                    .attachNode(EndNode())

            gotoNode.jumpTargetNode = gotoTarget
        }

        replaceGotoWithEdge(gotoNode)

        val expectedStructureRoot = BeginNode()
        run {
            val splitNode = SplitNode(LabelNode(777), LabelNode(888))
            expectedStructureRoot
                    .attachNode(splitNode)
                    .attachNode(splitNode.lastNodeOfSecondBranch)
                    .attachNode(LabelNode(999))
                    .attachNode(EndNode())
        }
        assertIsSameStructure(expectedStructureRoot, rootNode)
    }

    @Test
    fun testReplacingSwitch() {
        val rootNode = BeginNode()

        fun setUpTest() {
            val switchNode = SwitchAndCaseNode(
                    StackChangingNode.ArgumentHolder.createLiteralArgument("local1"),
                    "1",
                    111)

            val caseGotoNode2 = CaseGotoNode("2", 222)
            val caseGotoNode3 = CaseGotoNode("3", 333)

            val commandNode1 = CommandNode("Branch1")
            val commandNode2 = CommandNode("Branch2")
            val commandNode3 = CommandNode("Branch3")

            val endLabelNode = LabelNode(999)

            rootNode
                    .attachNode(LabelNode(101))
                    .attachNode(switchNode)
                    .attachNode(caseGotoNode2)
                    .attachNode(caseGotoNode3)
                    .attachNode(DropStackNode("default"))
                    .attachNode(endLabelNode)
                    .attachNode(EndNode())

            switchNode.jumpTargetNode = commandNode1
            caseGotoNode2.jumpTargetNode = commandNode2
            caseGotoNode3.jumpTargetNode = commandNode3

            commandNode1.attachNode(endLabelNode)
            commandNode2.attachNode(endLabelNode)
            commandNode3.attachNode(endLabelNode)
        }
        setUpTest()


        val expectedStructureRoot = setupExpectedFullSwitchNodeStructure()

        optimizeWhileAsserted(rootNode) {
            node -> joinNextSwitchNodes(node)
        }

        assertIsSameStructure(expectedStructureRoot, rootNode)
    }

    @Test
    fun testTextNodeSwitch() {
        val rootNode = setupExpectedFullSwitchNodeStructure()
        val expectedStructureRoot = setupExpectedTextNodeStructure()

        optimizeWhileAsserted(rootNode) { node ->
            var changed = false

            changed = packPairsToTextNodes(node) || changed

            changed = convertToTextNodes(node) || changed

            changed = packSwitchBlockToText(node) || changed

            changed
        }

        assertIsSameStructure(expectedStructureRoot, rootNode)
    }

    fun setupExpectedTextNodeStructure(): BeginNode {
        val expectedStructureRoot = BeginNode()
        expectedStructureRoot
                .attachNode(LabelNode(101))
                .attachNode(TextNode("switch (local1) {\n" +
                        "case 1:\n" +
                        "\tBranch1\n" +
                        "\tbreak;\n" +
                        "case 2:\n" +
                        "\tBranch2\n" +
                        "\tbreak;\n" +
                        "case 3:\n" +
                        "\tBranch3\n" +
                        "\tbreak;\n" +
                        "}"))
                .attachNode(LabelNode(999))
                .attachNode(EndNode())
        return expectedStructureRoot
    }
}

class SplitNode(vararg secondBranchNodes: BaseNode): BaseNode("Split", 2) {
    val lastNodeOfSecondBranch: BaseNode

    init {
        outputs[1] = secondBranchNodes[0]
        secondBranchNodes
                .drop(1)
                .fold(outputs[1]) { previousNode, itNode ->
                    previousNode.attachNode(itNode)
                }
        lastNodeOfSecondBranch = secondBranchNodes.last()
    }
}

private fun assertIsSameStructure(expectedStructureRoot: BeginNode, rootNode: BeginNode) {
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
    Assert.assertEquals(expectedNode.javaClass, actualNode.javaClass)
    Assert.assertEquals(expectedNode.asText, actualNode.asText)

    // Order of inputs is never important
    Assert.assertEquals("Inputs of ${nodeToDebugString(actualNode)}",
            listToText(expectedNode.inputs.sortedBy { nodeToDebugString(it) }),
            listToText(actualNode.inputs.sortedBy { nodeToDebugString(it) }))

    // But order of outputs is always important
    Assert.assertEquals("Outputs of ${nodeToDebugString(actualNode)}",
            listToText(expectedNode.outputs.copy()), listToText(actualNode.outputs.copy()))
}

fun listToText(expectedList: Collection<BaseNode>): String {
    return expectedList.size.toString() + " items: " + expectedList.joinToString { nodeToDebugString(it) }
}

private fun nodeToDebugString(it: BaseNode) =
        it.javaClass.simpleName + "[" + it.asText + "]"

private fun optimizeWhileAsserted(
        rootNode: BaseNode,
        visitor: (BaseNode) -> Boolean
): Boolean {
    var wasAtLeastOneChange = false

    assertInputsAndOutputsDiscoverable(rootNode)

    for (i in 0 until 100) {
        val traversedNodes = HashSet<BaseNode>()

        val shouldContinue = recurse(rootNode, traversedNodes, visitor)

//        assertInputsAndOutputsDiscoverable(rootNode)

        wasAtLeastOneChange = wasAtLeastOneChange || shouldContinue

        if (!shouldContinue) break
    }

    Assert.assertTrue(wasAtLeastOneChange)

    return wasAtLeastOneChange
}

private fun assertInputsAndOutputsDiscoverable(rootNode: BaseNode) {
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

fun recurseAsserted(
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

private fun BaseNode.attachNode(node: BaseNode): BaseNode {
    nextNode = node
    return node
}
