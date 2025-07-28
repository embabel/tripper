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

import com.embabel.agent.BraveImageSearchService
import com.embabel.agent.api.annotation.*
import com.embabel.agent.api.common.OperationContext
import com.embabel.agent.api.common.SomeOf
import com.embabel.agent.api.common.create
import com.embabel.agent.api.common.createObjectIfPossible
import com.embabel.agent.config.models.AnthropicModels
import com.embabel.agent.config.models.OpenAiModels
import com.embabel.agent.core.CoreToolGroups
import com.embabel.agent.core.ToolGroupRequirement
import com.embabel.agent.domain.io.UserInput
import com.embabel.agent.prompt.ResponseFormat
import com.embabel.agent.prompt.element.ToolCallControl
import com.embabel.agent.prompt.persona.Persona
import com.embabel.agent.prompt.persona.RoleGoalBackstory
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.ai.model.ModelSelectionCriteria.Companion.byName
import com.embabel.common.util.StringTransformer
import com.embabel.tripper.config.ToolsConfig
import com.embabel.tripper.util.ImageChecker
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

@ConfigurationProperties("embabel.tripper")
data class TravelPlannerProperties(
    val wordCount: Int = 700,
    val imageWidth: Int = 800,
    val travelPlannerPersona: Persona = HermesPersona,
    val researcher: RoleGoalBackstory = Researcher,
    val toolCallControl: ToolCallControl = ToolCallControl(),
    private val thinkerModel: String = OpenAiModels.GPT_41,
    private val researcherModel: String = OpenAiModels.GPT_41_MINI,
    private val writerModel: String = AnthropicModels.CLAUDE_37_SONNET,
    val maxConcurrency: Int = 15,
) {
    val thinkerLlm = LlmOptions(
        criteria = byName(thinkerModel),
    )

    val researcherLlm = LlmOptions(
        criteria = byName(researcherModel),
    )

    val writerLlm = LlmOptions(
        criteria = byName(writerModel),
    )

}

/**
 * Overall flow:
 * 1. Lookup travelers based on a travel brief. Brief may be about exploring a location or a journey.
 * 2. Find points of interest based on travel brief, travelers and mapping data.
 * 3. Research each point of interest to gather detailed information.
 */
