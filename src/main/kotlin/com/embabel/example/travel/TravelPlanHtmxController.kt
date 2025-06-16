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
import com.embabel.agent.core.ProcessOptions
import com.embabel.agent.core.Verbosity
import com.embabel.example.travel.agent.JourneyTravelBrief
import com.embabel.example.travel.agent.TravelPlan
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import java.time.LocalDate

@Controller
class TravelPlanHtmxController(
    private val agentPlatform: AgentPlatform,
) {

    @GetMapping
    fun showForm(model: Model): String {
        model.addAttribute("travelBrief", JourneyTravelBrief("", "", "", "", LocalDate.now(), LocalDate.now()))
        return "travel-form"
    }

    @PostMapping("/plan")
    fun planJourney(
        @RequestParam from: String,
        @RequestParam to: String,
        @RequestParam transportPreference: String,
        @RequestParam brief: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
        model: Model
    ): String {
        val travelBrief = JourneyTravelBrief(
            from = from,
            to = to,
            transportPreference = transportPreference,
            brief = brief,
            startDate = startDate,
            endDate = endDate,
        )
        val ap = agentPlatform.runAgentWithInput(
            agent = agentPlatform.agents().singleOrNull { it.name.lowercase().contains("travel") }
                ?: error("No travel agent found. Please ensure the travel agent is registered."),
            input = travelBrief,
            processOptions = ProcessOptions(
                verbosity = Verbosity(
                    showPrompts = true,
                    showLlmResponses = true,
                ),
            )
        )
        val travelPlan = ap.lastResult() as TravelPlan
        model.addAttribute("travelPlan", travelPlan)
        return "travel-plan-htmx-result"
    }
}
