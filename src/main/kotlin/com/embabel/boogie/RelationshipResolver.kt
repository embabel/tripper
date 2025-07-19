package com.embabel.boogie

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