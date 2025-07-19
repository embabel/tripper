package com.embabel.boogie

import com.embabel.agent.rag.EntityData
import com.embabel.agent.rag.Retrievable

data class EntityDetermination(
    val resolution: SuggestedEntityResolution,
    val entityProduct: EntityData?
)

/**
 * Decide on final entities to write
 */
data class EntityDeterminations(
    override val basis: Retrievable,
    val determinations: List<EntityDetermination>,
) : Sourced

interface EntityDeterminer {

    /**
     * Determine final entities to write based on the suggested entities resolution.
     */
    fun determineEntities(
        suggestedEntitiesResolution: SuggestedEntitiesResolution,
        schema: KnowledgeGraphSchema,
    ): EntityDeterminations
}