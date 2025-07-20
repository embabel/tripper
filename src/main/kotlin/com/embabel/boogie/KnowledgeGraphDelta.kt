package com.embabel.boogie

import com.embabel.agent.rag.NamedEntityData
import com.embabel.agent.rag.Retrievable
import com.embabel.common.core.types.HasInfoString


/**
 * Update we'll apply to a knowledge graph in a persistent store.
 */
data class KnowledgeGraphDelta(
    override val basis: Retrievable,
    val entityDeterminations: EntityDeterminations,
    val relationshipDeterminations: RelationshipDeterminations,
) : Sourced, HasInfoString {

    fun newEntities(): List<NamedEntityData> {
        return entityDeterminations.determinations.filter { it.resolution is NewEntity }
            .mapNotNull { it.convergenceTarget }
    }

    fun mergedEntities(): List<EntityDetermination> {
        return entityDeterminations.determinations.filter { it.convergenceTarget != null && it.resolution is ExistingEntity }
    }

    fun newOrModifiedEntities(): List<NamedEntityData> {
        return newEntities() + mergedEntities().mapNotNull { it.convergenceTarget }
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

