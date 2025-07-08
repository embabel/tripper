package com.embabel.agent.web.htmx

import com.embabel.agent.core.AgentProcess
import org.springframework.ui.Model

/**
 * Generic processing values to be used in the model for HTMX responses
 * across different apps.
 * This allows for consistent handling of agent processes and page details.
 */
data class GenericProcessingValues(
    val agentProcess: AgentProcess,
    val pageTitle: String,
    val detail: String,
    val resultModelKey: String,
    val successView: String,
) {

    fun addToModel(model: Model) {
        model.addAttribute("processId", agentProcess.id)
        model.addAttribute("pageTitle", pageTitle)
        model.addAttribute("detail", detail)
        model.addAttribute("resultModelKey", resultModelKey)
        model.addAttribute("successView", successView)
    }
}