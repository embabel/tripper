package com.embabel.agent.rag

import com.embabel.agent.core.PropertyDefinition

data class EntityDefinition(
    val description: String,
    val labels: Set<String>,
    val properties: List<PropertyDefinition>,
) {

    constructor (
        type: String,
        description: String,
        properties: List<PropertyDefinition> = emptyList(),
    ) : this(description, setOf(type), properties)

    val type get() = labels.firstOrNull() ?: "Unknown"
}

data class RelationshipDefinition(
    val sourceEntity: String,
    val targetEntity: String,
    val type: String,
    val description: String,
)

data class Schema(
    val entities: List<EntityDefinition>,
    val relationships: List<RelationshipDefinition>,
) {

    fun possibleRelationshipsBetween(entities: List<EntityData>): List<RelationshipDefinition> {
        return relationships.filter { relationship ->
            entities.any { it.labels.contains(relationship.sourceEntity) } &&
                    entities.any { it.labels.contains(relationship.targetEntity) }
        }
    }
}