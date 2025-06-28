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
import com.embabel.common.core.types.ZeroToOne
import org.slf4j.LoggerFactory

interface Feedback {
    val score: ZeroToOne
}

data class SimpleFeedback(
    override val score: ZeroToOne,
    val feedback: String,
) : Feedback

/**
 * See https://www.anthropic.com/engineering/building-effective-agents
 */
object Workflows {

    private val logger = LoggerFactory.getLogger(Workflows::class.java)

    @JvmStatic
    fun <RESULT : Any, FEEDBACK : Feedback> evaluatorOptimizer(
        // TODO is this right or need context
        generator: (FEEDBACK?) -> RESULT,
        evaluator: (TransformationActionContext<RESULT, FEEDBACK>) -> FEEDBACK,
        acceptanceCriteria: (FEEDBACK) -> Boolean,
        resultClass: Class<RESULT>,
        feedbackClass: Class<FEEDBACK>,
        maxIterations: Int,
    ): AgentScopeBuilder<RESULT> {

        val ACCEPTABLE = "acceptable"
        val REPORT_WAS_LAST_ACTION = "reportWasLastAction"
        val BEST_FEEDBACK = "bestFeedback"
        val BEST_RESULT = "bestResult"

        val actions = mutableListOf<Action>()

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
            val report = generator.invoke(it.last(feedbackClass))
            logger.info("Generated report: {}", report)
            report
        }

        actions += generationAction
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
                context[BEST_RESULT] = context.input
                context[BEST_FEEDBACK] = feedback
            }
            logger.info("Feedback is {}", feedback)
            feedback
        }
        actions += evaluationAction
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
                    // TODO should take best
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
        val resultGoal = Goal(
            "final-${resultClass.name}",
            "Satisfied with the final ${resultClass.name}",
            pre = setOf(ACCEPTABLE)
        )

        return AgentScopeBuilder(
            name = MobyNameGenerator.generateName(),
            actions = actions,
            conditions = setOf(acceptableCondition, reportWasLastActionCondition),
            goals = setOf(resultGoal)
        )
    }
}

fun <O : Any> AgentScopeBuilder<O>.runInAction(
    context: ActionContext,
    outputClass: Class<O>,
): O {
    val agent = build().createAgent(
        name = name,
        provider = provider,
        description = name,
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