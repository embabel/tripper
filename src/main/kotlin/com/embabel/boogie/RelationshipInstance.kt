package com.embabel.boogie

interface RelationshipInstance {
    val sourceId: String
    val targetId: String
    val type: String
    val description: String?

    companion object {

        operator fun invoke(
            sourceId: String,
            targetId: String,
            type: String,
            description: String?,
        ): RelationshipInstance {
            return RelationshipInstanceImpl(sourceId, targetId, type, description)
        }
    }
}

private data class RelationshipInstanceImpl(
    override val sourceId: String,
    override val targetId: String,
    override val type: String,
    override val description: String? = null
) : RelationshipInstance