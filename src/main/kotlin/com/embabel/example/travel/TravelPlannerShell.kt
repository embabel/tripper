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
import com.embabel.agent.shell.markdownToConsole
import com.embabel.example.travel.agent.JourneyTravelBrief
import com.embabel.example.travel.agent.MarkdownTravelPlan
import com.embabel.example.travel.service.PersonService
import org.apache.commons.text.WordUtils
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import java.time.LocalDate

@ShellComponent("Travel planner commands")
internal class TravelPlannerShell(
    private val personService: PersonService,
    private val agentPlatform: AgentPlatform,
) {
    @ShellMethod
    fun findPeople(
    ): String {
        return personService.loadPeople().toString()
    }

    @ShellMethod
    fun planTravel() {
        val travelBrief = JourneyTravelBrief(
            from = "Antwerp",
            to = "Bordeaux",
            startDate = LocalDate.of(2025, 10, 10),
            endDate = LocalDate.of(2025, 11, 5),
            transportPreference = "car",
            brief = """
                Rod and Lynda would like to take back roads and see nice countryside.
            """.trimIndent(),
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
        val travelPlan = ap.lastResult() as MarkdownTravelPlan

        println("Travel Plan: ${WordUtils.wrap(markdownToConsole(travelPlan.plan), 100)}")

        println(travelPlan)
    }
}
