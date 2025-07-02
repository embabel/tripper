/*
 * Copyright 2024-2025 Embabel Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.embabel.tripper.web

import com.embabel.agent.core.*
import com.embabel.tripper.agent.JourneyTravelBrief
import com.embabel.tripper.agent.TravelPlan
import org.slf4j.LoggerFactory
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.time.Period

@Controller
@RequestMapping
class HomeController(
) {

    @GetMapping
    fun home(): String {
        return "home"
    }
}

@Controller
@RequestMapping(value = ["/travel/journey"])
class JourneyHtmxController(
    private val agentPlatform: AgentPlatform,
) {

    private val logger = LoggerFactory.getLogger(JourneyHtmxController::class.java)

    @GetMapping
    fun showForm(
        @RequestParam(defaultValue = "Antwerp") from: String,
        @RequestParam(defaultValue = "Bordeaux") to: String,
        @RequestParam(defaultValue = "driving") transport: String,
        @RequestParam(defaultValue = "") brief: String,
        @RequestParam(required = false) departureDate: String?,
        @RequestParam(required = false) returnDate: String?,
        model: Model
    ): String {
        val defaultBrief = """
            Relaxed journey with historical sightseeing for Rod and Lynda 
            who love food, wine, cycling and history
        """.trimIndent()
        val defaultDepartureDate = departureDate?.let {
            LocalDate.parse(it)
        } ?: LocalDate.now()

        val defaultReturnDate = returnDate?.let {
            LocalDate.parse(it)
        } ?: LocalDate.now().plus(Period.ofDays(7))

        model.addAttribute(
            "travelBrief",
            JourneyTravelBrief(
                from = from,
                to = to,
                transportPreference = transport,
                brief = brief.ifBlank { defaultBrief },
                departureDate = defaultDepartureDate,
                returnDate = defaultReturnDate,
            )
        )
        return "journey-form"
    }

    @PostMapping("/plan")
    fun planJourney(
        @RequestParam from: String,
        @RequestParam to: String,
        @RequestParam transportPreference: String,
        @RequestParam brief: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) departureDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) returnDate: LocalDate,
        model: Model
    ): String {
        val travelBrief = JourneyTravelBrief(
            from = from,
            to = to,
            transportPreference = transportPreference,
            brief = brief,
            departureDate = departureDate,
            returnDate = returnDate,
        )
        val agent = agentPlatform.agents().singleOrNull { it.name.lowercase().contains("trip") }
            ?: error("No travel agent found. Please ensure the tripper agent is registered.")
        val agentProcess = agentPlatform.createAgentProcess(
            agent = agent,
            processOptions = ProcessOptions(
                verbosity = Verbosity(
                    showPrompts = true,
                    showLlmResponses = true,
                ),
            ),
            bindings = mapOf(
                IoBinding.DEFAULT_BINDING to travelBrief,
            )
        )
        model.addAttribute("processId", agentProcess.id)
        agentPlatform.start(agentProcess)
        return "planning"
    }

    /**
     * The HTML page that shows the status of the plan generation.
     */
    @GetMapping("/status/{processId}")
    fun checkPlanStatus(@PathVariable processId: String, model: Model): String {
        val agentProcess = agentPlatform.getAgentProcess(processId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Process not found")

        return when (agentProcess.status) {
            AgentProcessStatusCode.COMPLETED -> {
                logger.info("Process {} completed successfully", processId)
                val travelPlan = agentProcess.resultOfType<TravelPlan>()
                model.addAttribute("travelPlan", travelPlan)
                model.addAttribute("agentProcess", agentProcess)
                "journey-plan"
            }

            AgentProcessStatusCode.FAILED -> {
                logger.error("Process {} failed: {}", processId, agentProcess.failureInfo)
                model.addAttribute("error", "Failed to generate travel plan: ${agentProcess.failureInfo}")
                "error-making-travel-plan"
            }

            AgentProcessStatusCode.TERMINATED -> {
                logger.info("Process {} was terminated", processId)
                model.addAttribute("error", "Process was terminated before completion")
                "error-making-travel-plan"
            }

            else -> {
                model.addAttribute("processId", processId)
                "making-plan" // Keep showing loading state
            }
        }
    }
}
