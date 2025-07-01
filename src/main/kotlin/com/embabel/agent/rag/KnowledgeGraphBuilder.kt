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

    fun analyze(resource: String, schema: Schema): KnowledgeGraphDelta {
        val ingestionResult = ingester.ingest(resource)
        // We now have a document
        println(ingestionResult)
        TODO()
    }

    fun computeUpdate(chunk: Chunk, schema: Schema): KnowledgeGraphDelta {
        val suggestedEntities = chunkAnalyzer.identifyEntities(chunk, schema)
        logger.info("Suggested entities: {}", suggestedEntities)
        val entityResolution = entityResolver.resolve(suggestedEntities)
        logger.info("Entity resolution: {}", entityResolution)

        val knowledgeGraphUpdate = chunkAnalyzer.analyzeRelationships(entityResolution)
        return knowledgeGraphUpdate
    }
}