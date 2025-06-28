package com.embabel.example.travel.agent

import com.embabel.agent.api.annotation.AchievesGoal
import com.embabel.agent.api.annotation.Action
import com.embabel.agent.api.annotation.Agent
import com.embabel.agent.api.annotation.usingDefaultLlm
import com.embabel.agent.api.common.ActionContext
import com.embabel.agent.api.common.TransformationActionContext
import com.embabel.agent.api.common.create
import com.embabel.agent.api.dsl.AgentScopeBuilder
import com.embabel.agent.api.workflow.ScoredResult
import com.embabel.agent.api.workflow.SimpleFeedback
import com.embabel.agent.api.workflow.Workflows
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

        fun generator(tac: TransformationActionContext<SimpleFeedback?, Report>): Report =
            tac.promptRunner().create(
                """
            Given the topic, generate a detailed report in ${reportRequest.words} words.
            
            # Topic
            ${reportRequest.topic}
            
            # Feedback
            ${tac.input ?: "No feedback provided"}
                    """.trimIndent()
            )

        return context.runAgent(
            Workflows.evaluatorOptimizer(
                maxIterations = 5,
                generator = ::generator,
                evaluator = {
                    it.promptRunner().create(
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
            )
        )
    }
}

fun <O : Any> ActionContext.runAgent(
    outputClass: Class<O>,
    agentScopeBuilder: AgentScopeBuilder<O>,
) =
    Workflows.runInAction(
        context = this,
        outputClass = outputClass,
        agentScopeBuilder,
    )

inline fun <reified O : Any> ActionContext.runAgent(
    agentScopeBuilder: AgentScopeBuilder<O>,
) =
    Workflows.runInAction(
        context = this,
        outputClass = O::class.java,
        agentScopeBuilder,
    )
