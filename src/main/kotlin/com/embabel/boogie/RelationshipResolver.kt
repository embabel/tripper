package com.embabel.boogie

import com.embabel.agent.rag.Retrievable
import com.embabel.boogie.schema.KnowledgeGraphSchema
import com.embabel.common.core.types.HasInfoString

sealed interface SuggestedRelationshipResolution : HasInfoString {
    val suggestedRelationship: SuggestedRelationship
}

data class NewRelationship(
    override val suggestedRelationship: SuggestedRelationship,
) : SuggestedRelationshipResolution {

    override fun infoString(verbose: Boolean?): String {
        return "NewRelationship(type=${suggestedRelationship.type}, sourceId=${suggestedRelationship.sourceId}, targetId=${suggestedRelationship.targetId})"
    }
}

data class ExistingRelationship(
    override val suggestedRelationship: SuggestedRelationship,
    val existingRelationship: RelationshipInstance,
) : SuggestedRelationshipResolution {

    override fun infoString(verbose: Boolean?): String {
        return "SuggestedRelationship(type=${suggestedRelationship.type}, sourceId=${suggestedRelationship.sourceId}, targetId=${suggestedRelationship.targetId})"
    }
}

data class SuggestedRelationshipsResolution(
    override val basis: Retrievable,
    val resolutions: List<SuggestedRelationshipResolution>,
) : Sourced


interface RelationshipResolver {

    /**
     * Analyze relationships between entities based on the provided schema.
     */
    fun resolveRelationships(
        entityResolution: SuggestedEntitiesResolution,
        suggestedRelationships: SuggestedRelationships,
        schema: KnowledgeGraphSchema,
    ): SuggestedRelationshipsResolution

}