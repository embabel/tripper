package com.embabel.boogie.support

import com.embabel.boogie.*

// Trust in all entities
class NaiveEntityResolver : EntityResolver {

    override fun resolve(
        suggestedEntities: SuggestedEntities,
        schema: KnowledgeGraphSchema,
    ): SuggestedEntitiesResolution {
        val resolvedEntities = suggestedEntities.suggestedEntities.map {
            NewEntity(it)
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