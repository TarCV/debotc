package com.github.tarcv.zandronum.debotc

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
                    .attachNode(FunctionNode("IsEnemyVisible", emptyList(), StackChangingNode.AddsTo.ADDS_TO_NORMAL_STACK))
                    .attachNode(FunctionNode("ClearEnemy", emptyList(), StackChangingNode.AddsTo.DONT_PUSHES_TO_STACK))
                    .attachNode(FunctionNode("Function", StackChangingNode.consumesNormalStack(1), StackChangingNode.AddsTo.ADDS_TO_NORMAL_STACK))
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
}