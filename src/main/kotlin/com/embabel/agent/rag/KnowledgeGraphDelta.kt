package com.embabel.agent.rag

import com.embabel.common.core.types.HasInfoString
import com.embabel.common.util.loggerFor
import com.fasterxml.jackson.annotation.JsonPropertyDescription

data class SuggestedEntity(
    val type: String,
    val name: String,
    val summary: String,
    @JsonPropertyDescription("Will be a UUID. Include only if provided")
    val id: String? = null,
)

sealed interface SuggestedEntityResolution : HasInfoString {
    val suggestedEntity: SuggestedEntity
}

/**
 * We were able to resolve the suggested entity to an existing or new entity.
 */
sealed interface EntityDataResolution : SuggestedEntityResolution {
    val entityData: EntityData
}

data class NewEntity(
    override val suggestedEntity: SuggestedEntity,
    override val entityData: EntityData,
) : EntityDataResolution {

    override fun infoString(verbose: Boolean?): String {
        return "NewEntity(type=${suggestedEntity.type}, ${entityData.infoString(verbose)})"
    }
}

data class ExistingEntity(
    override val suggestedEntity: SuggestedEntity,
    override val entityData: EntityData,
    val conflicts: Boolean,
) : EntityDataResolution {

    override fun infoString(verbose: Boolean?): String {
        return "ResolvedEntity(type=${suggestedEntity.type}, ${entityData.infoString(verbose)})"
    }
}

data class VetoedEntity(
    override val suggestedEntity: SuggestedEntity,
) : SuggestedEntityResolution {

    override fun infoString(verbose: Boolean?): String {
        return "VetoedEntity(type=${suggestedEntity.type})"
    }
}

data class SuggestedEntities(
    val basis: Retrievable,
    val suggestedEntities: List<SuggestedEntity>,
)

data class SuggestedRelationships(
    val entitiesResolution: SuggestedEntitiesResolution,
    val suggestedRelationships: List<SuggestedRelationship>,
)

data class SuggestedEntitiesResolution(
    val basis: Retrievable,
    val resolutions: List<SuggestedEntityResolution>,
)

interface RelationshipInstance {
    val sourceId: String
    val targetId: String
    val type: String
    val description: String?
}

data class SuggestedRelationship(
    override val sourceId: String,
    override val targetId: String,
    override val type: String,
    override val description: String? = null,
) : RelationshipInstance {

    fun isValid(
        schema: Schema,
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
            loggerFor<Schema>().info(
                "Relationship between {} and {} of type {} is invalid",
                sourceType,
                targetType,
                type,
            )
        }
        return valid
    }
}

data class KnowledgeGraphDelta(
    val basis: Retrievable,
    val newEntities: List<EntityData>,
    val newRelationships: List<RelationshipInstance>,
) : HasInfoString {

    override fun infoString(verbose: Boolean?): String {
        return "KnowledgeGraphUpdate(entities=${newEntities.size}, relationships=${newEntities.size}, entityLabels=${
            newEntities.joinToString(", ") { it.infoString(verbose) }
        }, relationshipTypes=${newRelationships.map { it.type }.distinct().joinToString(", ")})"
    }
}

data class SimpleEntityData(
    override val id: String,
    override val description: String,
    override val labels: Set<String>,
    override val properties: Map<String, Any>,
    override val metadata: Map<String, Any?> = emptyMap(),
) : EntityData