package com.github.tarcv.zandronum.debotc

import com.github.tarcv.zandronum.debotc.LiteralNode.Companion.consumedMarker
import com.github.tarcv.zandronum.debotc.StackChangingNode.AddsTo.ADDS_TO_NORMAL_STACK
import kotlin.collections.listOf
import kotlin.test.Test

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
                .attachNode(TextNode("Test command;"))
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
                .attachNode(TextNode("TestCommand1;" + lineSeparator + "TestCommand2;"))
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
    fun testDeletingUnusedLiterals() {
        val rootNode = run {
            val rootNode = BeginNode()
            val splitNode1 = SplitNode(
                    CommandNode("TestCommand2"),
                    LiteralNode(listOf(consumedMarker to ADDS_TO_NORMAL_STACK))
            )
            val splitNode2 = SplitNode(
                    CommandNode("TestCommand4"),
                    EndNode()
            )
            rootNode
                    .attachNode(splitNode1)
                    .attachNode(CommandNode("TestCommand1"))
                    .attachNode(splitNode1.nodeJoiningBothBranches)
                    .attachNode(splitNode2)
                    .attachNode(CommandNode("TestCommand3"))
                    .attachNode(splitNode2.nodeJoiningBothBranches)

            rootNode
        }
        optimizeWhileAsserted(rootNode) {
            node -> removeUnusedLiterals(node)
        }

        val expectedStructureRoot = run {
            val rootNode = BeginNode()
            val splitNode2 = SplitNode(
                    CommandNode("TestCommand4"),
                    EndNode()
            )
            val splitNode1 = SplitNode(
                    CommandNode("TestCommand2"),
                    splitNode2
            )


            rootNode
                    .attachNode(splitNode1)
                    .attachNode(CommandNode("TestCommand1"))
                    .attachNode(splitNode2)
                    .attachNode(CommandNode("TestCommand3"))
                    .attachNode(splitNode2.nodeJoiningBothBranches)

            rootNode
        }

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
                    .attachNode(splitNode.nodeJoiningBothBranches)
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
                    .attachNode(splitNode.nodeJoiningBothBranches)
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

    private fun setupExpectedTextNodeStructure(): BeginNode {
        val expectedStructureRoot = BeginNode()
        expectedStructureRoot
                .attachNode(LabelNode(101))
                .attachNode(TextNode("switch (local1) {\n" +
                        "case 1:\n" +
                        "\tBranch1;\n" +
                        "\tbreak;\n" +
                        "case 2:\n" +
                        "\tBranch2;\n" +
                        "\tbreak;\n" +
                        "case 3:\n" +
                        "\tBranch3;\n" +
                        "\tbreak;\n" +
                        "}"))
                .attachNode(LabelNode(999))
                .attachNode(EndNode())
        return expectedStructureRoot
    }
}
