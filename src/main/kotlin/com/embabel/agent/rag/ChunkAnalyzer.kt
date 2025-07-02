package com.embabel.agent.rag

interface EntityResolver {

    fun resolve(suggestedEntities: SuggestedEntities): SuggestedEntitiesResolution

}

/**
 * First identify entities in a chunk, then analyze relationships between them
 * once they've been resolved
 */
interface ChunkAnalyzer {

    /**
     * Identify entities in a chunk based on the provided schema.
     */
    fun identifyEntities(
        chunk: Chunk,
        schema: Schema,
    ): SuggestedEntities

    fun analyzeRelationships(
        entityResolution: SuggestedEntitiesResolution,
        schema: Schema,
    ): KnowledgeGraphDelta
}

