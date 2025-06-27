package com.embabel.example.travel.agent

import com.embabel.agent.api.common.TransformationActionContext
import com.embabel.agent.api.common.support.SupplierAction
import com.embabel.agent.api.common.support.TransformationAction
import com.embabel.agent.api.dsl.AgentScopeBuilder
import com.embabel.agent.core.Action
import com.embabel.agent.core.support.Rerun
import com.embabel.common.core.MobyNameGenerator

/**
 * See https://www.anthropic.com/engineering/building-effective-agents
 */
class Workflows {

    fun <A, B, F> evaluatorOptimizer(
        generator: () -> B,
        evaluator: (TransformationActionContext<B, F>) -> F?,
        bClass: Class<B>,
        cClass: Class<F>,
    ): AgentScopeBuilder<B> {

        val actions = mutableListOf<Action>()

        val transformAction = SupplierAction(
            name = "=>${bClass.name}",
            description = "Generate $bClass",
            cost = 0.0,
            value = 0.0,
            canRerun = true,
            outputClass = bClass,
            toolGroups = emptySet(),
        ) {
            generator.invoke()
        }

        actions += transformAction
        val mergeAction = TransformationAction<B, F>(
            name = "List<${bClass.name}>=>${cClass.name}",
            description = "Aggregate list $bClass to $cClass",
            pre = listOf(Rerun.hasRunCondition(transformAction)),
            cost = 0.0,
            value = 0.0,
            canRerun = true,
            inputClass = bClass,
            outputClass = cClass,
            toolGroups = emptySet(),
        ) { context ->
            evaluator(context)
        }
        actions += mergeAction
        return AgentScopeBuilder(
            name = MobyNameGenerator.generateName(),
            actions = actions,
            goals = emptySet(),
        )
    }
}