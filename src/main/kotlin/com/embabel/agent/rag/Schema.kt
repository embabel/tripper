package com.embabel.agent.rag

import com.embabel.agent.core.PropertyDefinition
import com.embabel.common.core.types.HasInfoString

/**
 * @param description a human-readable description of the entity type
 * @param labels a set of labels or types that this entity belongs to
 * @param properties a list of properties that this entity has
 * @param creationPermitted if false new entities of this type cannot be created
 */
data class EntityDefinition(
    val description: String,
    val labels: Set<String>,
    val properties: List<PropertyDefinition>,
    val creationPermitted: Boolean = true,
) : HasInfoString {

    constructor (
        type: String,
        description: String,
        properties: List<PropertyDefinition> = emptyList(),
    ) : this(description, setOf(type), properties)

    val type get() = labels.firstOrNull() ?: "Unknown"

    override fun infoString(verbose: Boolean?): String {
        return """
            EntityDefinition(type='$type', description='$description', labels=$labels, properties=${properties.size})
        """.trimIndent()
    }
}

enum class Cardinality {
    ONE,
    MANY,
}

data class RelationshipDefinition(
    val sourceEntity: String,
    val targetEntity: String,
    val type: String,
    val description: String,
    val cardinality: Cardinality = Cardinality.ONE,
) : HasInfoString {

    override fun infoString(verbose: Boolean?): String {
        return """
            RelationshipDefinition(sourceEntity='$sourceEntity', targetEntity='$targetEntity', type='$type', cardinality=$cardinality, description='$description')
        """.trimIndent()
    }
}

data class Schema(
    val entities: List<EntityDefinition>,
    val relationships: List<RelationshipDefinition>,
) : HasInfoString {

    fun possibleRelationshipsBetween(entities: List<EntityData>): List<RelationshipDefinition> {
        return relationships.filter { relationship ->
            entities.any { it.labels.contains(relationship.sourceEntity) } &&
                    entities.any { it.labels.contains(relationship.targetEntity) }
        }
    }

    override fun infoString(verbose: Boolean?): String {
        return """
            |Schema with ${entities.size} entities and ${relationships.size} relationships:
            |Entities:
            |${entities.joinToString("\n") { "\t${it.infoString(verbose)} " }}
            |Relationships:
            |${relationships.joinToString("\n") { "\t${it.infoString(verbose)} " }}
            """.trimMargin()
    }
}