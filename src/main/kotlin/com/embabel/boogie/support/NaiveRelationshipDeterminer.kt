package com.embabel.boogie.support

import com.embabel.boogie.RelationshipDetermination
import com.embabel.boogie.RelationshipDeterminations
import com.embabel.boogie.RelationshipDeterminer
import com.embabel.boogie.SuggestedRelationshipsResolution
import com.embabel.boogie.schema.KnowledgeGraphSchema

/**
 * Always adds new entities and ignores existing or vetoed entities.
 */
class NaiveRelationshipDeterminer : RelationshipDeterminer {

    override fun determineRelationships(
        suggestedRelationshipsResolution: SuggestedRelationshipsResolution,
        schema: KnowledgeGraphSchema,
    ): RelationshipDeterminations {
        return RelationshipDeterminations(
            basis = suggestedRelationshipsResolution.basis,
            determinations = suggestedRelationshipsResolution.resolutions.map { suggestedRelationship ->
                RelationshipDetermination(suggestedRelationship, suggestedRelationship.suggestedRelationship)
            }
        )
    }
}