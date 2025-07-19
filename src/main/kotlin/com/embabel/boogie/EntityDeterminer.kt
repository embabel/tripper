package com.embabel.boogie

interface EntityDeterminer {

    /**
     * Determine final entities to write based on the suggested entities resolution.
     */
    fun determineEntities(
        suggestedEntitiesResolution: SuggestedEntitiesResolution,
        schema: KnowledgeGraphSchema,
    ): EntityDeterminations
}