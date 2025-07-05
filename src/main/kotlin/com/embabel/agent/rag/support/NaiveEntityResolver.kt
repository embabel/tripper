package com.embabel.agent.rag.support

import com.embabel.agent.rag.*
import java.util.*

// Trust in all entities
class NaiveEntityResolver : EntityResolver {

    override fun resolve(
        suggestedEntities: SuggestedEntities,
        schema: KnowledgeGraphSchema,
    ): SuggestedEntitiesResolution {
        // For simplicity, let's assume we resolve entities by their name
        val resolvedEntities = suggestedEntities.suggestedEntities.map {
            NewEntity(
                it,
                SimpleEntityData(
                    id = it.id ?: UUID.randomUUID().toString(),
                    description = it.summary,
                    labels = setOf(it.type),
                    properties = mapOf(
                        "name" to it.name,
                    ),
                )
            )
        }
        return SuggestedEntitiesResolution(
            basis = suggestedEntities.basis,
            resolutions = resolvedEntities,
        )
    }

}

class NaiveRelationshipResolver : RelationshipResolver {

    override fun resolveRelationships(
        entityResolution: SuggestedEntitiesResolution,
        suggestedRelationships: SuggestedRelationships,
        schema: KnowledgeGraphSchema,
    ): SuggestedRelationshipsResolution {

        return SuggestedRelationshipsResolution(
            basis = entityResolution.basis,
            resolutions = suggestedRelationships.suggestedRelationships.map {
                NewRelationship(it)
            },
        )
    }

}