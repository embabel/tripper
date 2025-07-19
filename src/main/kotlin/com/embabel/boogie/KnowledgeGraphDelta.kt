package com.embabel.boogie

import com.embabel.agent.rag.EntityData
import com.embabel.agent.rag.Retrievable
import com.embabel.common.core.types.HasInfoString


data class KnowledgeGraphDelta(
    override val basis: Retrievable,
    val entityDeterminations: EntityDeterminations,
    val relationshipDeterminations: RelationshipDeterminations,
) : Sourced, HasInfoString {

    fun newEntities(): List<EntityData> {
        return entityDeterminations.determinations.filter { it.resolution is NewEntity }.mapNotNull { it.entityProduct }
    }

    fun mergedEntities(): List<EntityDetermination> {
        return entityDeterminations.determinations.filter { it.entityProduct != null && it.resolution is ExistingEntity }
    }

    fun newRelationships(): List<NewRelationship> {
        return relationshipDeterminations.determinations.map { it.resolution }.filterIsInstance<NewRelationship>()
    }

    fun mergedRelationships(): List<ExistingRelationship> {
        return relationshipDeterminations.determinations.map { it.resolution }.filterIsInstance<ExistingRelationship>()
    }

    override fun infoString(verbose: Boolean?): String {
//        return "KnowledgeGraphUpdate(entitiesResolution=${entitiesResolution.resolutions.size}, relationships=${newRelationships.size}, entityLabels=${
//            "TODO"
//        }, relationshipTypes=${newRelationships.map { it.type }.distinct().joinToString(", ")})"
        return toString()
    }
}

data class SimpleEntityData(
    override val id: String,
    override val description: String,
    override val labels: Set<String>,
    override val properties: Map<String, Any>,
    override val metadata: Map<String, Any?> = emptyMap(),
) : EntityData {

    override fun embeddableValue(): String {
        return description
    }
}