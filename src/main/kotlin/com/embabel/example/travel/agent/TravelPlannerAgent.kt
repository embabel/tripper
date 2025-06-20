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

import com.embabel.agent.api.annotation.AchievesGoal
import com.embabel.agent.api.annotation.Action
import com.embabel.agent.api.annotation.Agent
import com.embabel.agent.api.annotation.using
import com.embabel.agent.api.common.OperationContext
import com.embabel.agent.api.common.create
import com.embabel.agent.api.common.createObjectIfPossible
import com.embabel.agent.api.dsl.parallelMap
import com.embabel.agent.config.models.AnthropicModels
import com.embabel.agent.config.models.OpenAiModels
import com.embabel.agent.core.CoreToolGroups
import com.embabel.agent.core.ToolGroupRequirement
import com.embabel.agent.domain.io.UserInput
import com.embabel.agent.domain.special.Megazord
import com.embabel.agent.prompt.ResponseFormat
import com.embabel.agent.prompt.element.ToolCallControl
import com.embabel.agent.prompt.persona.Persona
import com.embabel.agent.prompt.persona.RoleGoalBackstory
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.ai.model.ModelSelectionCriteria.Companion.byName
import com.embabel.example.travel.service.PersonRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties

val HermesPersona = Persona(
    name = "Hermes",
    persona = "You are an expert travel planner",
    voice = "friendly and concise",
    objective = "Make a detailed travel plan meeting requirements",
)

val Researcher = RoleGoalBackstory(
    role = "Researcher",
    goal = "Research points of interest for a travel plan",
    backstory = "You are an expert researcher who can find interesting stories about art, culture, and famous people associated with places.",
)

@ConfigurationProperties("embabel.travel.planner")
data class TravelPlannerProperties(
    val wordCount: Int = 700,
    val travelPlannerPersona: Persona = HermesPersona,
    val researcher: RoleGoalBackstory = Researcher,
    val toolCallControl: ToolCallControl = ToolCallControl(),
    private val thinkerModel: String = AnthropicModels.CLAUDE_37_SONNET,
    private val researcherModel: String = OpenAiModels.GPT_41_MINI,
) {
    val thinkerLlm = LlmOptions(
        criteria = byName(thinkerModel),
    )

    val researcherLlm = LlmOptions(
        criteria = byName(researcherModel),
    )

}

/**
 * Overall flow:
 * 1. Lookup travelers based on a travel brief. Brief may be about exploring a location or a journey.
 * 2. Find points of interest based on travel brief, travelers and mapping data.
 * 3. Research each point of interest to gather detailed information.
 */
