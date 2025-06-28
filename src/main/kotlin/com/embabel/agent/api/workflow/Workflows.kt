package com.embabel.agent.api.workflow

import com.embabel.agent.api.common.ActionContext
import com.embabel.agent.api.common.TransformationActionContext
import com.embabel.agent.api.common.agentTransformer
import com.embabel.agent.api.common.support.SupplierAction
import com.embabel.agent.api.common.support.TransformationAction
import com.embabel.agent.api.dsl.AgentScopeBuilder
import com.embabel.agent.core.Action
import com.embabel.agent.core.ComputedBooleanCondition
import com.embabel.agent.core.Goal
import com.embabel.agent.core.last
import com.embabel.common.core.MobyNameGenerator
import com.embabel.common.core.types.Timestamped
import com.embabel.common.core.types.ZeroToOne
import org.slf4j.LoggerFactory
import java.time.Instant

interface Feedback {
    val score: ZeroToOne
}

data class SimpleFeedback(
    override val score: ZeroToOne,
    val feedback: String,
) : Feedback

data class ScoredResult<RESULT, FEEDBACK>(
    val result: RESULT,
    val feedback: FEEDBACK,
    val iterations: Int,
) : Timestamped {

    override val timestamp: Instant = Instant.now()

}

/**
 * See https://www.anthropic.com/engineering/building-effective-agents
 */
object Workflows {

    private val logger = LoggerFactory.getLogger(Workflows::class.java)

    @JvmStatic
    fun <RESULT : Any, FEEDBACK : Feedback> runEvaluatorOptimizer(
        context: ActionContext,
        generator: (TransformationActionContext<FEEDBACK?, RESULT>) -> RESULT,
        evaluator: (TransformationActionContext<RESULT, FEEDBACK>) -> FEEDBACK,
        acceptanceCriteria: (FEEDBACK) -> Boolean,
        maxIterations: Int,
        resultClass: Class<RESULT>,
        feedbackClass: Class<FEEDBACK>,
    ): ScoredResult<RESULT, FEEDBACK> {
        val agentScope = evaluatorOptimizer(
            generator = generator,
            evaluator = evaluator,
            acceptanceCriteria = acceptanceCriteria,
            maxIterations = maxIterations,
            resultClass = resultClass,
            feedbackClass = feedbackClass,
        )
        return runInAction(context, ScoredResult::class.java as Class<ScoredResult<RESULT, FEEDBACK>>, agentScope)
    }

    inline fun <reified RESULT : Any, reified FEEDBACK : Feedback> evaluatorOptimizer(
        noinline generator: (TransformationActionContext<FEEDBACK?, RESULT>) -> RESULT,
        noinline evaluator: (TransformationActionContext<RESULT, FEEDBACK>) -> FEEDBACK,
        maxIterations: Int,
        noinline acceptanceCriteria: (FEEDBACK) -> Boolean = { it.score >= 0.98 },
    ): AgentScopeBuilder<ScoredResult<RESULT, FEEDBACK>> =
        evaluatorOptimizer(
            generator = generator,
            evaluator = evaluator,
            acceptanceCriteria = acceptanceCriteria,
            maxIterations = maxIterations,
            resultClass = RESULT::class.java,
            feedbackClass = FEEDBACK::class.java,
        )

