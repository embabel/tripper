package com.embabel.boogie.support

import com.embabel.boogie.KnowledgeGraphSchema
import com.embabel.boogie.RelationshipDeterminations
import com.embabel.boogie.RelationshipDeterminer
import com.embabel.boogie.SuggestedRelationshipsResolution

/**
 * Always adds new entities and ignores existing or vetoed entities.
 */
class NaiveRelationshipDeterminer : RelationshipDeterminer {

    override fun determineRelationships(
        suggestedRelationshipsResolution: SuggestedRelationshipsResolution,
        schema: KnowledgeGraphSchema,
    ): RelationshipDeterminations {
        TODO()
    }
}