@Agent(description = "Make a detailed travel plan")
class TripperAgent(
    private val config: TravelPlannerProperties,
    private val braveImageSearch: BraveImageSearchService,
) {

    private val logger = LoggerFactory.getLogger(TripperAgent::class.java)


    @Action
    fun planFromUserInput(userInput: UserInput): JourneyTravelBrief? =
        using()
            .createObjectIfPossible(
                """
                Given the following user input, extract a travel brief for a journey.
                <user-input>${userInput.content}</user-input>
            """.trimIndent(),
            )


    @Action
    fun findPointsOfInterest(
        travelBrief: JourneyTravelBrief,
        travelers: Travelers,
    ): ItineraryIdeas {
        return using(
            llm = config.thinkerLlm,
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
                Use mapping tools to consider appropriate order and put a rough date
                range for each point of interest.
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
            llm = config.researcherLlm,
            promptContributors = listOf(config.researcher, travelers, config.toolCallControl),
            toolGroups = setOf(
                ToolGroupRequirement(CoreToolGroups.WEB),
                ToolGroupRequirement(CoreToolGroups.BROWSER_AUTOMATION),
            ),
        ).withToolObject(braveImageSearch)
        val poiFindings = context.parallelMap(
            itineraryIdeas.pointsOfInterest,
            maxConcurrency = config.maxConcurrency,
        ) { poi ->
            val rpi = promptRunner.create<ResearchedPointOfInterest>(
                prompt = """
                Research the following point of interest.
                Consider interesting stories about art and culture and famous people.
                Your audience: ${travelBrief.brief}
                Dates to consider: ${travelBrief.departureDate} to ${travelBrief.returnDate}
                If any particularly important events are happening here during this time, mention them
                and list specific dates.
                <point-of-interest-to-research>
                ${poi.name}
                ${poi.description}
                ${poi.location}
                Date: from ${poi.fromDate} to: ${poi.toDate}
                </point-of-interest-to-research>
                Use the image search tool to find images of the point of interest.
            """.trimIndent(),
            )
            rpi
        }
        return PointOfInterestFindings(
            pointsOfInterest = poiFindings,
        )
    }

    @Action
    fun proposeTravelPlan(
        travelBrief: JourneyTravelBrief,
        travelers: Travelers,
        poiFindings: PointOfInterestFindings,
    ): ProposedTravelPlan {
        return using(config.writerLlm)
            .withToolGroups(setOf(CoreToolGroups.WEB, CoreToolGroups.MAPS, CoreToolGroups.MATH))
            .withPromptContributors(
                listOf(
                    config.travelPlannerPersona, travelers, ResponseFormat.HTML,
                )
            )
            .create(
                prompt = """
                Given the following travel brief, create a detailed plan.
                Give it a brief, catchy title that doesn't include dates,
                but may consider season, mood or relate to travelers's interests.

                Plan the journey to minimize travel time.
                However, consider any important events or places of interest along the way
                that might inform routing.
                Include total distances.

                <brief>${travelBrief.contribution()}</brief>
                Consider the weather in your recommendations. Use mapping tools to consider distance of driving or walking.

                Write up in ${config.wordCount} words or less.
                Include links in text where appropriate and in the links field.
                
                The Day field locationAndCountry field should be in the format <location,+Country> e.g.
                Ghent,+Belgium

                Put image links where appropriate in text and also in the links field.
                Links must specify opening in a new window.
                IMPORTANT: Image links must have been provided by the researchers
                          and not be general knowledge or from other web sites.

                Recount at least one interesting story about a famous person
                associated with an area.
                
                Include natural headings and paragraphs in HTML format.
                Use unordered lists as appropriate.
                Start any headings at <h4>
                Embed images in text, with max width of ${config.imageWidth}px.
                Be sure to include informative caption and alt text for each image.

                Consider the following points of interest:
                ${
                    poiFindings.pointsOfInterest.joinToString("\n") {
                        """
                    ${it.pointOfInterest.name}
                    ${it.research}
                    ${it.links.joinToString { link -> "${link.url}: ${link.summary}" }}
                    Images: ${it.imageLinks.joinToString { link -> "${link.url}: ${link.summary}" }}

                """.trimIndent()
                    }
                }
            """.trimIndent(),
            )
    }

    @Action
    fun findPlacesToSleep(
        brief: JourneyTravelBrief,
        plan: ProposedTravelPlan,
        travelers: Travelers,
        context: OperationContext,
    ): TravelPlan {
        // Sanitize the content to ensure it is safe for display
        val stays = plan.days.groupBy { it.stayingAt }.map { (stayingAt, days) ->
            Stay(
                days = days,
            )
        }.sortedBy { it.days.first().date }

        val stayFinderPromptRunner = context.promptRunner()
            .withLlm(config.researcherLlm)
            .withPromptContributor(travelers)
            .withToolGroups(setOf(ToolsConfig.AIRBNB, CoreToolGroups.MATH))
        val foundStays = context.parallelMap(stays, maxConcurrency = config.maxConcurrency) { stay ->
            logger.info("Finding Airbnb options for stay at: {}", stay.locationAndCountry())
            val airbnbResults = stayFinderPromptRunner
                .create<AirbnbResults>(
                    prompt = """
                Find the Airbnb search URL for the following stay using the available tools.
                Staying at location: ${stay.stayingAt()}
                Dates: ${stay.days.joinToString { it.date.toString() }}
                You MUST set the 'ignoreRobotsText' parameter value to true for all calls to the airbnb API
            """.trimIndent(),
                )
            stay.copy(
                airbnbUrl = airbnbResults.searchUrl,
            )
        }

        return TravelPlan(
            brief = brief,
            proposal = plan,
            stays = foundStays,
            travelers = travelers,
        )
    }

    @AchievesGoal(
        description = "Create a detailed travel plan based on a given travel brief",
        export = Export(
            name = "makeTravelPlan",
            remote = true,
            exposeTextInput = false,
            startingInputTypes = [TravelersAndBrief::class],
        ),
    )
    @Action
    fun postProcessHtml(
        plan: TravelPlan
    ): TravelPlan {
        val oldPlan = plan.proposal.plan
        return plan.copy(
            proposal = plan.proposal.copy(
                plan = StringTransformer.transform(
                    oldPlan, listOf(
                        styleImages,
                        ImageChecker.removeInvalidImageLinks,
                    )
                ),
            ),
        )
    }


    private val styleImages = StringTransformer { html ->
        html.replace(
            "<img",
            "<img class=\"styled-image-thick\""
        )
    }

}

private data class AirbnbResults(
    val searchUrl: String,
)

data class TravelersAndBrief(
    val travelers: Travelers,
    val brief: JourneyTravelBrief,
) : SomeOf