    @JvmStatic
    fun <RESULT : Any, FEEDBACK : Feedback> evaluatorOptimizer(
        generator: (TransformationActionContext<FEEDBACK?, RESULT>) -> RESULT,
        evaluator: (TransformationActionContext<RESULT, FEEDBACK>) -> FEEDBACK,
        acceptanceCriteria: (FEEDBACK) -> Boolean,
        maxIterations: Int,
        resultClass: Class<RESULT>,
        feedbackClass: Class<FEEDBACK>,
    ): AgentScopeBuilder<ScoredResult<RESULT, FEEDBACK>> {

        val ACCEPTABLE = "acceptable"
        val REPORT_WAS_LAST_ACTION = "reportWasLastAction"
        val BEST_FEEDBACK = "bestFeedback"
        val BEST_RESULT = "bestResult"

        val generationAction = SupplierAction(
            name = "=>${resultClass.name}",
            description = "Generate $resultClass",
            post = listOf("reportWasLastAction"),
            cost = 0.0,
            value = 0.0,
            canRerun = true,
            outputClass = resultClass,
            toolGroups = emptySet(),
        ) {
            val tac = (it as TransformationActionContext<FEEDBACK?, RESULT>).copy(
                input = it.last(feedbackClass)
            )
            val report = generator.invoke(tac)
            logger.info("Generated report: {}", report)
            report
        }

        val evaluationAction = TransformationAction(
            name = "${resultClass.name}=>${feedbackClass.name}",
            description = "Evaluate $resultClass to $feedbackClass",
            pre = listOf(REPORT_WAS_LAST_ACTION),
            post = listOf(ACCEPTABLE),
            cost = 0.0,
            value = 0.0,
            canRerun = true,
            inputClass = resultClass,
            outputClass = feedbackClass,
            toolGroups = emptySet(),
        ) { context ->
            val feedback = evaluator(context)
            val bestSoFar = context[BEST_FEEDBACK] as FEEDBACK?
            if (bestSoFar == null || feedback.score > bestSoFar.score) {
                logger.info(
                    "New best feedback found: {} (was {})",
                    feedback,
                    bestSoFar ?: "none",
                )
                context[BEST_RESULT] = context.input
                context[BEST_FEEDBACK] = feedback
            }
            logger.info("Feedback is {}", feedback)
            feedback
        }
        val reportWasLastActionCondition = ComputedBooleanCondition(
            name = REPORT_WAS_LAST_ACTION,
            evaluator = { context, _ ->
                val result = context.lastResult()
                result != null && result::class.java == resultClass
            },
        )

        val acceptableCondition = ComputedBooleanCondition(
            name = ACCEPTABLE,
            evaluator = { context, _ ->
                val iterations = context.objects.filterIsInstance(resultClass)
                    .count()
                if (iterations > maxIterations) {
                    logger.info("Giving up after {} iterations", iterations)
                    context += context[BEST_FEEDBACK] ?: throw IllegalStateException(
                        "No feedback found in context after $maxIterations iterations"
                    )
                    context += context[BEST_RESULT] ?: throw IllegalStateException(
                        "No result found in context after $maxIterations iterations"
                    )
                    true
                } else {
                    val feedback = context.last(feedbackClass)
                    if (feedback == null) {
                        logger.info("No feedback to evaluate")
                        false
                    } else {
                        val isAcceptable = acceptanceCriteria(feedback)
                        logger.info(
                            "Feedback acceptable={}: {}",
                            isAcceptable,
                            feedback,
                        )
                        isAcceptable
                    }
                }
            }
        )
        val consolidateAction: Action = SupplierAction(
            name = "consolidate-${resultClass.name}-${feedbackClass.name}",
            description = "Consolidate results and feedback",
            pre = listOf(ACCEPTABLE),
            cost = 0.0,
            value = 0.0,
            toolGroups = emptySet(),
            outputClass = ScoredResult::class.java,
        ) {
            val bestFeedback = it[BEST_FEEDBACK] as FEEDBACK
            val bestResult = it[BEST_RESULT] as RESULT
            ScoredResult(
                result = bestResult,
                feedback = bestFeedback,
                // Remove the last result which is best
                iterations = it.objects.filterIsInstance(resultClass).count() - 2,
            )
        }

        val resultGoal = Goal(
            "final-${resultClass.name}",
            "Satisfied with the final ${resultClass.name}",
            satisfiedBy = ScoredResult::class.java,
        )

        return AgentScopeBuilder(
            name = MobyNameGenerator.generateName(),
            actions = listOf(
                generationAction,
                evaluationAction,
                consolidateAction,
            ),
            conditions = setOf(acceptableCondition, reportWasLastActionCondition),
            goals = setOf(resultGoal)
        )
    }

    fun <O : Any> runInAction(
        context: ActionContext,
        outputClass: Class<O>,
        agentScopeBuilder: AgentScopeBuilder<O>,
    ): O {
        val agent = agentScopeBuilder.build().createAgent(
            name = agentScopeBuilder.name,
            provider = agentScopeBuilder.provider,
            description = agentScopeBuilder.name,
        )
        val singleAction = agentTransformer(
            agent = agent,
            inputClass = Unit::class.java,
            outputClass = outputClass,
        )

        singleAction.execute(
            processContext = context.processContext,
            action = context.action!!,
        )
        return context.last(outputClass) ?: throw IllegalStateException(
            "No output of type ${outputClass.name} found in context"
        )
    }
}

