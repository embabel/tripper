package com.embabel.example.travel.agent

import com.embabel.agent.api.annotation.AchievesGoal
import com.embabel.agent.api.annotation.Action
import com.embabel.agent.api.annotation.Condition
import com.embabel.agent.api.annotation.usingDefaultLlm
import com.embabel.agent.api.common.OperationContext
import com.embabel.agent.api.common.create
import com.embabel.agent.api.workflow.SimpleFeedback
import com.embabel.agent.core.count
import com.embabel.agent.core.last
import com.embabel.agent.domain.io.UserInput
import com.embabel.agent.domain.library.HasContent
import com.embabel.agent.rag.RagService
import com.embabel.common.core.types.Timestamped
import org.slf4j.LoggerFactory
import java.time.Instant


// TODO allow to return an agent
data class ReportRequest(
    val topic: String,
    val words: Int,
    override val timestamp: Instant = Instant.now(),
) : Timestamped


data class Report(
    val request: ReportRequest,
    val iterations: Int,
    val feedback: SimpleFeedback?,
    override val content: String,
    override val timestamp: Instant = Instant.now(),
) : Timestamped, HasContent

//@Agent(description = "report on a given topic")
class RagAgent(
    private val acceptanceThreshold: Double = 1.0,
    private val maxIterations: Int = 10,
    private val ragService: RagService,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    data class InterimReport(
        val report: String,
    )

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

    @Action(
        canRerun = true,
        pre = [NEED_REDO],
        post = [REPORT_IS_ACCEPTABLE],
        outputBinding = "interimReport",
    )
    fun iterate(
        reportRequest: ReportRequest,
        feedback: SimpleFeedback?,
    ): InterimReport =
        usingDefaultLlm.create(
            """
            Given the topic and word count, generate a detailed report.
            # Topic
            ${reportRequest.topic}
            Report in ${reportRequest.words} words.
            
            Consider the following feedback:
            ${feedback?.feedback ?: "No feedback yet."}
            """.trimIndent()
        )

    @Action(
        canRerun = true,
        post = [REPORT_IS_ACCEPTABLE],
    )
    fun evaluate(
        reportRequest: ReportRequest,
        interimReport: InterimReport,
    ): SimpleFeedback =
        usingDefaultLlm.create(
            """
            Given the report request and the proposed report,
            evaluate the report and provide feedback.
            Score the report from 0 to 1, where 1 is perfect.
            
            # Report Request
            ${reportRequest.topic}
            ${reportRequest.words} words.
            
            # Proposed Report
            ${interimReport.report}
            """.trimIndent()
        )

    @Condition(name = NEED_REDO)
    fun needRedo(
        context: OperationContext,
    ): Boolean {
        if (context.count<InterimReport>() == 0) {
            logger.info("No interim reports found, need to report")
            return true
        }
        val feedback = context.last<SimpleFeedback>()
        return feedback == null || feedback.score < acceptanceThreshold
            .also { logger.info("Need redo: {}", it) }
    }

    @Condition(name = REPORT_IS_ACCEPTABLE)
    fun reportIsAcceptable(
        feedback: SimpleFeedback,
        context: OperationContext,
    ): Boolean {
        val iterations = context.count<InterimReport>()
        logger.info("Feedback: {}", feedback)
        if (feedback.score < acceptanceThreshold) {
            if (iterations < maxIterations) {
                return false
            } else {
                logger.info(
                    "Report still not acceptable after {} iterations, but we're going to have to accept it",
                    iterations,
                )
            }
        }
        return true
    }

    @Action(
        pre = [REPORT_IS_ACCEPTABLE],
    )
    @AchievesGoal("Report is finalized")
    fun compileReport(
        reportRequest: ReportRequest,
        interimReport: InterimReport,
        feedback: SimpleFeedback,
        context: OperationContext,
    ): Report {
        val report = Report(
            request = reportRequest,
            iterations = context.count<InterimReport>(),
            feedback = feedback,
            content = interimReport.report,
        )
        logger.info("Final report:\n{}", report)
        return report
    }

    companion object {
        const val REPORT_IS_ACCEPTABLE = "FinalReport"
        const val NEED_REDO = "need_redo"
    }

}
