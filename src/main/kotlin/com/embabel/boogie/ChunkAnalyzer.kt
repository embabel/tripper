package com.embabel.boogie

import com.embabel.agent.rag.*
import com.embabel.boogie.schema.KnowledgeGraphSchema
import com.embabel.common.util.loggerFor
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import java.util.*


data class SuggestedEntity(
    private val type: String,
    private val name: String,
    private val summary: String,
    @param:JsonPropertyDescription("Will be a UUID. Include only if provided")
    private val id: String? = null,
//    @JsonPropertyDescription("Map from property name to value")
//    val properties: Map<String, Any> = emptyMap(),
) {
    @JsonIgnore
    val kgEntity: NamedEntityData = SimpleNamedEntityData(
        id = id ?: UUID.randomUUID().toString(),
        name = name,
        description = summary,
        labels = setOf(type),
        // TODO fix this
        properties = emptyMap(),
    )
}

/**
 * Entities suggested by the LLM based on input.
 */
data class SuggestedEntities(
    override val basis: Retrievable,
    val suggestedEntities: List<SuggestedEntity>,
) : Sourced

data class SuggestedRelationship(
    override val sourceId: String,
    override val targetId: String,
    override val type: String,
    override val description: String? = null,
) : RelationshipInstance {

    fun isValid(
        schema: KnowledgeGraphSchema,
        sourceEntity: EntityData,
        targetEntity: EntityData,
    ): Boolean {
        val sourceType = sourceEntity.labels.singleOrNull()
            ?: throw IllegalArgumentException("Source entity must have a single label")
        val targetType = targetEntity.labels.singleOrNull()
            ?: throw IllegalArgumentException("Target entity must have a single label")
        val valid =
            schema.relationships.any { it.type == type && it.sourceEntity == sourceType && it.targetEntity == targetType }
        if (!valid) {
            loggerFor<KnowledgeGraphSchema>().info(
                "Relationship between {} and {} of type {} is invalid",
                sourceType,
                targetType,
                type,
            )
        }
        return valid
    }
}


data class SuggestedRelationships(
    val entitiesResolution: SuggestedEntitiesResolution,
    val suggestedRelationships: List<SuggestedRelationship>,
) : Sourced by entitiesResolution

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

