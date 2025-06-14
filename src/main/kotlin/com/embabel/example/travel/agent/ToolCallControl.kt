package com.embabel.example.travel.agent

import com.embabel.common.ai.prompt.PromptContributor

/**
 * PromptContributor to control tool usage.
 */
data class ToolCallControl(
    val toolCalls: Int = 5,
) : PromptContributor {

    override fun contribution(): String =
        """
        You are allowed to make up to $toolCalls tool calls to complete the task.
        Use them wisely.
        If you reach this limit, you must stop and return your best answer.
        """.trimIndent()
}