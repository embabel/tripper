package com.embabel.boogie.support

import com.embabel.boogie.*
import com.embabel.boogie.schema.KnowledgeGraphSchema

/**
 * Always adds new entities and ignores existing or vetoed entities.
 */
class NaiveEntityDeterminer : EntityDeterminer {

    override fun determineEntities(
        suggestedEntitiesResolution: SuggestedEntitiesResolution,
        schema: KnowledgeGraphSchema,
    ): EntityDeterminations {
        return EntityDeterminations(
            basis = suggestedEntitiesResolution.basis,
            determinations = suggestedEntitiesResolution.resolutions.map {
                when (it) {
                    is NewEntity -> EntityDetermination(
                        resolution = it,
                        convergenceTarget = it.kgEntity
                    )

                    is ExistingEntity -> EntityDetermination(
                        resolution = it,
                        convergenceTarget = null // Vetoed entities have no product
                    )

                    is VetoedEntity -> EntityDetermination(
                        resolution = it,
                        convergenceTarget = null // Vetoed entities have no product
                    )
                }
            }
        )
    }
}