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
import com.embabel.tripper.agent.Traveler
import com.embabel.tripper.agent.Travelers
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
@RequestMapping(value = ["/", "/travel/journey"])
class JourneyHtmxController(
    private val agentPlatform: AgentPlatform,
) {

    private val logger = LoggerFactory.getLogger(JourneyHtmxController::class.java)

    data class JourneyPlanForm(
        val from: String = "Antwerp",
        val to: String = "Bordeaux",
        val transportPreference: String = "driving",
        val brief: String = """
            Relaxed road trip exploring countryside, history, food and wine.
        """.trimIndent(),
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        val departureDate: LocalDate = LocalDate.now(),
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        val returnDate: LocalDate = departureDate.plus(Period.ofDays(10)),
        val travelers: MutableList<TravelerForm> = mutableListOf(
            TravelerForm(name = "Ingrid", about = "Loves history and museums. Fascinated by Joan of Arc."),
            TravelerForm(name = "Claude", about = "Enjoys food and wine. Has a particular interest in cabernet.")
        ),
    )

    data class TravelerForm(
        val name: String = "",
        val about: String = ""
    )


    @GetMapping
    fun showPlanForm(model: Model): String {
        model.addAttribute("travelBrief", JourneyPlanForm())
        return "journey-form"
    }

    @PostMapping("/plan")
    fun planJourney(
        @ModelAttribute form: JourneyPlanForm,
        model: Model
    ): String {
        val travelBrief = JourneyTravelBrief(
            from = form.from,
            to = form.to,
            transportPreference = form.transportPreference,
            brief = form.brief,
            departureDate = form.departureDate,
            returnDate = form.returnDate,
        )

        // Convert form travelers to domain objects
        val travelersList = form.travelers.map { travelerForm ->
            Traveler(name = travelerForm.name, about = travelerForm.about)
        }
        val travelers = Travelers(travelers = travelersList)

        val agent = agentPlatform.agents().singleOrNull { it.name.lowercase().contains("trip") }
            ?: error("No travel agent found. Please ensure the tripper agent is registered.")

        val agentProcess = agentPlatform.createAgentProcessFrom(
            agent = agent,
            processOptions = ProcessOptions(
                verbosity = Verbosity(
                    showPrompts = true,
                    showLlmResponses = true,
                ),
            ),
            travelBrief, travelers
        )

        model.addAttribute("travelBrief", travelBrief)
        model.addAttribute("processId", agentProcess.id)
        agentPlatform.start(agentProcess)
        return "common/processing"
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
                model.addAttribute("title", "Planning Journey...")
                "common/processing" // Keep showing loading state
            }
        }
    }
}