@Agent(description = "Make a detailed travel plan")
class TravelPlannerAgent(
    private val config: TravelPlannerProperties,
    private val personRepository: PersonRepository,
) {

    private val logger = LoggerFactory.getLogger(TravelPlannerAgent::class.java)

    @Action
    fun lookupTravelers(
        travelBrief: TravelBrief,
        context: OperationContext,
    ): Travelers {
//        val nlr = personRepository.naturalLanguageRepository({ it.id }, context, LlmOptions())
//        val entities = nlr.find(FindEntitiesRequest(content = travelBrief.brief))
//        if (entities.matches.isEmpty()) {
//            logger.info("No travelers found for travel brief: {}", travelBrief.brief)
//        }
//        return Travelers(entities.matches.map { it.match })
        return Travelers(emptyList())
    }

    data class MZ(val travelers: Travelers, val b: TravelBrief) : Megazord

    @Action
    fun planFromUserInput(userInput: UserInput, context: OperationContext): MZ? {
        val journeyTravelBrief = context.promptRunner()
            .createObjectIfPossible<JourneyTravelBrief>(
                """
                Given the following user input, extract a travel brief for a journey.
            """.trimIndent(),
            )
        val travelers = context.promptRunner()
            .createObjectIfPossible<Travelers>(
                """
                Given the following user input, extract information about travelers.
                It's fine to return an empty list if no travelers can be found.
            """.trimIndent(),
            )
        if (journeyTravelBrief == null || travelers == null) {
            logger.warn("Could not parse JourneyTravelBrief or Travelers from user input: {}", userInput.content)
            return null
        }
        return MZ(travelers, journeyTravelBrief)
    }

    @Action
    fun findPointsOfInterest(
        travelBrief: JourneyTravelBrief,
        travelers: Travelers,
    ): ItineraryIdeas {
        return using(
            llm = config.thinkerLlm.withMaxTokens(5000),
            promptContributors = listOf(
                config.travelPlannerPersona,
                travelers,
            ),
        ).withToolGroups(
            setOf(CoreToolGroups.WEB, CoreToolGroups.MAPS, CoreToolGroups.MATH),
        )
            .create(
                prompt = """
                Consider the following travel brief for a journey from ${travelBrief.from} to ${travelBrief.to}.
                ${travelBrief.contribution()}
                Find points of interest that are relevant to the travel brief and travelers.
                Use mapping tools to consider appropriate order
            """.trimIndent(),
            )
    }

    @Action
    fun researchPointsOfInterest(
        travelBrief: JourneyTravelBrief,
        travelers: Travelers,
        itineraryIdeas: ItineraryIdeas,
        context: OperationContext,
    ): PointOfInterestFindings {
        logger.info(
            "Researching {} points of interest: {}",
            itineraryIdeas.pointsOfInterest.size,
            itineraryIdeas.pointsOfInterest.sortedBy { it.name }.joinToString { it.name },
        )
        val promptRunner = context.promptRunner(
            llm = config.researcherLlm.withMaxTokens(5000),
            promptContributors = listOf(config.researcher, travelers, config.toolCallControl),
            toolGroups = setOf(
                ToolGroupRequirement(CoreToolGroups.WEB),
                ToolGroupRequirement(CoreToolGroups.BROWSER_AUTOMATION)
            ),
        )
        val poiFindings = itineraryIdeas.pointsOfInterest.parallelMap(
            context = context,
            concurrencyLevel = 6,
        ) { poi ->
            promptRunner.create<ResearchedPointOfInterest>(
                prompt = """
                Research the following point of interest.
                Consider in particular interesting stories about art and culture and famous people.
                Your audience: ${travelBrief.brief}
                Dates to consider: ${travelBrief.departureDate} to ${travelBrief.returnDate}
                If any particularly important events are happening here during this time, mention them
                and list specific dates.
                <point-of-interest-to-research>
                ${poi.name}
                ${poi.description}
                ${poi.location}
                </point-of-interest-to-research>
            """.trimIndent(),
            )
        }
        return PointOfInterestFindings(
            pointsOfInterest = poiFindings,
        )
    }

    @Action
    fun createMarkdownTravelPlan(
        travelBrief: JourneyTravelBrief,
        travelers: Travelers,
        poiFindings: PointOfInterestFindings,
    ): TravelPlan {
        return using(config.thinkerLlm.withMaxTokens(5000))
            .withToolGroups(setOf(CoreToolGroups.WEB, CoreToolGroups.MAPS, CoreToolGroups.MATH))
            .withPromptContributors(
                listOf(
                    config.travelPlannerPersona, travelers, ResponseFormat.MARKDOWN,
                )
            )
            .create(
                prompt = """
                Given the following travel brief, create a detailed plan.

                Plan the journey to minimize travel time.
                However, consider any important events or places of interest along the way
                that might inform routing.
                Include total distances.
                Include one or more links to the whole trip in Google Maps format.
                IMPORTANT: Do not include any special characters like accents in the links.

                <brief>${travelBrief.contribution()}</brief>
                Consider the weather in your recommendations. Use mapping tools to consider distance of driving or walking.

                Write up in ${config.wordCount} words or less.
                Include links in text where appropriate and in the links field.

                Put image links where appropriate in text and also in the links field.
                IMPORTANT: Image links must come from the web pages you found, not from
                general knowledge.

                Recount at least one interesting story about a famous person
                associated with an area.

                Consider the following points of interest:
                ${
                    poiFindings.pointsOfInterest.joinToString("\n") {
                        """
                    ${it.pointOfInterest.name}
                    ${it.research}
                    ${it.links.joinToString { link -> "${link.url}: ${link.summary}" }}
                """.trimIndent()
                    }
                }
            """.trimIndent(),
            )
    }

    @AchievesGoal(
        description = "Create a detailed travel plan based on a given travel brief",
    )
    @Action
    fun outputArtifact(
        travelPlan: TravelPlan,
    ): TravelPlan {
        // Sanitize the markdown content to ensure it is safe for display
        return travelPlan
    }
}
