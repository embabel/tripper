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
    val startDate: LocalDate
    val endDate: LocalDate
}

data class ExplorationTravelBrief(
    val areaToExplore: String,
    val stayingAt: String,
    override val brief: String,
    override val startDate: LocalDate,
    override val endDate: LocalDate,
) : TravelBrief {

    override fun contribution(): String =
        """
        Area to explore: $areaToExplore
        Staying at: $stayingAt
        Brief: $brief
        Dates: $startDate to $endDate
    """.trimIndent()
}

data class JourneyTravelBrief(
    val from: String,
    val to: String,
    val transportPreference: String,
    override val brief: String,
    override val startDate: LocalDate,
    override val endDate: LocalDate,
) : TravelBrief {

    override fun contribution(): String =
        """
        Journey from $from to $to
        Dates: $startDate to $endDate
        Brief: $brief
        Transport preference: $transportPreference
    """.trimIndent()
}

data class Travelers(
    val people: List<Person>,
) : PromptContributor {

    override fun contribution(): String =
        "Travelers:\n" + people.joinToString(separator = "\n") {
            "${it.name}: activities:${it.activities.joinToString(", ") { act -> act.name }}"
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
            Maps:
            ${if (mapLinks.isNotEmpty()) "Maps: ${mapLinks.joinToString(", ")}" else ""}
            Pages:
            ${pageLinks.joinToString("\n") { "${it.url} - ${it.summary}" }}
            Images:
            ${imageLinks.joinToString("\n") { "${it.url} - ${it.summary}" }}           
        """.trimIndent()
}