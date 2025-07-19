package com.embabel.boogie

/**
 * Resolves entities based on existing data
 */
interface EntityResolver {

    fun resolve(
        suggestedEntities: SuggestedEntities,
        schema: KnowledgeGraphSchema,
    ): SuggestedEntitiesResolution

}