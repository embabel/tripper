package com.embabel.agent.rag

interface EntityResolver {

    fun resolve(suggestedEntities: SuggestedEntities): EntityResolution

}

/**
 * First identify entities in a chunk, then analyze relationships between them
 * once they've been resolved
 */
interface ChunkAnalyzer {

    fun identifyEntities(
        chunk: Chunk,
        schema: Schema,
    ): SuggestedEntities

    fun analyzeRelationships(
        entityResolution: EntityResolution,
    ): KnowledgeGraphDelta
}

