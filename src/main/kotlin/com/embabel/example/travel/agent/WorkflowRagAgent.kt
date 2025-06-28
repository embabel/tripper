package com.embabel.example.travel.agent

import com.embabel.agent.api.annotation.AchievesGoal
import com.embabel.agent.api.annotation.Action
import com.embabel.agent.api.annotation.Agent
import com.embabel.agent.api.annotation.usingDefaultLlm
import com.embabel.agent.api.common.ActionContext
import com.embabel.agent.api.common.create
import com.embabel.agent.api.workflow.ScoredResult
import com.embabel.agent.api.workflow.SimpleFeedback
import com.embabel.agent.api.workflow.Workflows
import com.embabel.agent.api.workflow.runInAction
import com.embabel.agent.domain.io.UserInput
import com.embabel.agent.rag.RagService

data class Report(
    val report: String,
)

@Agent(description = "report on a given topic")
class WorkflowRagAgent(
    private val ragService: RagService,
) {

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
    fun report(
        reportRequest: ReportRequest,
        context: ActionContext,
    ): ScoredResult<Report, SimpleFeedback> {

        val evaluator = Workflows.evaluatorOptimizer(
            generator = {
                context.promptRunner().create<Report>(
                    """
            Given the topic, generate a detailed report in ${reportRequest.words} words.
            
            # Topic
            ${reportRequest.topic}
            
            # Feedback
            ${it?.feedback ?: "No feedback provided"}
                    """.trimIndent()
                )
            },
            evaluator = {
                context.promptRunner().create<SimpleFeedback>(
                    """
            Given the topic and word count, evaluate the report and provide feedback
            Feedback must be a score between 0 and 1, where 1 is perfect.
            
            # Report
            ${it.input.report}
            
            # Report request:
          
            ${reportRequest.topic}
            Word count: ${reportRequest.words}
            """.trimIndent()
                )
            },
            resultClass = Report::class.java,
            feedbackClass = SimpleFeedback::class.java,
            acceptanceCriteria = { it.score > .96 },
            maxIterations = 5,
        )
        return evaluator.runInAction(
            context,
            outputClass = ScoredResult::class.java as Class<ScoredResult<Report, SimpleFeedback>>,
        )
    }
}