package com.embabel.boogie

import com.embabel.agent.rag.EntityData
import com.embabel.agent.rag.Retrievable
import com.embabel.common.core.types.HasInfoString

data class SuggestedEntitiesResolution(
    override val basis: Retrievable,
    val resolutions: List<SuggestedEntityResolution>,
) : Sourced

sealed interface SuggestedEntityResolution : HasInfoString {
    val suggestedEntity: SuggestedEntity
}

/**
 * We were able to resolve the suggested entity to an existing or new entity.
 */
sealed interface EntityDataResolution : SuggestedEntityResolution {
    val entityData: EntityData
}

/**
 * No entity existed. We simply create a new entity.
 */
data class NewEntity(
    override val suggestedEntity: SuggestedEntity,
    override val entityData: EntityData,
) : EntityDataResolution {

    override fun infoString(verbose: Boolean?): String {
        return "NewEntity(type=${suggestedEntity.type}, ${entityData.infoString(verbose)})"
    }
}

/**
 * An existing entity was found that matches the suggested entity.
 */
data class ExistingEntity(
    override val suggestedEntity: SuggestedEntity,
    override val entityData: EntityData,
) : EntityDataResolution {

    override fun infoString(verbose: Boolean?): String {
        return "ResolvedEntity(type=${suggestedEntity.type}, ${entityData.infoString(verbose)})"
    }
}

data class VetoedEntity(
    override val suggestedEntity: SuggestedEntity,
) : SuggestedEntityResolution {

    override fun infoString(verbose: Boolean?): String {
        return "VetoedEntity(type=${suggestedEntity.type})"
    }
}

/**
 * Resolves entities based on existing data
 */
interface EntityResolver {

    fun resolve(
        suggestedEntities: SuggestedEntities,
        schema: KnowledgeGraphSchema,
    ): SuggestedEntitiesResolution

}