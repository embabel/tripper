package com.embabel.boogie

import com.embabel.agent.rag.Chunk

/**
 * Resolves entities based on existing data
 */
interface EntityResolver {

    fun resolve(
        suggestedEntities: SuggestedEntities,
        schema: KnowledgeGraphSchema,
    ): SuggestedEntitiesResolution

}

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

