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
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod

@ShellComponent("Travel planner commands")
internal class TravelPlannerShell(
    private val agentPlatform: AgentPlatform,
) {
    @ShellMethod
    fun planTravel() {
        val travelBrief = JourneyTravelBrief(
            from = "Nice",
            to = "Paris",
            dates = "June 1-5 2025 arriving in Paris on June 5",
            brief = """
                The travelers are interested in history, art, food, wine
                and classical music.
                They love walking and cycling.
                They are driving in a car that has no power.
                   They would like to take back roads and see nice countryside.
            """.trimIndent(),
        )

        val ap = agentPlatform.runAgentWithInput(
            agent = agentPlatform.agents().single { it.name == "TravelPlanner" },
            input = travelBrief,
            processOptions = ProcessOptions(
                verbosity = Verbosity(
                    showPrompts = true,
                    showLlmResponses = true,
                ),
            )
        )
        val travelPlan = ap.lastResult() as TravelPlan

        println("Travel Plan: ${markdownToConsole(travelPlan.plan)}")
    }
}
