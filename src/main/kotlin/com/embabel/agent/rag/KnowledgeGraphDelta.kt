package com.embabel.agent.rag

import com.embabel.common.core.types.HasInfoString
import com.fasterxml.jackson.annotation.JsonPropertyDescription

data class SuggestedEntity(
    val type: String,
    val summary: String,
    @JsonPropertyDescription("Will be a UUID. Include only if provided")
    val id: String? = null,
)

sealed interface Resolution : HasInfoString {
    val suggestedEntity: SuggestedEntity
    val entityData: EntityData
}

data class NewEntity(
    override val suggestedEntity: SuggestedEntity,
    override val entityData: EntityData,
) : Resolution {

    override fun infoString(verbose: Boolean?): String {
        return "NewEntity(type=${suggestedEntity.type}, ${entityData.infoString(verbose)})"
    }
}

data class ResolvedEntity(
    override val suggestedEntity: SuggestedEntity,
    override val entityData: EntityData,
    val conflicts: Boolean,
) : Resolution {

    override fun infoString(verbose: Boolean?): String {
        return "ResolvedEntity(type=${suggestedEntity.type}, ${entityData.infoString(verbose)})"
    }
}

data class SuggestedEntities(
    val basis: Retrievable,
    val suggestedEntities: List<SuggestedEntity>,
)

// TODO should be able to have problems where there's a conflict
data class EntityResolution(
    val basis: Retrievable,
    val resolvedEntities: List<Resolution>,
    // TODO conflicts
)

data class SuggestedRelationship(
    val sourceId: String,
    val targetId: String,
    val type: String,
    val description: String? = null,
)

data class KnowledgeGraphDelta(
    val entityResolution: EntityResolution,
    val relationships: List<SuggestedRelationship>,
) : HasInfoString {

    override fun infoString(verbose: Boolean?): String {
        return "KnowledgeGraphUpdate(entities=${entityResolution.resolvedEntities.size}, relationships=${relationships.size}, entityLabels=${
            entityResolution.resolvedEntities.joinToString(", ") { it.infoString(verbose) }
        }, relationshipTypes=${relationships.map { it.type }.distinct().joinToString(", ")})"
    }
}

data class SimpleEntityData(
    override val id: String,
    override val description: String,
    override val labels: Set<String>,
    override val properties: Map<String, Any>,
) : EntityData