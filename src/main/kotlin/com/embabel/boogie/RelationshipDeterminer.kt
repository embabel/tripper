package com.embabel.boogie

import com.embabel.agent.rag.Retrievable

data class RelationshipDetermination(
    val resolution: SuggestedRelationshipResolution,
    val relationship: RelationshipInstance
)

/**
 * Decide on final entities to write
 */
data class RelationshipDeterminations(
    override val basis: Retrievable,
    val determinations: List<RelationshipDetermination>,
) : Sourced

interface RelationshipDeterminer {

    /**
     * Determine final relationships to write based on the suggested relationships resolution.
     */
    fun determineRelationships(
        suggestedRelationshipsResolution: SuggestedRelationshipsResolution,
        schema: KnowledgeGraphSchema,
    ): RelationshipDeterminations
}