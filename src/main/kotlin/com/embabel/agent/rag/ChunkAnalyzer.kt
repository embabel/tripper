package com.embabel.agent.rag

interface EntityResolver {

    fun resolve(suggestedEntities: SuggestedEntities): SuggestedEntitiesResolution

}

interface RelationshipResolver {

    /**
     * Analyze relationships between entities based on the provided schema.
     */
    fun resolveRelationships(
        entityResolution: SuggestedEntitiesResolution,
        schema: Schema,
    ): KnowledgeGraphDelta

}

/**
 * First identify entities in a chunk, then analyze relationships between them
 * once they've been resolved
 */
interface ChunkAnalyzer {

    /**
     * Identify entities in a chunk based on the provided schema.
     */
    fun suggestEntities(
        chunk: Chunk,
        schema: Schema,
    ): SuggestedEntities

    fun suggestRelationships(
        suggestedEntitiesResolution: SuggestedEntitiesResolution,
        schema: Schema,
    ): SuggestedRelationships
}

