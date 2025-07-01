package com.embabel.agent.rag

import com.embabel.common.core.types.HasInfoString
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

data class SuggestedEntity(
    val type: String,
    val summary: String,
    @JsonPropertyDescription("Will be a UUID. Include only if provided")
    val id: String? = null,
) {

    fun resolve(
        entityData: EntityData?,
    ): ResolvedEntity {
        return ResolvedEntity(
            suggestedEntity = this,
            entityData = entityData,
        )
    }
}

data class ResolvedEntity(
    val suggestedEntity: SuggestedEntity,
    val entityData: EntityData?,
) {

    fun isNew(): Boolean = entityData == null

}

data class SuggestedEntities(
    val chunk: Chunk,
    val suggestedEntities: List<SuggestedEntity>,
)

data class EntityResolution(
    val chunk: Chunk,
    val resolvedEntities: List<ResolvedEntity>,
)

interface EntityResolver {

    fun resolve(suggestedEntities: SuggestedEntities): EntityResolution

}

data class SuggestedRelationship(
    val sourceId: String,
    val targetId: String,
    val type: String,
    val description: String? = null,
)

data class KnowledgeGraphUpdate(
    val entityResolution: EntityResolution,
    val relationships: List<SuggestedRelationship>,
) : HasInfoString {


    fun newEntities(): List<EntityData> {
        return entityResolution.resolvedEntities.mapNotNull { it.entityData }
    }

    override fun infoString(verbose: Boolean?): String {
        return "KnowledgeGraphUpdate(entities=${entityResolution.resolvedEntities.size}, relationships=${relationships.size}, entityLabels=${
            entityResolution.resolvedEntities.mapNotNull { it.entityData?.labels }.flatten().distinct()
                .joinToString(", ")
        }, relationshipTypes=${relationships.map { it.type }.distinct().joinToString(", ")})"
    }
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
    ): KnowledgeGraphUpdate
}

data class SimpleEntityData(
    override val id: String,
    override val description: String,
    override val labels: Set<String>,
    override val properties: Map<String, Any>,
) : EntityData {
    override fun persistent(): Boolean {
        return false
    }
}

// Trust in all entities
class NaiveEntityResolver : EntityResolver {

    override fun resolve(suggestedEntities: SuggestedEntities): EntityResolution {
        // For simplicity, let's assume we resolve entities by their name
        val resolvedEntities = suggestedEntities.suggestedEntities.map {
            it.resolve(
                SimpleEntityData(
                    id = it.id ?: UUID.randomUUID().toString(),
                    description = it.summary,
                    labels = setOf(it.type),
                    properties = emptyMap(),
                )
            )
        }
        return EntityResolution(
            chunk = suggestedEntities.chunk,
            resolvedEntities = resolvedEntities,
        )
    }

}

@Service
class ChunkIngester(
    private val chunkAnalyzer: ChunkAnalyzer,
    private val entityResolver: EntityResolver = NaiveEntityResolver(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun computeUpdate(chunk: Chunk, schema: Schema): KnowledgeGraphUpdate {
        val suggestedEntities = chunkAnalyzer.identifyEntities(chunk, schema)
        logger.info("Suggested entities: {}", suggestedEntities)
        val entityResolution = entityResolver.resolve(suggestedEntities)
        logger.info("Entity resolution: {}", entityResolution)

        val knowledgeGraphUpdate = chunkAnalyzer.analyzeRelationships(entityResolution)
        return knowledgeGraphUpdate
    }
}


interface Projector {

    /**
     * Project somewhere
     */
    fun project(
        knowledgeGraphUpdate: KnowledgeGraphUpdate,
    )
}

