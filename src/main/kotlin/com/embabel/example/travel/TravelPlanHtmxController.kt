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
package com.embabel.example.travel

import com.embabel.agent.core.AgentPlatform
import com.embabel.agent.core.AgentProcessStatusCode
import com.embabel.agent.core.IoBinding
import com.embabel.agent.core.ProcessOptions
import com.embabel.agent.core.Verbosity
import com.embabel.example.travel.agent.JourneyTravelBrief
import com.embabel.example.travel.agent.TravelPlan
import jakarta.servlet.http.HttpServletResponse
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate

@Controller
@RequestMapping(value = ["/travel/journey"])
class TravelPlanHtmxController(
    private val agentPlatform: AgentPlatform,
    private val asyncWrapper: AsyncWrapper,
) {

    @GetMapping
    fun showForm(
        @RequestParam(defaultValue = "Antwerp") from: String,
        @RequestParam(defaultValue = "Bordeaux") to: String,
        @RequestParam(defaultValue = "driving") transport: String,
        @RequestParam(defaultValue = "Relaxed journey with historical sightseeing for Rod and Lynda") brief: String,
        @RequestParam(required = false) departureDate: String?,
        @RequestParam(required = false) returnDate: String?,
        model: Model
    ): String {
        val defaultDepartureDate = departureDate?.let {
            LocalDate.parse(it)
        } ?: LocalDate.now()

        val defaultReturnDate = returnDate?.let {
            LocalDate.parse(it)
        } ?: LocalDate.now()

        model.addAttribute(
            "travelBrief",
            JourneyTravelBrief(
                from = from,
                to = to,
                transportPreference = transport,
                brief = brief,
                departureDate = defaultDepartureDate,
                returnDate = defaultReturnDate,
            )
        )
        return "travel-form"
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
        val agent = agentPlatform.agents().singleOrNull { it.name.lowercase().contains("travel") }
            ?: error("No travel agent found. Please ensure the travel agent is registered.")
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
        asyncWrapper.async { agentProcess.run() }

        return "travel-plan-loading"
    }

    @GetMapping("/status/{processId}")
    fun checkPlanStatus(@PathVariable processId: String, model: Model): String {
        val agentProcess = agentPlatform.getAgentProcess(processId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Process not found")

        return if (agentProcess.status == AgentProcessStatusCode.COMPLETED) {
            val travelPlan = agentProcess.lastResult() as TravelPlan
            model.addAttribute("travelPlan", travelPlan)
            "travel-plan-htmx-result"
        } else {
            model.addAttribute("processId", processId)
            "travel-plan-loading" // Keep showing loading state
        }
    }
}
