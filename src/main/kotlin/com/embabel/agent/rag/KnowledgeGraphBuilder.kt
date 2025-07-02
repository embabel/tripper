package com.embabel.agent.rag

import com.embabel.agent.rag.support.NaiveEntityResolver
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class KnowledgeGraphBuilder(
    private val ingester: Ingester,
    private val chunkAnalyzer: ChunkAnalyzer,
    private val entityResolver: EntityResolver = NaiveEntityResolver(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun computeDelta(chunks: List<Chunk>, schema: Schema): KnowledgeGraphDelta? {
        if (chunks.isEmpty()) {
            logger.warn("No chunks provided for analysis")
            return null
        }
        if (chunks.size > 1) {
            TODO("Support multiple chunks")
        }
        return computeChunkDelta(chunks.single(), schema)
    }

    fun computeChunkDelta(chunk: Chunk, schema: Schema): KnowledgeGraphDelta {
        val suggestedEntities = chunkAnalyzer.identifyEntities(chunk, schema)
        logger.info("Suggested entities: {}", suggestedEntities)
        val entityResolution = entityResolver.resolve(suggestedEntities)
        logger.info("Entity resolution: {}", entityResolution)

        val knowledgeGraphUpdate = chunkAnalyzer.analyzeRelationships(entityResolution, schema)
        return knowledgeGraphUpdate
    }
}