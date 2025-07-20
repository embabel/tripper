package com.embabel.boogie

import com.embabel.agent.rag.EntityData

interface NamedEntityData : EntityData {
    val name: String
}

data class SimpleNamedEntityData(
    override val id: String,
    override val name: String,
    override val description: String,
    override val labels: Set<String>,
    override val properties: Map<String, Any>,
    override val metadata: Map<String, Any?> = emptyMap(),
) : NamedEntityData {

    override fun embeddableValue(): String {
        return "$name: $description"
    }
}