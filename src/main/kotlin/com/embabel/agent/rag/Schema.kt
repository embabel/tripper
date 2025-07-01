package com.embabel.agent.rag

data class EntityDefinition(val type: String, val description: String)

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