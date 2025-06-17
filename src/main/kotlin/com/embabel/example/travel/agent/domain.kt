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
package com.embabel.example.travel.agent

import com.embabel.agent.domain.library.HasContent
import com.embabel.agent.domain.library.InternetResource
import com.embabel.agent.domain.library.InternetResources
import com.embabel.common.ai.prompt.PromptContributor
import com.embabel.example.travel.service.Person
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import java.time.LocalDate

sealed interface TravelBrief : PromptContributor {
    val brief: String
    val departureDate: LocalDate
    val returnDate: LocalDate
}

data class ExplorationTravelBrief(
    val areaToExplore: String,
    val stayingAt: String,
    override val brief: String,
    override val departureDate: LocalDate,
    override val returnDate: LocalDate,
) : TravelBrief {

    override fun contribution(): String =
        """
        Area to explore: $areaToExplore
        Staying at: $stayingAt
        Brief: $brief
        Dates: $departureDate to $returnDate
    """.trimIndent()
}

data class JourneyTravelBrief(
    val from: String,
    val to: String,
    val transportPreference: String,
    override val brief: String,
    override val departureDate: LocalDate,
    override val returnDate: LocalDate,
) : TravelBrief {

    override fun contribution(): String =
        """
        Journey from $from to $to
        Dates: $departureDate to $returnDate
        Brief: $brief
        Transport preference: $transportPreference
    """.trimIndent()
}

data class Travelers(
    val people: List<Person>,
) : PromptContributor {

    override fun contribution(): String =
        if (people.isEmpty()) "No information could be found about travelers"
        else "Travelers:\n" + people.joinToString(separator = "\n") {
            "${it.name}: activities:${it.activities.joinToString(", ") { act -> act.name ?: "anon" }}"
        }
}

data class PointOfInterest(
    val name: String,
    val description: String,
    val location: String,
)

data class ItineraryIdeas(
    val pointsOfInterest: List<PointOfInterest>,
)

data class ResearchedPointOfInterest(
    val pointOfInterest: PointOfInterest,
    val research: String,
    override val links: List<InternetResource>,
) : InternetResources

data class PointOfInterestFindings(
    val pointsOfInterest: List<ResearchedPointOfInterest>,
)

data class Day(
    val date: LocalDate,
    val stayingAt: String,
)

data class TravelPlan(
    @JsonPropertyDescription("Catchy title appropriate to the travelers and travel brief")
    val title: String,
    @JsonPropertyDescription("Detailed travel plan")
    val plan: String,
    @JsonPropertyDescription("List of days in the travel plan")
    val days: List<Day>,

    @JsonPropertyDescription("Links to maps or other resources related to the travel plan")
    val mapLinks: List<String>,
    @JsonPropertyDescription("Links to images")
    val imageLinks: List<InternetResource>,
    @JsonPropertyDescription("Links to pages with more information about the travel plan")
    val pageLinks: List<InternetResource>,
) : HasContent {

    override val content: String
        get() = """
            $title
            $plan
            Days: ${days.joinToString(separator = "\n") { "${it.date} - ${it.stayingAt}" }}
            Maps:
            ${if (mapLinks.isNotEmpty()) "Maps: ${mapLinks.joinToString(", ")}" else ""}
            Pages:
            ${pageLinks.joinToString("\n") { "${it.url} - ${it.summary}" }}
            Images:
            ${imageLinks.joinToString("\n") { "${it.url} - ${it.summary}" }}
        """.trimIndent()
}
