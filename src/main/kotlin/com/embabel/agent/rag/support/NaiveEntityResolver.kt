package com.embabel.agent.rag.support

import com.embabel.agent.rag.*
import java.util.*

// Trust in all entities
class NaiveEntityResolver : EntityResolver {

    override fun resolve(suggestedEntities: SuggestedEntities): EntityResolution {
        // For simplicity, let's assume we resolve entities by their name
        val resolvedEntities = suggestedEntities.suggestedEntities.map {
            NewEntity(
                it,
                SimpleEntityData(
                    id = it.id ?: UUID.randomUUID().toString(),
                    description = it.summary,
                    labels = setOf(it.type),
                    properties = emptyMap(),
                )
            )
        }
        return EntityResolution(
            chunk = suggestedEntities.chunk,
            resolvedEntities = resolvedEntities,
        )
    }

}