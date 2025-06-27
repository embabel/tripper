package com.embabel.example.travel.agent

import com.embabel.agent.api.annotation.AchievesGoal
import com.embabel.agent.api.annotation.Action
import com.embabel.agent.api.annotation.Agent
import com.embabel.agent.api.annotation.usingDefaultLlm
import com.embabel.agent.api.common.create
import com.embabel.agent.domain.io.UserInput
import com.embabel.agent.domain.library.HasContent
import com.embabel.agent.rag.RagService
import com.embabel.common.core.types.Timestamped
import java.time.Instant

data class Question(
    val question: String,
    // TODO context
)

data class ReportRequest(
    val topic: String,
    val words: Int,
    override val timestamp: Instant = Instant.now(),
) : Timestamped

data class Report(
    val request: ReportRequest,
    override val content: String,
    override val timestamp: Instant = Instant.now(),
) : Timestamped, HasContent


@Agent(description = "report on a given topic")
class RagAgent(
    private val ragService: RagService,
) {

    @Action
    fun answerQuestion(question: Question) {
        TODO()
    }

    @Action
    fun fromUserInput(
        userInput: UserInput,
    ): ReportRequest = usingDefaultLlm.create(
        """
            Given the user input, extract the topic to report on.
            # User input 
            ${userInput.content}
        """.trimIndent()
    )

    @AchievesGoal(
        description = "Topic report was created",
    )
    @Action
    fun report(reportRequest: ReportRequest): Report {
        return Report(
            request = reportRequest,
            content = "Report on ${reportRequest.topic} of ${reportRequest.words} words.",
        )
    }
}