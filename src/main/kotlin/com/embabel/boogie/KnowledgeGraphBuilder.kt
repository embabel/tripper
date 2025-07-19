package com.embabel.boogie

import com.embabel.agent.rag.Chunk
import com.embabel.boogie.support.NaiveEntityDeterminer
import com.embabel.boogie.support.NaiveEntityResolver
import com.embabel.boogie.support.NaiveRelationshipDeterminer
import com.embabel.boogie.support.NaiveRelationshipResolver
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Strategy-based builder for knowledge graphs.
 */
@Service
class KnowledgeGraphBuilder(
    private val chunkAnalyzer: ChunkAnalyzer,
    private val entityResolver: EntityResolver = NaiveEntityResolver(),
    private val relationshipResolver: RelationshipResolver = NaiveRelationshipResolver(),
    private val entityDeterminer: EntityDeterminer = NaiveEntityDeterminer(),
    private val relationshipDeterminer: RelationshipDeterminer = NaiveRelationshipDeterminer(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun computeDelta(chunks: List<Chunk>, schema: KnowledgeGraphSchema): KnowledgeGraphDelta? {
        if (chunks.isEmpty()) {
            logger.warn("No chunks provided for analysis")
            return null
        }
        if (chunks.size > 1) {
            TODO("Support multiple chunks")
        }
        return computeChunkDelta(chunks.single(), schema)
    }

    fun computeChunkDelta(chunk: Chunk, schema: KnowledgeGraphSchema): KnowledgeGraphDelta {
        val suggestedEntities = chunkAnalyzer.suggestEntities(chunk, schema)
        logger.info("Suggested entities: {}", suggestedEntities)
        val entitiesResolution = entityResolver.resolve(suggestedEntities, schema)
        logger.info("Entity resolution: {}", entitiesResolution)
        val suggestedRelationships = chunkAnalyzer.suggestRelationships(entitiesResolution, schema)
        logger.info("Suggested relationships: {}", suggestedRelationships)
        val relationshipsResolution = relationshipResolver.resolveRelationships(
            entitiesResolution,
            suggestedRelationships,
            schema,
        )
        logger.info("Relationships resolution: {}", relationshipsResolution)
        val relationshipDeterminations = relationshipDeterminer.determineRelationships(relationshipsResolution, schema)
        
        val entityDeterminations = entityDeterminer.determineEntities(entitiesResolution, schema)
        return KnowledgeGraphDelta(
            basis = chunk,
            entityDeterminations = entityDeterminations,
            relationshipDeterminations = relationshipDeterminations,
        )
    }
}