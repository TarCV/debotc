package com.github.tarcv.zandronum.debotc

import com.github.tarcv.zandronum.debotc.BaseNode.HasNonStackDeps.NON_STACK_DEPS
import com.github.tarcv.zandronum.debotc.Decompiler.Companion.compactNodes
import com.github.tarcv.zandronum.debotc.StackChangingNode.AddsTo
import kotlin.test.Test

/**
 * Counterexamples for inlining, i.e. some temporaries should not be inlines in these examples
 */
class CounterexamplesTest {
    @Test
    fun testNoInlinesAcrossStatements() {
        fun setupNodes(): BeginNode {
            val rootNode = BeginNode()
            rootNode
                    .attachNode(FunctionNode("IsEnemyVisible", emptyList(), AddsTo.ADDS_TO_NORMAL_STACK))
                    .attachNode(FunctionNode("ClearEnemy", emptyList(), AddsTo.DONT_PUSHES_TO_STACK))
                    .attachNode(FunctionNode("Function", StackChangingNode.consumesNormalStack(1), AddsTo.ADDS_TO_NORMAL_STACK))
                    .attachNode(EndNode())
            return rootNode
        }

        val rootNode = setupNodes()

        optimizeWhile(rootNode) { currentNode ->
            var changed: Boolean = joinNextLiteralNodes(currentNode)
            changed = inlineStackArgs(currentNode) || changed
            changed
        }

        val expectedStructureRoot = setupNodes()
        assertIsSameStructure(expectedStructureRoot, rootNode)
    }

    @Test
    fun testNoInlinesAcrossStatements2() {
        val rootNode = BeginNode()
        rootNode
                .attachNode(FunctionNode("Roam", emptyList(), AddsTo.ADDS_TO_NORMAL_STACK, NON_STACK_DEPS))
                .attachNode(DropStackNode())
                .attachNode(FunctionNode("Function", emptyList(), AddsTo.DONT_PUSHES_TO_STACK))
                .attachNode(EndNode())
        compactNodes(rootNode)

        val expectedStructureRoot = BeginNode()
        expectedStructureRoot
                .attachNode(TextNode("Roam();${lineSeparator}// dropped 'Roam()';${lineSeparator}Function();"))
                .attachNode(EndNode())

        assertIsSameStructure(expectedStructureRoot, rootNode)
    }
}