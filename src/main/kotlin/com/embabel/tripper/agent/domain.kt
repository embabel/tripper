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
package com.embabel.tripper.agent

import com.embabel.agent.domain.library.HasContent
import com.embabel.agent.domain.library.InternetResource
import com.embabel.agent.domain.library.InternetResources
import com.embabel.common.ai.prompt.PromptContributor
import com.embabel.tripper.service.Person
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import java.net.URLEncoder
import java.time.LocalDate

@JsonDeserialize(`as` = JourneyTravelBrief::class)
sealed interface TravelBrief : PromptContributor {
    val brief: String
    val departureDate: LocalDate
    val returnDate: LocalDate
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
            "${it.name}: activities:${it.activities.joinToString(", ") { act -> act.name }}"
        }
}

data class PointOfInterest(
    val name: String,
    val description: String,
    val location: String,
    val fromDate: LocalDate,
    val toDate: LocalDate,
)

data class ItineraryIdeas(
    val pointsOfInterest: List<PointOfInterest>,
)

data class ResearchedPointOfInterest(
    val pointOfInterest: PointOfInterest,
    val research: String,
    override val links: List<InternetResource>,
    @JsonPropertyDescription("Links to images. Links must be the images themselves, not just links to them.")
    val imageLinks: List<InternetResource>,
) : InternetResources

data class PointOfInterestFindings(
    val pointsOfInterest: List<ResearchedPointOfInterest>,
)

data class Day(
    val date: LocalDate,
    @JsonPropertyDescription("Location where the traveler will stay on this day in Google Maps friendly format 'City,+Country'")
    val locationAndCountry: String,
) {
    /**
     * More readable location name, e.g. "Paris" rather than "Paris,+FR".
     */
    val stayingAt: String = locationAndCountry.split(",").firstOrNull()?.trim() ?: "Unknown location"
}

data class ProposedTravelPlan(
    @JsonPropertyDescription("Catchy title appropriate to the travelers and travel brief")
    val title: String,
    @JsonPropertyDescription("Detailed travel plan")
    val plan: String,
    @JsonPropertyDescription("List of days in the travel plan")
    val days: List<Day>,
    @JsonPropertyDescription("Links to images")
    val imageLinks: List<InternetResource>,
    @JsonPropertyDescription("Links to pages with more information about the travel plan")
    val pageLinks: List<InternetResource>,
)

data class Stay(
    val days: List<Day>,
    val airbnbUrl: String? = null,
) {

    fun stayingAt(): String {
        return days.firstOrNull()?.stayingAt ?: "Unknown location"
    }

    fun locationAndCountry(): String {
        return days.firstOrNull()?.locationAndCountry ?: "Unknown location"
    }
}

data class TravelPlan(
    val brief: JourneyTravelBrief,
    val plan: ProposedTravelPlan,
    val stays: List<Stay>,
) : HasContent {

    /**
     * Google Maps link for the whole journey. Computed from days.
     * Even good LLMs seem to get map links wrong, so we compute it here.
     */
    val journeyMapUrl: String
        get() {
            val encodedLocations = plan.days.distinctBy { it.locationAndCountry }.map { day ->
                URLEncoder.encode(day.locationAndCountry, Charsets.UTF_8.name())
            }

            return if (encodedLocations.size == 1) {
                "https://www.google.com/maps/search/?api=1&query=${encodedLocations.first()}"
            } else {
                "https://www.google.com/maps/dir/${encodedLocations.joinToString("/")}"
            }
        }

    fun journeyMapImageUrl(
        width: Int = 600,
        height: Int = 400,
    ): String {
        val encodedLocations = plan.days.distinctBy { it.stayingAt }.map { day ->
            URLEncoder.encode(day.stayingAt, "UTF-8")
        }

        val baseUrl = "http://localhost:8080/api/v1/maps/image"
        val params = mutableListOf(
            "size=${width}x$height",// Width x Height in pixels
            "maptype=roadmap", // roadmap, satellite, terrain, hybrid
        )

        // Add markers for each location
        encodedLocations.forEachIndexed { index, location ->
            params.add("markers=color:red%7Clabel:${index + 1}%7C$location")
        }

        // If multiple locations, add path between them
        if (encodedLocations.size > 1) {
            params.add("locations=${encodedLocations.joinToString("%7C")}")
        }

        return "$baseUrl?${params.joinToString("&")}"
    }

    override val content: String
        get() = """
            ${plan.title}
            ${plan.plan}
            Days: ${plan.days.joinToString(separator = "\n") { "${it.date} - ${it.stayingAt}" }}
            Map:
            $journeyMapUrl
            Pages:
            ${plan.pageLinks.joinToString("\n") { "${it.url} - ${it.summary}" }}
            Images:
            ${plan.imageLinks.joinToString("\n") { "${it.url} - ${it.summary}" }}
        """.trimIndent()
}
