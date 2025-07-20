package com.embabel.boogie

import com.embabel.agent.rag.NamedEntityData
import com.embabel.agent.rag.Retrievable
import com.embabel.boogie.schema.KnowledgeGraphSchema

typealias EntityDetermination = Convergence<SuggestedEntityResolution, NamedEntityData>

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