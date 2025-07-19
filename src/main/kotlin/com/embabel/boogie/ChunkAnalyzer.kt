package com.embabel.boogie

import com.embabel.agent.rag.Chunk

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
        schema: KnowledgeGraphSchema,
    ): SuggestedEntities

    fun suggestRelationships(
        suggestedEntitiesResolution: SuggestedEntitiesResolution,
        schema: KnowledgeGraphSchema,
    ): SuggestedRelationships
}

